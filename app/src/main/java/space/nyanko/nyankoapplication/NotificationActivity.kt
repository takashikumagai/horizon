package space.nyanko.nyankoapplication

import android.app.PendingIntent
import android.content.Intent
import android.app.NotificationManager
import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.util.Log


class NotificationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        // Send the service to background
        // This seems necessary in order to cancel the notification
        BackgroundAudioService.instance?.stopForegroundAndRemoveNotification()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(intent.getIntExtra(NOTIFICATION_ID, -1))
        finish() // since finish() is called in onCreate(), onDestroy() will be called immediately
    }

    companion object {

        val TAG = "NotificationActivity"

        val NOTIFICATION_ID = "NOTIFICATION_ID"

        fun getDismissIntent(notificationId: Int, context: Context): PendingIntent {
            Log.d(TAG, "gDI: " + notificationId)
            val intent = Intent(context, NotificationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra(NOTIFICATION_ID, notificationId)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        }
    }

}