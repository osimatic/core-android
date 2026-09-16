package com.osimatic.core_android;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Generic base class for Firebase Cloud Messaging.
 * Handles installation id lifecycle, topic subscriptions and notification display.
 * Subclasses provide app-specific behavior via abstract methods.
 *
 * <p>Supported data payload keys:
 * <ul>
 *   <li>{@code title} — overrides the notification payload title</li>
 *   <li>{@code body} — overrides the notification payload body</li>
 *   <li>{@code categ_type} — forwarded to {@link #createNotificationIntent} for deep linking</li>
 * </ul>
 *
 * <p>Usage:
 * <ol>
 *   <li>Extend this class in your app.</li>
 *   <li>Implement the abstract methods.</li>
 *   <li>Call {@link #subscribeToTopic} / {@link #unsubscribeFromTopic} where needed (e.g. after login/logout).</li>
 *   <li>Call {@link #unregister(Consumer)} on logout.</li>
 * </ol>
 *
 * @see <a href="https://firebase.google.com/docs/cloud-messaging/android/receive">FCM — Receive messages on Android</a>
 */
public abstract class BaseFcmService extends FirebaseMessagingService {

	private static final String TAG = "BaseFcmService";

	// -------------------------------------------------------------------------
	// Abstract methods — implement in your app subclass
	// -------------------------------------------------------------------------

	/** Returns true if a user session is currently active. */
	protected abstract boolean isUserLoggedIn();

	/**
	 * Sends the Firebase Installations ID (FID) to the backend server.
	 * Called automatically on registration when a session is active.
	 *
	 * @param installationId the Firebase Installations ID
	 */
	protected abstract void registerInstallationIdOnServer(String installationId);

	/**
	 * Builds the {@link Intent} to open when the user taps the notification.
	 *
	 * @param categType value of {@code categ_type} from the data payload, may be null
	 * @param data      full data payload map (never null, may be empty)
	 * @return the intent to launch on notification tap
	 */
	protected abstract Intent createNotificationIntent(String categType, Map<String, String> data);

	/**
	 * Returns the drawable resource to use as the notification small icon.
	 *
	 * @return a {@link DrawableRes} resource id
	 */
	@DrawableRes
	protected abstract int getNotificationSmallIcon();

	/**
	 * Returns the ID of the notification channel.
	 * Must match the value declared in {@code AndroidManifest.xml}.
	 *
	 * @return the notification channel id
	 */
	protected abstract String getNotificationChannelId();

	/**
	 * Returns the user-visible name of the notification channel.
	 *
	 * @return the notification channel name
	 */
	protected abstract String getNotificationChannelName();

	// -------------------------------------------------------------------------
	// FCM lifecycle
	// -------------------------------------------------------------------------

	@Override
	public final void onMessageReceived(RemoteMessage remoteMessage) {
		Log.d(TAG, "From: " + remoteMessage.getFrom());

		Map<String, String> data = remoteMessage.getData();
		String title = null;
		String body = null;

		// Notification payload (only received in foreground; background handled by system)
		if (remoteMessage.getNotification() != null) {
			title = remoteMessage.getNotification().getTitle();
			body = remoteMessage.getNotification().getBody();
			Log.d(TAG, "Notification payload — title: " + title + ", body: " + body);
		}

		// Data payload (always received, foreground and background)
		if (!data.isEmpty()) {
			Log.d(TAG, "Data payload: " + data);
			if (data.containsKey("title")) title = data.get("title");
			if (data.containsKey("body")) body = data.get("body");
		}

		if (title != null || body != null) {
			Intent intent = createNotificationIntent(data.get("categ_type"), data);
			showNotification(title, body, intent);
		}
	}

	@Override
	public final void onRegistered(@NonNull String installationId) {
		Log.d(TAG, "Registered, installation id: " + installationId);
		if (!isUserLoggedIn()) {
			return;
		}
		new Thread(() -> registerInstallationIdOnServer(installationId)).start();
	}

	// -------------------------------------------------------------------------
	// Installation id management
	// -------------------------------------------------------------------------

	/**
	 * Retrieves the current Firebase Installations ID (FID).
	 *
	 * @param onIdRetrieved callback invoked (on a background thread) with the id; may be null
	 * @see <a href="https://firebase.google.com/docs/reference/android/com/google/firebase/installations/FirebaseInstallations#getId()">FirebaseInstallations.getId()</a>
	 */
	public static void getInstallationId(Consumer<String> onIdRetrieved) {
		FirebaseInstallations.getInstance().getId()
				.addOnSuccessListener(id -> {
					if (onIdRetrieved != null) {
						new Thread(() -> onIdRetrieved.accept(id)).start();
					}
				})
				.addOnFailureListener(e -> Log.e(TAG, "Failed to retrieve installation id: " + e.getMessage()));
	}

	/**
	 * Unregisters from FCM. The device will no longer receive FCM messages until registered again.
	 * On success, {@code onUnregistered} is called on a background thread with the installation id.
	 * Call this on logout.
	 *
	 * @param onUnregistered callback invoked (on a background thread) with the installation id; may be null
	 * @see <a href="https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/FirebaseMessaging#unregister()">FirebaseMessaging.unregister()</a>
	 */
	public static void unregister(Consumer<String> onUnregistered) {
		FirebaseInstallations.getInstance().getId()
				.addOnSuccessListener(id -> FirebaseMessaging.getInstance().unregister()
						.addOnSuccessListener(unused -> {
							Log.d(TAG, "FCM unregistered");
							if (onUnregistered != null) {
								new Thread(() -> onUnregistered.accept(id)).start();
							}
						})
						.addOnFailureListener(e -> Log.e(TAG, "Failed to unregister: " + e.getMessage())))
				.addOnFailureListener(e -> Log.e(TAG, "Failed to retrieve installation id: " + e.getMessage()));
	}

	// -------------------------------------------------------------------------
	// Topic subscriptions
	// -------------------------------------------------------------------------

	/**
	 * Subscribes to an FCM topic. All devices subscribed to the same topic
	 * will receive messages sent to it (no individual token needed).
	 * Call after login or when the relevant feature is enabled.
	 *
	 * @param topic topic name (alphanumeric, hyphens and underscores only)
	 * @see <a href="https://firebase.google.com/docs/cloud-messaging/android/topic-messaging">FCM topic messaging</a>
	 */
	public static void subscribeToTopic(String topic) {
		FirebaseMessaging.getInstance().subscribeToTopic(topic)
				.addOnSuccessListener(unused -> Log.d(TAG, "Subscribed to topic: " + topic))
				.addOnFailureListener(e -> Log.e(TAG, "Failed to subscribe to topic " + topic + ": " + e.getMessage()));
	}

	/**
	 * Unsubscribes from an FCM topic.
	 * Call on logout or when the relevant feature is disabled.
	 *
	 * @param topic topic name to unsubscribe from
	 */
	public static void unsubscribeFromTopic(String topic) {
		FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
				.addOnSuccessListener(unused -> Log.d(TAG, "Unsubscribed from topic: " + topic))
				.addOnFailureListener(e -> Log.e(TAG, "Failed to unsubscribe from topic " + topic + ": " + e.getMessage()));
	}

	// -------------------------------------------------------------------------
	// Notification display
	// -------------------------------------------------------------------------

	/**
	 * Builds and displays a system notification.
	 *
	 * @param title  notification title (may be null)
	 * @param body   notification body (may be null)
	 * @param intent intent to launch on tap
	 */
	protected final void showNotification(String title, String body, Intent intent) {
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(
				this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

		String channelId = getNotificationChannelId();
		Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
				.setSmallIcon(getNotificationSmallIcon())
				.setContentTitle(title)
				.setContentText(body)
				.setAutoCancel(true)
				.setSound(defaultSoundUri)
				.setContentIntent(pendingIntent);

		NotificationManager notificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
					channelId, getNotificationChannelName(), NotificationManager.IMPORTANCE_DEFAULT);
			notificationManager.createNotificationChannel(channel);
		}

		int uniqId = (int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE);
		notificationManager.notify(uniqId, builder.build());
	}
}