package com.osimatic.core_android;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.DrawableRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Affiche une notification simple, en créant le canal si besoin.
 * Ne fait rien si la permission POST_NOTIFICATIONS (Android 13+) ou l'autorisation de notifier n'est pas accordée.
 */
public class Notifier {

    /**
     * @param context contexte Android
     * @param channelId identifiant du canal de notification (créé au premier appel, Android 8+)
     * @param channelName nom du canal affiché dans les réglages système
     * @param title titre de la notification
     * @param text texte de la notification, déployé en entier (BigTextStyle)
     * @param smallIconResId icône de la barre d'état
     */
    public static void show(Context context, String channelId, String channelName, String title, String text, @DrawableRes int smallIconResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
            return;
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (!notificationManager.areNotificationsEnabled()) {
            return;
        }

        // Android 8+ : un canal de notification est obligatoire
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager systemManager = context.getSystemService(NotificationManager.class);
            if (null != systemManager && null == systemManager.getNotificationChannel(channelId)) {
                systemManager.createNotificationChannel(new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT));
            }
        }

        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(smallIconResId)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .build();

        // Identifiant unique : plusieurs notifications successives restent visibles
        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }
}