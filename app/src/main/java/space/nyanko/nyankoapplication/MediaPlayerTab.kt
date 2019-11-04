package space.nyanko.nyankoapplication

import java.io.Serializable
import java.util.ArrayList

class MediaPlayerTab internal constructor() : Serializable {

    var fileSystemNavigator: FileSystemNavigator

    /**
     * @breif 0: filesystem, 1: play queue
     */
    var viewMode = 0

    var playbackQueue: Playback

    var metadataCache: MetadataCache = MetadataCache()

    init {
        fileSystemNavigator = FileSystemNavigator()
        fileSystemNavigator.initialize()

        playbackQueue = Playback()
    }

    companion object {
        private val TAG = "MediaPlayerTab"

        var tabs: ArrayList<MediaPlayerTab> = ArrayList()

        var selectedTabIndex = -1

        @JvmStatic
        fun getSelected(): MediaPlayerTab? {
            if(0 <= selectedTabIndex && selectedTabIndex < tabs.size) {
                return tabs[selectedTabIndex]
            } else {
                return null
            }
        }
    }

    /**
     * @brief Clones the tab
     *
     * - Special logic for selectively copying fields and cloning tabs
     * -
     */
    fun cloneTab(): MediaPlayerTab {

        var tab = MediaPlayerTab()

        tab.fileSystemNavigator = this.fileSystemNavigator.cloneNavigator()

        tab.viewMode = 0

        return tab
    }
}