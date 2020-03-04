package space.nyanko.nyankoapplication

import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File
import java.util.HashMap

const val UPDATE_STATE_STARTED: Int = 9000
const val UPDATE_STATE_COMPLETED: Int = 9001
const val UPDATE_STATE_FAILED: Int = 9002

class MediaMetadataUpdateRunnable(
        private val mediaMetadataUpdateTask: MediaMetadataUpdateTask
) : Runnable {

    fun retrieveMetadata(entry: File) {
        mediaMetadataUpdateTask.metaTags = HashMap<Int, String>()
        if(HorizonOptions.retrieveMediaMetadata) {
            val tags = intArrayOf(
                    MediaMetadataRetriever.METADATA_KEY_TITLE,
                    MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER,
                    MediaMetadataRetriever.METADATA_KEY_DURATION)
            mediaMetadataUpdateTask.metaTags = HorizonUtils.getMediaFileMetaTags(entry, tags)
        }
    }

    override fun run() {
        Log.d(TAG,"run t" + Thread.currentThread().id)

        mediaMetadataUpdateTask.handleUpdateState(UPDATE_STATE_STARTED)

        val entry = mediaMetadataUpdateTask.entry
        if(entry == null) {
            mediaMetadataUpdateTask.handleUpdateState(UPDATE_STATE_FAILED)
            return
        }

        retrieveMetadata(entry)

        mediaMetadataUpdateTask.handleUpdateState(UPDATE_STATE_COMPLETED)
    }

    companion object {

        private const val TAG = "MetadataUpdateRunnable"
    }
}