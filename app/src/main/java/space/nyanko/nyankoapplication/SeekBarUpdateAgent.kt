package space.nyanko.nyankoapplication

import android.os.Handler
import android.util.Log

class SeekBarUpdateAgent {

    private val handler = Handler()

    /**
     * Start updating the seekbar
     *
     *
     */
    fun enable(activity: MainActivity) {

        activity.runOnUiThread(
                object : Runnable {

                    override fun run() {
                        Log.d(TAG, "runOnUiThread run")

                        activity.updateSeekbarProgressAndTime()

                        // Add this runnable to the message queue
                        val queued = handler.postDelayed(this, 1000)
                        if(!queued) {
                            Log.d(TAG, "postDelayed runnable was not queued")
                        }
                    }
                })
    }

    /**
     * Stop updating the seekbar
     *
     *
     */
    fun disable() {

    }

    companion object {
        private val TAG = "SeekBarUpdateAgent"
    }
}