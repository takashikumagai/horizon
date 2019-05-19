package space.nyanko.nyankoapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class HorizonBroadcastReceiver : BroadcastReceiver() {

    init {
        Log.d(TAG,"init")
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG,"onReceive " + intent.toString())
        var listener = HorizonNotificationListener()
        listener.cancelAllNotifications()
    }

    companion object {
        private const val TAG = "HrzBroadcastReceiver"

        var instance = HorizonBroadcastReceiver()
    }
}
