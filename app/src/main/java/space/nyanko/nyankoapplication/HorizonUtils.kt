package space.nyanko.nyankoapplication

import android.util.Log

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.concurrent.TimeUnit

import android.media.MediaMetadataRetriever

object HorizonUtils {

    private val TAG = "HorizonUtils"

    fun getExtension(fileName: String): String {
        val i = fileName.lastIndexOf('.')
        return if (i > 0) {
            fileName.substring(i + 1)
        } else {
            ""
        }
    }

    fun isMediaFile(fileName: String): Boolean {
        val ext = getExtension(fileName)
        return if (ext.equals("mp3") || ext.equals("ogg") || ext.equals("flac") || ext.equals("wav")) {
            true
        } else {
            false
        }
    }

    fun pickMediaFiles(filesAndDirs: ArrayList<File>): ArrayList<File> {
        val mediaFiles = ArrayList<File>()
        for (entry in filesAndDirs) {
            if (isMediaFile(entry.getName())) {
                mediaFiles.add(entry)
            }
        }

        return mediaFiles
    }

    /**
     * @brief Returns the title metadata
     *
     * @param f
     * @return Title or null if the media tag was not found.
     */
    fun getMediaFileTitle(f: File?): String? {
        Log.d(TAG, "gMFT")

        if (f == null) {
            return ""
        }

        if (isMediaFile(f!!.getName())) {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(f!!.getPath())
            val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            if (title == null) {
                Log.d(TAG, "!t")
                return null
            }

            return title
        }

        Log.d(TAG, "!iMF")
        return ""
    }

    /**
     * @brief Retrieves metadata tags from a media file, e.g. an mp3 file.
     *
     * @param f
     * @return a hashmap where each entry is an integer representing a tag and the tag value,
     * or null if arg is invalid
     */
    fun getMediaFileMetaTags(f: File?, tags: IntArray): HashMap<Int, String>? {
        Log.v(TAG, "gMFMTs")

        val tagMaps = HashMap<Int, String>()

        if (f == null) {
            return null
        }

        if (isMediaFile(f!!.getName())) {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(f!!.getPath())
            for (tag in tags) {
                val value = mmr.extractMetadata(tag)
                tagMaps.put(tag, value)
            }
        } else {
            Log.d(TAG, "not a media file")
        }
        return tagMaps
    }

    fun millisecondsToHhmmss(milliseconds: Long): String {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(milliseconds),
                TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)))
    }

    fun millisecondsToHhmmssOrMmss(milliseconds: Long): String {
        val h = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val min = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds))
        val s = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))

        return if (0 < h) {
            String.format("%02d:%02d:%02d", h, min, s)
        } else {
            String.format("%02d:%02d", min, s)
        }
    }
}