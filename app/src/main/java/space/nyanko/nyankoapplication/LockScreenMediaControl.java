package space.nyanko.nyankoapplication;

import android.os.Build;
import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.media.session.MediaSessionCompat;
import androidx.core.content.ContextCompat;
import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import androidx.media.app.NotificationCompat.MediaStyle;
//import android.support.v4.media.app.Notification.MediaStyle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;


/**
 * @brief Media controls on lock screen using notification
 *
 * - Ref: https://developer.android.com/training/notify-user/expanded#media-style
 */
@SuppressLint("NewApi")
class LockScreenMediaControl {

    private static final String TAG = "LockScreenMediaControl";

    private static final String CHANNEL_ID = "MyChannel";

    private static final int NOTIFICATION_ID = 12345;

    private static Notification notification;

    /**
     * @brief Initializses the lock screen control without showing it on screen.
     *
     * @param context
     * @param mediaSession
     */
    public static void init(Context context, MediaSessionCompat mediaSession) {
        Log.d(TAG,"init");



        PendingIntent prevPendingIntent = null;
        PendingIntent pausePendingIntent = null;
        PendingIntent nextPendingIntent = null;

        // Token to hand to the builder
        MediaSessionCompat.Token compatToken = mediaSession.getSessionToken();
        // We also need to do this conversion
        //MediaSession.Token sessionToken = (MediaSession.Token)compatToken.getToken();

        notification = new NotificationCompat.Builder(context, CHANNEL_ID)
        // Show controls on lock screen even when user hides sensitive content.
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setSmallIcon(R.drawable.ic_file)
        // Add media control buttons that invoke intents in your media service
        .addAction(R.drawable.ic_file, "Previous", prevPendingIntent) // #0
        .addAction(R.drawable.ic_file, "Pause", pausePendingIntent)  // #1
        .addAction(R.drawable.ic_file, "Next", nextPendingIntent)     // #2
        // Apply the media style template
        .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1 /* #1: pause button */)
                .setMediaSession(compatToken))
        .setContentTitle("Nyanko music")
        .setContentText("Nyankolz")
        //.setLargeIcon(albumArtBitmap)
        .build();

        if(notification == null) {
            Log.e(TAG,"init !n");
            return;
        }
    }

    /**
     * @brief Shows the notification / lock screen controls.
     *
     * - Displayed as a notification if the phone is not locked.
     *
     * @param context
     */
    public static void show(Context context) {
        Log.d(TAG,"show");

        if(notification == null) {
            Log.w(TAG,"!n");
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if(notificationManager == null) {
            Log.w(TAG,"!nM");
            NotificationManager mgr
                    = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if(mgr == null) {
                Log.w(TAG,"!mgr");
            }
            return;
        }

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * @brief Hides the notification / lock screen controls.
     * @param context
     */
    public static void hide(Context context) {
        Log.d(TAG,"hide");

        if(notification == null) {
            Log.w(TAG,"!n");
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if(notificationManager == null) {
            Log.w(TAG,"!nM");
            return;
        }

        notificationManager.cancel(NOTIFICATION_ID);
    }

    /**
     * @biref Creates a notification channel for the notification (lock screen controls)
     *
     * - This has to be called as soon as the app starts up.
     *
     * @param context
     */
    public static void createNotificationChannel(Context context) {
        Log.d(TAG,"cNC");

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "channel-name";//getString(R.string.channel_name);
            String description = "my-channel";//getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}

