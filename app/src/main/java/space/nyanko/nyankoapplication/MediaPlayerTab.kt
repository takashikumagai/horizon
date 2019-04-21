package space.nyanko.nyankoapplication

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaMetadataRetriever
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import android.widget.TextView
import android.widget.Toast

import java.io.File
import java.io.Serializable
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap

class MediaPlayerTab internal constructor() : Serializable {

    val fileSystemNavigator: FileSystemNavigator

    /**
     * @breif 0: filesystem, 1: play queue
     */
    var viewMode = 0

    val playbackQueue: Playback

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
}