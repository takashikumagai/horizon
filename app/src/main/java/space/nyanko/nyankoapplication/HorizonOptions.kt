package space.nyanko.nyankoapplication

import java.util.ArrayList

/**
 * \brief Stors the playback information
 *
 * -
 *
 */
object HorizonOptions {

    private val TAG = "HorizonOptions"

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
}
