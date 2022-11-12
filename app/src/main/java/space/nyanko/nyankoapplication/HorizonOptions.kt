package space.nyanko.nyankoapplication

import android.content.SharedPreferences
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
    private const val retrieveMediaMetadataKey = "retrieve_media_metadata"
    private const val showMetaTagTitlesKey = "show_meta_tag_titles"
    private const val autoQueueMediaFilesKey = "auto_queue_media_files"

//    private val OPTIONS_FILE = "horizon-options"

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

    fun saveOptionsToFile(pref: SharedPreferences) {
        Log.d(TAG, "sOTF")
//        val file = File(parentDir, OPTIONS_FILE)
        with (pref.edit()) {
            putBoolean(retrieveMediaMetadataKey, retrieveMediaMetadata)
            putBoolean(showMetaTagTitlesKey, showMetaTagTitles)
            putBoolean(autoQueueMediaFilesKey, autoQueueMediaFiles)
            apply()
        }
    }

    fun loadOptionsFromFile(pref: SharedPreferences) {
        Log.d(TAG, "lOFF")
        retrieveMediaMetadata = pref.getBoolean(retrieveMediaMetadataKey, true)
        showMetaTagTitles = pref.getBoolean(showMetaTagTitlesKey, true)
        autoQueueMediaFiles = pref.getBoolean(autoQueueMediaFilesKey, false)
    }
}
