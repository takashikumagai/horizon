package space.nyanko.nyankoapplication

import android.app.PendingIntent
import android.content.Intent
import android.app.NotificationManager
import android.os.Bundle
import android.app.Activity
import android.app.TaskStackBuilder
import android.content.Context
import android.util.Log


/**
 * @brief This activity exists for the sole purpose of dismissing the notification
 *
 * - Sends the audio service to the background in onCreate() and swiftly destroys itself.
 */
class NotificationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        // Pause the playback before sending the service to background
        BackgroundAudioService.instance?.pause()

        // Send the service to background
        // This seems necessary in order to cancel the notification
        BackgroundAudioService.instance?.stopForegroundAndRemoveNotification()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(intent.getIntExtra(NOTIFICATION_ID, -1))
        finish() // since finish() is called in onCreate(), onDestroy() will be called immediately
    }

    companion object {

        private const val TAG = "NotificationActivity"

        private const val NOTIFICATION_ID = "NOTIFICATION_ID"

        fun getDismissIntent(notificationId: Int, context: Context): PendingIntent? {
            Log.d(TAG, "gDI: " + notificationId)

            // Create an intent for this activity
            val intent = Intent(context, NotificationActivity::class.java)
            intent.putExtra(NOTIFICATION_ID, notificationId)

            // used to retrieve the same pending intent instance later on (for cancelling, etc)
            val requestCode = 0//4253

            val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
                addNextIntentWithParentStack(intent)
                getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT)
            }

            if(pendingIntent == null) {
                Log.e(TAG, "!pI")
            }

            return pendingIntent
        }
    }

}