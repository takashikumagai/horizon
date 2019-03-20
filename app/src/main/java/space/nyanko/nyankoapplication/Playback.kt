package space.nyanko.nyankoapplication

import android.util.Log
import android.media.MediaPlayer
import android.widget.TextView

import java.io.File
import java.io.Serializable
import java.io.IOException
import java.util.ArrayList

/**
 * \brief Stors the playback information
 *
 * - TODO: rename this to MyMediaPlayer
 * - Maps to each tab.
 * - There are as many playback instanes as the number of tabs.
 *
 */
//    private int tabViewMode = 0;


class Playback : Serializable {

    // 0 stopped
    // 1 playing
    // 2 paused
    internal var state = 0

    /**
     * @brief Index to the currently played or paused media file in queue
     */
    var pointedMediaIndex = -1

    // Play position in milliseconds
    private var playbackPosition = 0

    /**
     * \brief Stores pathnames of playable media files (e.g. .mp3, .wav)
     *
     * Files are played starting from the first element to the last element
     */
    var mediaFilePathQueue = ArrayList<String>()

    // Views (not serialized)
    // TODO there has to be a better way to update view from outside the activity
    @Transient
    private var recyclerViewAdapter: RecyclerViewAdapter? = null
    @Transient
    private var playingTrackName: TextView? = null

    val currentlyPlayedMediaName: String?
        get() {
            val path = currentlyPlayedMediaPath
            return if (path != null) {
                File(path).getName()
            } else {
                null
            }
        }

    val currentlyPlayedMediaPath: String?
        get() {
            if (0 <= pointedMediaIndex && pointedMediaIndex < mediaFilePathQueue.size) {
                return File(mediaFilePathQueue.get(pointedMediaIndex)).getPath()
            } else {
                return null
            }
        }

    fun clearQueue() {
        Log.d(TAG, "clearing queue")
        mediaFilePathQueue.clear()
        pointedMediaIndex = -1
    }

    fun startCurrentlyPointedMediaInQueue() {
        Log.d(TAG, "sCPMIQ")

        val mediaPlayer = mediaPlayer
        if (mediaPlayer == null) {
            Log.w(TAG, "!mP")
            return
        }

        if (pointedMediaIndex < 0) {
            Log.d(TAG, "pMI<0")
            return
        }

        if (mediaFilePathQueue.size <= pointedMediaIndex) {
            Log.d(TAG, "q.size<=pMI")
            return
        }

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop()
        }

        var mediaFilepath = ""
        try {
            mediaFilepath = mediaFilePathQueue.get(pointedMediaIndex)

            val service = BackgroundAudioService.instance
            if (service != null) {
                service.setMetadata()
            }

            Log.d(TAG, "resetting")
            mediaPlayer.reset()
            Log.d(TAG, "setDataSource: $mediaFilepath")
            mediaPlayer.setDataSource(mediaFilepath)
            Log.d(TAG, "prepare")
            mediaPlayer.prepare()
            Log.d(TAG, "starting")
            mediaPlayer.start()
        } catch (ioe: IOException) {
            Log.e(TAG, "caught an IO exception: " + ioe.toString() + " File: " + mediaFilepath)
        } catch (e: Exception) {
            Log.e(TAG, "caught an exception: " + e.toString() + " File: " + mediaFilepath)
        }

        // Repaint GUI as a currently played track has just been changed.
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter!!.notifyDataSetChanged()
        } else {
            Log.e(TAG, "!rVA")
        }

        if (playingTrackName != null) {
            val f = File(mediaFilepath)
            playingTrackName!!.setText(f.getName())
        }
    }

    fun playPrevTrack(): Boolean {
        if (pointedMediaIndex <= 0) {
            return false
        }

        pointedMediaIndex -= 1

        startCurrentlyPointedMediaInQueue()

        return true
    }

    fun playNextTrack(): Boolean {
        if (pointedMediaIndex < 0) {
            return false
        }
        if (mediaFilePathQueue.size - 1 <= pointedMediaIndex) {
            return false
        }

        pointedMediaIndex += 1

        startCurrentlyPointedMediaInQueue()

        return true
    }

    fun addToQueue(mediaFilePath: String) {
        Log.d(TAG, "addToQueue")
        if (500 <= mediaFilePathQueue.size) {
            Log.w(TAG, "Too many songs/vids in the queue (they reached 500).")
        }
        mediaFilePathQueue.add(mediaFilePath)

        if (mediaFilePathQueue.size == 1) {
            // Queue just got one media file; point to it
            pointedMediaIndex = 0
        }
    }

    fun addToQueue(mediaFiles: ArrayList<File>) {
        for (file in mediaFiles) {
            mediaFilePathQueue.add(file.getPath())
        }

        if (0 < mediaFiles.size && pointedMediaIndex == -1) {
            pointedMediaIndex = 0
        }
    }
    //    public void startFirstInQueue() {
    //        if(mediaFilePathQueue.size == 0) {
    //            Log.d(TAG, "queue is empty");
    //            return;
    //        }
    //
    //        startCurrentlyPointedMediaInQueue();
    //    }

    /**
     * \brief Pop front first and then play the one 2nd from the head
     *
     * Note that this function removes the first eleemnt from the queue
     */
    //    public void startNextInQueue() {
    //        if(mediaFilePathQueue.size == 0) {
    //            return;
    //        }
    //
    //        // Pop the first eleemnt
    //        mediaFilePathQueue.remove(0);
    //
    //        startFirstInQueue();
    //    }

    fun isPointed(filePath: String): Boolean {
        if (pointedMediaIndex < 0 || mediaFilePathQueue.size <= pointedMediaIndex) {
            return false
        }

        return if (mediaFilePathQueue.get(pointedMediaIndex).equals(filePath)) {
            true
        } else false

    }

    fun isInQueue(filePath: String): Boolean {
        for (pathInQueue in mediaFilePathQueue) {
            if (pathInQueue.equals(filePath)) {
                return true
            }
        }

        return false
    }


    // Play the next media in queue

    fun onCompletion(mp: MediaPlayer) {
        Log.d(TAG, "oC pMI: $pointedMediaIndex")
        playNextTrack()
    }

    fun setRecyclerViewAdapter(adapter: RecyclerViewAdapter?) {
        recyclerViewAdapter = adapter
    }

    fun setPlayingTrackName(view: TextView?) {
        playingTrackName = view
    }

    fun saveCurrentPlaybackPosition() {
        if (mediaPlayer == null) {
            Log.w(TAG, "sCPP !mP")
        }

        playbackPosition = mediaPlayer!!.getCurrentPosition()
    }

    fun pause() {
        if (mediaPlayer == null) {
            return
        }

        mediaPlayer!!.pause()
        playbackPosition = mediaPlayer!!.getCurrentPosition()
    }

    fun resume() {
        if (mediaPlayer == null) {
            return
        }

        mediaPlayer!!.seekTo(playbackPosition)
        mediaPlayer!!.start()
    }

    companion object {

        private val TAG = "Playback"

        /**
         * Set by the service
         */
        var mediaPlayer: MediaPlayer? = null
    }
}
