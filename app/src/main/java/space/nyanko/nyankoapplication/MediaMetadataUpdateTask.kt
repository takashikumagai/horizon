package space.nyanko.nyankoapplication

import android.media.MediaMetadataRetriever
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

        if (HorizonOptions.showMetaTagTitles) {

            val title = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_TITLE)
            if (title != null && 0 < title.length) {
                // The media file has a meta tag; use the title instead of its file name
                holder.fileName.setText(title)
            } else {
                // Does not have the title tag; just set the file name
                holder.fileName.setText(entry.getName())
            }
        } else {
            // User explicitly chose to see file name instead of title
            holder.fileName.setText(entry.getName())
        }

        // Show the track number in the upper right corner of the media type icon
        // if the media file has the track number metadata
        val track = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
        if (track != null) {
            holder.fileTypeIcon?.setText(track)
        } else {
            holder.fileTypeIcon?.setText("")
        }

        // Track duration
        var secondRow = ""
        val duration = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_DURATION)
        if (duration != null) {
            val hhmmss = HorizonUtils.millisecondsToHhmmssOrMmss(duration.toLong())
            secondRow = "$hhmmss"
        }
        holder.secondaryRow.setText(secondRow)
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