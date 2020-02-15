package space.nyanko.nyankoapplication

import android.os.Build
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.app.PendingIntent
import android.app.Notification
import android.app.NotificationManager
import android.app.NotificationChannel
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

    private var bitmap: Bitmap? = null

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

    fun init(context: Context, mediaSession: MediaSessionCompat?) {
        init(context, mediaSession, false, "", "", null)
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
            contentText: String?,
            bitmap: Bitmap?) {
        Log.d(TAG, String.format("init %b %s %s",isPlaying,contentTitle,contentText))

        if(mediaSession == null) {
            return;
        }

        val receiverIntent = Intent(context, HorizonBroadcastReceiver::class.java)
        val deletePendingIntent = PendingIntent.getBroadcast(
                context.applicationContext, 0, receiverIntent, 0)

        this.contentTitle = contentTitle ?: ""
        this.contentText = contentText ?: ""
        this.bitmap = bitmap

        // Token to hand to the builder
        val compatToken = mediaSession.getSessionToken()

        val largeIcon
                = bitmap ?: BitmapFactory.decodeResource(context.resources, R.drawable.app_icon)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.app_icon)
                // Add media control buttons that invoke intents in your media service
                // Apply the media style template
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                        // Show the buttons except the dismiss button (index 0)
                        .setShowActionsInCompactView(1, 2, 3)
                        .setMediaSession(compatToken))
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setLargeIcon(largeIcon)

                .setOngoing(false)

                // This does not affect whether the notification can be dismissed by swiping
                // When set to true, the notification disappears when the user taps it and
                // MainActivity is launched
                .setAutoCancel(false)

                .setDeleteIntent(deletePendingIntent)

        Log.d(TAG, "adding actions")

        val applicationContext = context.applicationContext

        val dismissIntent
                = NotificationActivity.getDismissIntent(NOTIFICATION_ID, applicationContext)

        builder.addAction(R.drawable.close, "Dismiss", dismissIntent)

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
        Log.d(TAG, "cS ${contentTitle} ${contentText}")
        init(context, mediaSession, isPlaying, contentTitle, contentText, bitmap)
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

        val n: Notification? = notification
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

        //val notificationManager = NotificationManagerCompat.from(context)
        val notificationManager
                = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager == null) {
            Log.w(TAG, "!nM")
            return
        }

        Log.d(TAG, "nM.cancel")
        notificationManager.cancel(NOTIFICATION_ID)

        context.sendBroadcast(Intent("space.nyanko.nyankoapplication"));
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
            val name = "horizon-notification"//getString(R.string.channel_name);
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

