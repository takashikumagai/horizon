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

    fun resetSavedPlaybackPosition() {
        playbackPosition = 0
    }

    /**
     * @brief Plays the track which is at the head in the queue
     *
     * Plays from the start or resumes the playback at the last paused position
     *
     */
    fun startCurrentlyPointedMediaInQueue(resume: Boolean): Boolean {
        Log.d(TAG, "sCPMIQ")

        val mediaPlayer = mediaPlayer
        if (mediaPlayer == null) {
            Log.w(TAG, "!mP")
            return false
        }

        if (pointedMediaIndex < 0) {
            Log.d(TAG, "pMI<0")
            return false
        }

        if (mediaFilePathQueue.size <= pointedMediaIndex) {
            Log.d(TAG, "q.size<=pMI")
            return false
        }

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop()
        }

        var started = false
        var mediaFilepath = ""
        try {
            mediaFilepath = mediaFilePathQueue.get(pointedMediaIndex)

            val service = BackgroundAudioService.instance
            if (service != null) {
                service.setMetadata()
            } else {
                return false
            }

            Log.d(TAG, "resetting")
            mediaPlayer.reset()
            Log.d(TAG, "setDataSource: $mediaFilepath")
            mediaPlayer.setDataSource(mediaFilepath)
            Log.d(TAG, "prepare")
            mediaPlayer.prepare()
            if(resume) {
                // Do nothing; service.play() seeks to the current position
            } else {
                playbackPosition = 0
            }

            // Acquire audio focus and if it succeeds, start playing
            Log.d(TAG, "starting")
            started = service.play()

        } catch (ioe: IOException) {
            Log.e(TAG, "caught an IO exception: " + ioe.toString() + " File: " + mediaFilepath)
        } catch (e: Exception) {
            Log.e(TAG, "caught an exception: " + e.toString() + " File: " + mediaFilepath)
        } finally {
            // Repaint GUI as a currently played track has just been changed.
            // or the failed-to-start track might need to show the status on its view
            if (recyclerViewAdapter != null) {
                recyclerViewAdapter!!.notifyDataSetChanged()
            } else {
                Log.e(TAG, "!rVA")
            }
        }

        if(started) {
            if (playingTrackName != null) {
                val f = File(mediaFilepath)
                playingTrackName!!.setText(f.getName())
            }
        }

        return started
    }

    fun playCurrentlyPointedMediaInQueue(): Boolean {
        return startCurrentlyPointedMediaInQueue(false)
    }

    fun resumeCurrentlyPointedMediaInQueue(): Boolean {
        return startCurrentlyPointedMediaInQueue(true)
    }

    fun playPrevTrack(): Boolean {
        if (pointedMediaIndex <= 0) {
            return false
        }

        pointedMediaIndex -= 1

        return playCurrentlyPointedMediaInQueue()
    }

    fun playNextTrack(): Boolean {
        if (pointedMediaIndex < 0) {
            return false
        }
        if (mediaFilePathQueue.size - 1 <= pointedMediaIndex) {
            return false
        }

        pointedMediaIndex += 1

        return playCurrentlyPointedMediaInQueue()
    }

    /**
     * @brief Start playing the i-th track in the play queue
     *
     */
    fun playTrackAt(pos: Int): Boolean {
        if(pos < 0 || mediaFilePathQueue.size <= pos) {
            Log.w(TAG, "pTA !pos " + pos)
            return false
        }

        pointedMediaIndex = pos

        resetSavedPlaybackPosition()
        return playCurrentlyPointedMediaInQueue()
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
        Log.d(TAG, "aTQ " + mediaFiles.toString())
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
        playbackPosition = 0
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

        Log.d(TAG, "sCPP: " + mediaPlayer!!.getCurrentPosition())
        playbackPosition = mediaPlayer!!.getCurrentPosition()
    }

//    fun onPause() {
//        if (mediaPlayer == null) {
//            Log.w(TAG, "onPause !mP")
//            return
//        }
//
//        //mediaPlayer!!.pause()
//        playbackPosition = mediaPlayer!!.getCurrentPosition()
//    }

    fun seekToCurrentPosition() {
        Log.w(TAG, "sTCP " + playbackPosition)
        if (mediaPlayer == null) {
            Log.w(TAG, "seekToCurrentPosition !mP")
            return
        }

        mediaPlayer!!.seekTo(playbackPosition)
        //mediaPlayer!!.start()
    }

    companion object {

        private val TAG = "Playback"

        /**
         * Set by the service
         */
        var mediaPlayer: MediaPlayer? = null
    }
}
