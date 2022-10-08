package space.nyanko.nyankoapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

import java.io.File
import java.util.concurrent.TimeUnit

import android.media.MediaMetadataRetriever
import java.lang.Exception
import java.util.*

object HorizonUtils {

    private const val TAG = "HorizonUtils"

    private fun createMediaMetadataRetriever(filePath: String?): MediaMetadataRetriever {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(filePath)
        } catch (e: Exception) {
            Log.d(TAG, "exception: ${e.message}")
        } finally {
            Log.d(TAG, "An exception was thrown")
        }
        return mmr
    }

    fun getExtension(fileName: String): String {
        val i = fileName.lastIndexOf('.')
        return if (i > 0) {
            fileName.substring(i + 1)
        } else {
            ""
        }
    }

    fun isMediaFile(fileName: String): Boolean {
        val ext = getExtension(fileName).lowercase(Locale.getDefault())
        return if (ext.equals("mp3") || ext.equals("ogg") || ext.equals("flac") || ext.equals("wav") || ext.equals("m4a") || ext.equals("aac")) {
            true
        } else {
            false
        }
    }

    fun pickMediaFiles(filesAndDirs: ArrayList<File>?): ArrayList<File> {

        if(filesAndDirs == null) {
            return ArrayList<File>()
        }

        val mediaFiles = ArrayList<File>()
        for (entry in filesAndDirs) {
            if (entry.isFile && isMediaFile(entry.getName())) {
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

        if (isMediaFile(f.getName())) {
            val mmr = createMediaMetadataRetriever(f.path)
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
//        Log.d(TAG, "gMFMTs file: ${f?.path}")

        val tagMaps = HashMap<Int, String>()

        if (f == null) {
            return null
        }

        if (isMediaFile(f.getName())) {
            val mmr = createMediaMetadataRetriever(f.path)
            for (tag in tags) {
                val value = mmr.extractMetadata(tag) ?: continue
                tagMaps.put(tag, value)
            }
        } else {
            Log.d(TAG, "not a media file")
        }
        return tagMaps
    }

    fun getEmbeddedPicture(mediaFilePath: String?): Bitmap? {

        val mmr = createMediaMetadataRetriever(mediaFilePath)
        val data = mmr.embeddedPicture
        return if(data != null) BitmapFactory.decodeByteArray(data, 0, data.size) else null
    }

    fun millisecondsToHhmmss(milliseconds: Long): String {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(milliseconds),
                TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)))
    }

    /**
     * @brief Returns a string representing time in the MM:SS format
     *        if the argument is shorter than an hour, or in the
     *        HH:MM:SS format if it's longer than an hour.
     *
     */
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
