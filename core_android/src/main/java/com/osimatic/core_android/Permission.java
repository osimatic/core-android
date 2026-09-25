package com.osimatic.core_android;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

/**
 * Utility class for handling permission-denied UI feedback.
 *
 * <p>This class is not instantiable. All methods are static.
 */
public class Permission {

	private Permission() {}

	/**
	 * Opens the app's details screen in the system Settings app, where the user can grant a permission manually (e.g. after permanently denying it).
	 *
	 * @param context the context used to start the settings screen; must not be {@code null}
	 * @see Settings#ACTION_APPLICATION_DETAILS_SETTINGS
	 */
	public static void openAppSettings(Context context) {
		Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		intent.setData(Uri.fromParts("package", context.getPackageName(), null));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	/**
	 * Shows an indefinite {@link Snackbar} explaining that a permission was denied, with an action button that opens the app's Settings screen.
	 *
	 * @param view                   the view used to find a suitable parent for the {@link Snackbar}; must not be {@code null}
	 * @param message                the message explaining the denied permission; must not be {@code null}
	 * @param openSettingsButtonText the text of the action button; must not be {@code null}
	 * @see #openAppSettings(Context)
	 */
	public static void showSettingsSnackbar(View view, String message, String openSettingsButtonText) {
		Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
				.setAction(openSettingsButtonText, v -> openAppSettings(v.getContext()))
				.show();
	}
}