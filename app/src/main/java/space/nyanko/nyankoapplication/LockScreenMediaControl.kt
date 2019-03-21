package space.nyanko.nyankoapplication

import android.os.Build
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.content.ContextCompat
import android.app.PendingIntent
import android.app.Notification
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.media.app.NotificationCompat.MediaStyle
//import android.support.v4.media.app.Notification.MediaStyle;
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log


/**
 * @brief Media controls on lock screen using notification
 *
 * - Ref: https://developer.android.com/training/notify-user/expanded#media-style
 */
@SuppressLint("NewApi")
internal object LockScreenMediaControl {

    private val TAG = "LockScreenMediaControl"

    private val CHANNEL_ID = "HorizonNotificationChannel"

    val NOTIFICATION_ID = 12345

    var notification: Notification? = null
        private set

    private var contentTitle: String = ""

    private var contentText: String = ""

    private fun generateAction(context: Context, action: String, icon: Int, title: CharSequence): NotificationCompat.Action {

        val intent = Intent(context.getApplicationContext(), BackgroundAudioService::class.java)
        intent.setAction(action)

        // As per the documentation, this 'retrieves a PendingIntent that will start a service'
        // https://developer.android.com/reference/android/app/PendingIntent.html
        // What this actually does is that it invokes onStartCommand() of the service.
        // onStartCommand() is called not only when the service is created and started but also
        // when a intent is sent like this.
        val requestCode = 1
        val flags = 0
        val pendingIntent = PendingIntent.getService(
                context.getApplicationContext(),
                requestCode,
                intent,
                flags)

        val builder = NotificationCompat.Action.Builder(icon, title, pendingIntent)

        return builder.build()
    }

    /**
     * @brief Initializses the lock screen control without showing it on screen.
     *
     * @param context
     * @param mediaSession
     */
    fun init(
            context: Context,
            mediaSession: MediaSessionCompat?,
            isPlaying: Boolean,
            contentTitle: String?,
            contentText: String?) {
        Log.d(TAG, String.format("init %b %s %s",isPlaying,contentTitle,contentText))

        if(mediaSession == null) {
            return;
        }

        this.contentTitle = contentTitle ?: ""
        this.contentText = contentText ?: ""

        // Token to hand to the builder
        val compatToken = mediaSession.getSessionToken()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.app_icon)
                // Add media control buttons that invoke intents in your media service
                // Apply the media style template
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(1 /* #1: pause button */)
                        .setMediaSession(compatToken))
                .setContentTitle(contentTitle)
                .setContentText(contentText)
        //.setLargeIcon(albumArtBitmap)

        Log.d(TAG, "adding actions")

        builder.addAction(generateAction(
                context,
                BackgroundAudioService.ACTION_PREV_TRACK,
                R.drawable.prev,
                "Prev"))
        builder.addAction(generateAction(
                context,
                BackgroundAudioService.ACTION_PLAY_PAUSE,
                if(isPlaying) R.drawable.pause else R.drawable.play,
                "Play/Pause"))
        builder.addAction(generateAction(
                context,
                BackgroundAudioService.ACTION_NEXT_TRACK,
                R.drawable.next,
                "Next"))

        // Create an explicit intent to open MainActivity when the notification is tapped.
        val intent = Intent(context, MainActivity::class.java).apply {
            Log.d(TAG,"intent apply")
            //flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        builder//.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
//                //.setAutoCancel(true)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.d(TAG, "sdk<codes.o")
            builder.setVibrate(longArrayOf(0L))
        }

        notification = builder.build()

        if (notification == null) {
            Log.e(TAG, "init !n")
            return
        }
    }

    fun changeState(context: Context, mediaSession: MediaSessionCompat?, isPlaying: Boolean) {
        init(context, mediaSession, isPlaying, contentTitle, contentText);
    }

    /**
     * @brief Shows the notification / lock screen controls.
     *
     * - Displayed as a notification if the phone is not locked.
     *
     * @param context
     */
    fun show(context: Context) {
        Log.d(TAG, "show")

        if (notification == null) {
            Log.w(TAG, "!n")
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)

        if (notificationManager == null) {
            Log.w(TAG, "!nM")
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr == null) {
                Log.w(TAG, "!mgr")
            }
            return
        }

        val n: Notification? = notification as? Notification;
        if(n != null) {
            notificationManager.notify(NOTIFICATION_ID, n)
        }
    }

    /**
     * @brief Hides the notification / lock screen controls.
     * @param context
     */
    fun hide(context: Context) {
        Log.d(TAG, "hide")

        if (notification == null) {
            Log.w(TAG, "!n")
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)

        if (notificationManager == null) {
            Log.w(TAG, "!nM")
            return
        }

        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * @biref Creates a notification channel for the notification (lock screen controls)
     *
     * - This has to be called as soon as the app starts up.
     *
     * @param context
     */
    fun createNotificationChannel(context: Context) {
        Log.d(TAG, "cNC")

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "sdk>=codes.o")
            val name = "channel-name"//getString(R.string.channel_name);
            val description = "my-channel"//getString(R.string.channel_description);
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.setDescription(description)

            // Disable vibration
            // Note that we have to set the arg of enableVibration() to true in order to disable
            // the vibration. See this SO post for more details:
            // https://stackoverflow.com/questions/46402510/notification-vibrate-issue-for-android-8-0
            channel.setVibrationPattern(longArrayOf(0))
            channel.enableVibration(true)

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

