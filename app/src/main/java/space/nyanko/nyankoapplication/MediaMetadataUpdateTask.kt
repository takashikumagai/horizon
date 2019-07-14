package space.nyanko.nyankoapplication

import android.util.Log
import java.io.File
import java.util.HashMap

class MediaMetadataUpdateTask {

    var entry: File? = null

    var holder: RecyclerViewAdapter.ViewHolder? = null

    var metaTags: HashMap<Int, String>? = null

    var runnable: Runnable = MediaMetadataUpdateRunnable(this)

    init {
        Log.d(TAG,"init")
    }

    fun updateMediaMetadata(entry: File, holder: RecyclerViewAdapter.ViewHolder) {

        RecyclerViewAdapter.updateMediaMetadata(entry,metaTags,holder,true)
    }

    fun handleUpdateState(state: Int) {
        Log.d(TAG,"hUS: " + state)
        val outState: Int = when(state) {
            UPDATE_STATE_COMPLETED -> MediaMetadataUpdateManager.TASK_COMPLETE
            else -> {MediaMetadataUpdateManager.TASK_INVALID_STATE}
        }

        MediaMetadataUpdateManager.handleState(this, outState)
    }

    fun initializeTask(entry: File, holder: RecyclerViewAdapter.ViewHolder) {
        this.entry = entry
        this.holder = holder
    }

    fun clear() {
        this.entry = null
        this.holder = null
    }

    companion object {

        private val TAG = "MediaMetadataUpdateTask"
    }
}