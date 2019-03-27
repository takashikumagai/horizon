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

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap

class RecyclerViewAdapter(
        /**
         * @brief Ref to MainActivity
         */
        private val mContext: Context)//, DirectoryNavigator directoryNavigator) {
//this.directoryNavigator = directoryNavigator;
    : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    private var currentFileSystemNavigator: FileSystemNavigator? = null

    //private DirectoryNavigator directoryNavigator;

    /**
     * @breif 0: filesystem, 1: play queue
     */
    internal var viewMode = 0

    override fun getItemCount(): Int {

            if (viewMode == 0) {
                if (currentFileSystemNavigator != null) {
                    return currentFileSystemNavigator!!.numCurrentDirectoryEntries
                } else {
                    Log.d(TAG, "gIC: !cFSN.")
                    return 0
                }
            } else if (viewMode == 1) {
                val mainActivity = mContext as MainActivity
                val tab = mainActivity.getCurrentlySelectedMediaPlayerTab()
                val count = tab?.playbackQueue?.mediaFilePathQueue?.size
                return if (count != null) count else 0
            } else {
                Log.e(TAG, "gIC !!vM")
                return 0
            }
        }

    fun refreshDirectoryContentList(directoryPath: String) {
        //directoryNavigator.setCurrentDirectory(directoryPath);
        //DirectoryNavigation.changeDirectory(directoryPath);
        notifyDataSetChanged()
    }

    fun setCurrentFileSystemNavigator(currentFileSystemNavigator: FileSystemNavigator?) {
        this.currentFileSystemNavigator = currentFileSystemNavigator
    }

    //@NonNull
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.v(TAG, "onCVH")
        val view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_listitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: " + position)

        if (viewMode == 0) {
            setFileOrDirectoryToHolder(holder, position)
        } else if (viewMode == 1) {
            setMediaInQueueToHolder(holder, position)
        } else {
            Log.e(TAG, "!!vM")
        }
    }

    /**
     * @brief Sets the specified drawabe as the file/folder icon
     *
     * @param drawableResourceId drawable ID or 0 to remove the background
     *
     */
    fun setFileTypeIcon(holder: ViewHolder, drawableResourceId: Int) {
        if (holder.fileTypeIcon != null) { // sanity check
            Log.v(TAG, "setting the audio file icon.")
            holder.fileTypeIcon!!.setBackgroundResource(drawableResourceId)
        }
    }

    fun setFileOrDirectoryToHolder(holder: ViewHolder, position: Int) {
        Log.v(TAG, "sFODTH: $position")
        //FileSystemNavigator currentFileSystemNavigator = null;

        val pos = holder.getAdapterPosition()

        if (currentFileSystemNavigator == null) {
            Log.d(TAG, "!cFSN")
            return
        }

        val entries = currentFileSystemNavigator!!.currentDirectoryEntries
        val entry = entries?.get(pos)
        if (entry == null) {
            Log.d(TAG, "onBVH: !entry")
            return
        }

        val mainActivity = mContext as MainActivity
        val st = mainActivity.getMediaFileStatus(entry.getPath())
        var color: Int = 0xffff00ff.toInt()
        if (st == 0) {
            color = 0xff2b2b2b.toInt()
        } else if (st == 1) { // playing
            color = 0xff314349.toInt()
        } else if (st == 2) { // queued
            color = 0xff2d393d.toInt()
        }
        holder.itemView.setBackgroundColor(color)

        // Set the icon based on the file type
        if (entry.isDirectory()) {

            // Set the name of the directory
            Log.d(TAG, "setting text: " + entry.getName())
            holder.fileName.setText(entry.getName())

            holder.secondaryRow.setText("Folder")

            setFileTypeIcon(holder, R.drawable.folder)

            holder.fileTypeIcon?.setText("")

        } else {
            // We are dealing with a file.


            // See if this one is a media file, e.g. mp3
            val isMediaFile = HorizonUtils.isMediaFile(entry.getName())
            if (isMediaFile) {
                setMediaFileToViewHolder(entry,holder)
            } else {
                // This should not happen as entries are supposed to contain
                // only directories and media files
                Log.w(TAG, "sFODTH !!!media file " + entry.getPath())
                setFileTypeIcon(holder, R.drawable.ic_file)
            }
        }

        val navigator = currentFileSystemNavigator

        holder.parentLayout.setOnClickListener() { v ->
            Log.d(TAG, "onClick: clicked")

            // Update the tab label
            mainActivity.setSelectedTabLabel(entry.getName())

            onEntryClickedInFileSystemViewMode(entry, pos, navigator, mainActivity)
        }
    }

    fun setMediaFileToViewHolder(entry: File, holder: ViewHolder) {

        var metaTags: HashMap<Int, String>? = HashMap<Int, String>()
        if(HorizonOptions.retrieveMediaMetadata) {
            val tags = intArrayOf(
                    MediaMetadataRetriever.METADATA_KEY_TITLE,
                    MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER,
                    MediaMetadataRetriever.METADATA_KEY_DURATION)
            metaTags = HorizonUtils.getMediaFileMetaTags(entry, tags)
        }

        if (HorizonOptions.showMetaTagTitles) {

            val title = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_TITLE)
            if (title != null && 0 < title.length) {
                // The media file has a meta tag; use the title instead of its file name
                holder.fileName.setText(title)
            } else {
                // Does not have the title tag; just set the file name
                holder.fileName.setText(entry.getName())
            }
        } else {
            // User explicitly chose to see file name instead of title
            holder.fileName.setText(entry.getName())
        }

        // Show the track number in the upper right corner of the media type icon
        // if the media file has the track number metadata
        val track = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
        if (track != null) {
            holder.fileTypeIcon?.setText(track)
        } else {
            holder.fileTypeIcon?.setText("")
        }

        setFileTypeIcon(holder,R.drawable.audio_file)

        // Track duration
        var secondRow = ""
        val duration = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_DURATION)
        if (duration != null) {
            val hhmmss = HorizonUtils.millisecondsToHhmmssOrMmss(duration.toLong())
            secondRow = "$hhmmss"
        }
        holder.secondaryRow.setText(secondRow)
    }

    fun clearViewHolder(holder: ViewHolder) {
        holder.fileTypeIcon?.setText("")
        holder.fileName.setText("")
        holder.secondaryRow.setText("")
    }



    fun onEntryClickedInFileSystemViewMode(entry: File?,
                                           pos: Int,
                                           navigator: FileSystemNavigator?,
                                           mainActivity: MainActivity) {
        if (entry!!.isDirectory()) {
            navigator?.moveToChild(pos)

            // Reset the scroll position when opening a directory
            val recyclerView: RecyclerView = mainActivity.findViewById(R.id.recycler_view)
            recyclerView.scrollToPosition(0)

            refreshDirectoryContentList(entry.getPath())

            mainActivity.updateFloatingActionButtonVisibility()

        } else if (entry.isFile()) {
            if (HorizonUtils.isMediaFile(entry.getName())) {
                // A playable media file, e.g. an mp3 fle, was tapped/clicked
                Log.d(TAG, "is media file")
                val player = mainActivity.playerOfSelectedTab
                if (player == null) {
                    Log.d(TAG, "!player")
                    return
                }
                val readyToPlay = mainActivity.onMediaStartRequestedOnScreen()
                if (!readyToPlay) {
                    Log.d(TAG, "!rTP")
                    return
                }
                player.clearQueue()
                player.addToQueue(entry.getPath())
                player.resetSavedPlaybackPosition()
                player.playCurrentlyPointedMediaInQueue()
                mainActivity.onMediaStartedOnScreen()

                mainActivity.showPlayingTrackControl()

                notifyDataSetChanged()

                //resetBackgroundColors(view);

                // Change the BG color of the playing track.
                //view.setBackgroundColor(0xffb4d296);

                //Button btn = (Button)findViewById(R.id.play_pause);
                //btn.setText("||");
                //Log.d(TAG, "started playing");
            }
        }
    }

    fun onEntryClickedInPlayQueueMode(entry: File?, pos: Int) {

        val mainActivity = mContext as MainActivity

        // Jump to the tapped/clicked track
        val mediaPlayerTab
                = mainActivity.mediaPlayerTabs.get(mainActivity.currentPlayerIndex)

        if(mediaPlayerTab == null) {
            Log.w(TAG,"oECIPQM !mPT")
        } else {
            mediaPlayerTab.playbackQueue.playTrackAt(pos)
        }

        notifyDataSetChanged()
    }

    fun setMediaInQueueToHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "sMIQTH")
        //FileSystemNavigator currentFileSystemNavigator = null;

        val pos = holder.getAdapterPosition()

        val mainActivity = mContext as MainActivity
        val tabs = mainActivity.mediaPlayerTabs
        val mediaPlayerTab = tabs.get(mainActivity.currentPlayerIndex)

        val queue = mediaPlayerTab.playbackQueue.mediaFilePathQueue
        if (pos < 0) { // Sanity check
            Log.w(TAG, "sMIQTH pos: $pos")
            return
        }

        if(queue.size <= pos) {
            // Make sure that the row is empty
            holder.fileName.setText("")
            holder.secondaryRow.setText("")
            holder.itemView.setBackgroundColor(0xff2b2b2b.toInt())
            setFileTypeIcon(holder,0)
            return
        }

        val path = queue.get(pos)
        Log.d(TAG, "sMIQTH " + path)

        val f = File(path)
        if (f != null) {
            setMediaFileToViewHolder(f,holder)
        } else {
            clearViewHolder(holder)
        }

        var color = 0xff2b2b2b.toInt()
        var text = ""
        if(pos == mediaPlayerTab.playbackQueue.pointedMediaIndex) {
            color = 0xff2d393d.toInt() // playing
            text = " - Playing"
        } else {
            color = 0xff2b2b2b.toInt() // queued
        }
        holder.itemView.setBackgroundColor(color)
        //holder.secondaryRow.setText(holder.secondaryRow.getText() + text)

        holder.parentLayout.setOnClickListener() { v ->
            Log.d(TAG, "playlist onClick " + f.getName())

            onEntryClickedInPlayQueueMode(f, pos)
        }
    }


    //    private void resetBackgroundColors(View view) {
    //        ViewParent vp = view.getParent();
    //        RecyclerView recyclerView = vp.findViewById(R.id.recycler_view);
    //        if(recyclerView == null) {
    //            Log.d(TAG, "!rV");
    //            return;
    //        }
    //        int count = recyclerView.getLayoutManager().getItemCount();
    //        Log.d(TAG, "item_count: " + count );
    //    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var fileTypeIcon: TextView? = null

        internal var fileName: TextView

        internal var secondaryRow: TextView

        internal var parentLayout: ConstraintLayout

        init {

            fileName = itemView.findViewById(R.id.file_name)
            secondaryRow = itemView.findViewById(R.id.secondary_row)
            parentLayout = itemView.findViewById(R.id.parent_layout)

            // Icon for showing the file type
            fileTypeIcon = itemView.findViewById(R.id.file_type_icon)

            Log.v(TAG, "vh ctor fileName: $fileName")
        }
    }

    companion object {

        private val TAG = "RecyclerViewAdapter"
    }
}
