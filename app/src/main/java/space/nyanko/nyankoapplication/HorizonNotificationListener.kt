package space.nyanko.nyankoapplication

import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class HorizonNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d(TAG,"onNotifPosted")
        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(TAG,"onNotifRemoved")
        super.onNotificationRemoved(sbn)
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG,"onBind")
        return super.onBind(intent)
    }

    override fun onDestroy() {
        Log.d(TAG,"onDestroy")
        unregisterReceiver(HorizonBroadcastReceiver.instance)
        super.onDestroy()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG,"onCreate")
        registerReceiver(
                HorizonBroadcastReceiver.instance,
                IntentFilter ("space.nyanko.nyankoapplication")
        )
    }

    companion object {
        private const val TAG = "HrzNotificationListener"
    }
}