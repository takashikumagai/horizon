package space.nyanko.nyankoapplication;

import android.os.Build;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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

import java.lang.CharSequence;


/**
 * @brief Media controls on lock screen using notification
 *
 * - Ref: https://developer.android.com/training/notify-user/expanded#media-style
 */
@SuppressLint("NewApi")
class LockScreenMediaControl {

    private static final String TAG = "LockScreenMediaControl";

    private static final String CHANNEL_ID = "HorizonNotificationChannel";

    public static final int NOTIFICATION_ID = 12345;

    private static Notification notification;

    private static NotificationCompat.Action generateAction(Context context, String action, int icon, CharSequence title) {

        Intent intent = new Intent(context.getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(action);

        // As per the documentation, this 'retrieves a PendingIntent that will start a service'
        // https://developer.android.com/reference/android/app/PendingIntent.html
        // What this actually does is that it invokes onStartCommand() of the service.
        // onStartCommand() is called not only when the service is created and started but also
        // when a intent is sent like this.
        int requestCode = 1;
        int flags = 0;
        PendingIntent pendingIntent
                = PendingIntent.getService(
                context.getApplicationContext(),
                requestCode,
                intent,
                flags);

        NotificationCompat.Action.Builder builder
                = new NotificationCompat.Action.Builder(icon, title, pendingIntent);

        return builder.build();
    }

    /**
     * @brief Initializses the lock screen control without showing it on screen.
     *
     * @param context
     * @param mediaSession
     */
    public static void init(
            Context context,
            MediaSessionCompat mediaSession,
            String contentTitle) {
        Log.d(TAG,"init");

        // Token to hand to the builder
        MediaSessionCompat.Token compatToken = mediaSession.getSessionToken();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
        // Show controls on lock screen even when user hides sensitive content.
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setSmallIcon(R.drawable.ic_file)
        // Add media control buttons that invoke intents in your media service
        // Apply the media style template
        .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1 /* #1: pause button */)
                .setMediaSession(compatToken))
        .setContentTitle(contentTitle)
        .setContentText("Nyankolz");
        //.setLargeIcon(albumArtBitmap)

        Log.d(TAG,"adding actions");

        builder.addAction(generateAction(
                context,
                BackgroundAudioService.ACTION_PREV_TRACK,
                R.drawable.ic_file,
                "Prev"));
        builder.addAction(generateAction(
                context,
                BackgroundAudioService.ACTION_PLAY_PAUSE,
                R.drawable.ic_file,
                "Play/Pause"));
        builder.addAction(generateAction(
                context,
                BackgroundAudioService.ACTION_NEXT_TRACK,
                R.drawable.ic_file,
                "Next"));

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.d(TAG,"sdk<codes.o");
            builder.setVibrate(new long[]{0L});
        }

        notification = builder.build();

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
            Log.d(TAG,"sdk>=codes.o");
            CharSequence name = "channel-name";//getString(R.string.channel_name);
            String description = "my-channel";//getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Disable vibration
            // Note that we have to set the arg of enableVibration() to true in order to disable
            // the vibration. See this SO post for more details:
            // https://stackoverflow.com/questions/46402510/notification-vibrate-issue-for-android-8-0
            channel.setVibrationPattern(new long[]{ 0 });
            channel.enableVibration(true);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static Notification getNotification() {
        return notification;
    }
}

