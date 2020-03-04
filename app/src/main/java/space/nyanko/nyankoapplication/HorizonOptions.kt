package space.nyanko.nyankoapplication

import android.util.Log
import java.io.*

/**
 * \brief Stors the playback information
 *
 * -
 *
 */
object HorizonOptions {

    private val TAG = "HorizonOptions"

    private val OPTIONS_FILE = "horizon-options"

    /**
     * @brief Whether or not to attempt to metadata from media files.
     *
     *
     *
     */
    var retrieveMediaMetadata = true

    /**
     * @brief whether to extract metadata from media files.
     *
     * - true: shows the meta tag title, falls back to the file name if
     *
     */
    var showMetaTagTitles = true

    var autoQueueMediaFiles = false

    fun saveOptionsToFile(parentDir: File) {
        Log.d(TAG, "sOTF")
        val file = File(parentDir, OPTIONS_FILE)
        try {
            val f = FileOutputStream(file.path)
            val stream = ObjectOutputStream(f)

            stream.writeBoolean(retrieveMediaMetadata)
            stream.writeBoolean(showMetaTagTitles)
            stream.writeBoolean(autoQueueMediaFiles)

            stream.close()
        } catch (e: Exception) {
            Log.e(TAG, "sOTF!: " + e.toString())
        }
    }

    fun loadOptionsFromFile(parentDir: File) {
        Log.d(TAG, "lOFF")
        val file = File(parentDir, OPTIONS_FILE)
        try {
            val f = FileInputStream(file.path)
            val stream = ObjectInputStream(f)

            retrieveMediaMetadata = stream.readBoolean()
            showMetaTagTitles = stream.readBoolean()
            autoQueueMediaFiles = stream.readBoolean()

            stream.close()
        } catch (e: Exception) {
            Log.e(TAG, "lOFF!: " + e.toString())
        }
    }
}
