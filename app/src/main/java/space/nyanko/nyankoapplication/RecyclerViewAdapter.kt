package space.nyanko.nyankoapplication

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

import java.io.File
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
        holder.fileName.setTextColor(ContextCompat.getColor(mContext,R.color.textColorPrimary))

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

        holder.parentLayout.setOnClickListener {
            Log.d(TAG, "onClick")

            onEntryClickedInFolderView(entry, pos, navigator, mainActivity)
        }

        holder.fileTypeIcon?.setOnLongClickListener {
            Log.d(TAG, "fTI onLC")

            if(entry.isDirectory) {
                // Do nothing
                false
            } else {
                mainActivity.openMediaInfoPopup(entry)
                true
            }
        }

        // The setOnLongClickListener above somehow disables the onclick listener
        // over the parentLayout so had to add this
        holder.fileTypeIcon?.setOnClickListener {
            Log.d(TAG, "fTI onClick")

            onEntryClickedInFolderView(entry, pos, navigator, mainActivity)
        }
    }

    fun setMediaFileToViewHolder(entry: File, holder: ViewHolder) {
        Log.d(TAG, "sMFTVH " + entry.path)

        // Just set the filename to each holder for now and
        // update it to metadata title asynchronously if needed
        holder.fileName.text = entry.name

        holder.secondaryRow.text = "-"

        setFileTypeIcon(holder,R.drawable.audio_file)

        val cache = getMetadataCache()
        if(cache != null && 0 < cache.tags.size) {
            val metaTags = cache.tags[entry.name]
            if(metaTags != null) {
                updateMediaMetadata(entry,metaTags,holder,false)
            } else {
                MediaMetadataUpdateManager.updateMediaMetadataDeferred(entry,holder)
            }
        } else {
            MediaMetadataUpdateManager.updateMediaMetadataDeferred(entry,holder)
        }
    }

    fun clearViewHolder(holder: ViewHolder) {
        holder.fileTypeIcon?.setText("")
        holder.fileName.setText("")
        holder.secondaryRow.setText("")
    }



    fun onEntryClickedInFolderView(entry: File?,
                                           pos: Int,
                                           navigator: FileSystemNavigator?,
                                           mainActivity: MainActivity) {

        // Update the tab label
        mainActivity.setSelectedTabLabel(entry?.name ?: "-")

        if (entry!!.isDirectory()) {
            navigator?.moveToChild(pos)

            MediaPlayerTab.getSelected()?.metadataCache?.clear()

            mainActivity.setTitle(navigator?.currentDirectoryName ?: "")

            // Reset the scroll position when opening a directory
            val recyclerView: RecyclerView = mainActivity.findViewById(R.id.recycler_view)
            recyclerView.scrollToPosition(0)

            notifyDataSetChanged()

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
                if(HorizonOptions.autoQueueMediaFiles) {
                    queueMediaFilesInDirectory(entry, player)
                } else {
                    // Note that this is the default behavior
                    player.addToQueue(entry.getPath())
                }
                player.resetSavedPlaybackPosition()
                val started = player.playCurrentlyPointedMediaInQueue()
                if(started) {
                    mainActivity.onMediaStartedOnScreen()
                    mainActivity.switchToPlaylistView()
                    //mainActivity.showPlayingTrackControl()
                } else {
                    Log.d(TAG, "oECIFSV !started")
                }

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

    private fun queueMediaFilesInDirectory(entry: File, player: Playback) {
        val filesAndDirs = File(entry.parent).listFiles().toCollection(ArrayList())
        var mediaFiles = HorizonUtils.pickMediaFiles(filesAndDirs)
        if(mediaFiles.isEmpty()) {
            Log.e(TAG, "!!!mediaFiles")
            player.addToQueue(ArrayList<File>())
            player.setPointedMediaIndex(-1)
            return
        }
        mediaFiles.sortWith(compareBy {it.name})

        player.addToQueue(mediaFiles)

        val index = mediaFiles.indexOfFirst { f -> f.path == entry.path }
        if(index == -1) {
            Log.e(TAG, "Clicked media not found")
            player.setPointedMediaIndex(0)
            return;
        } else {
            player.setPointedMediaIndex(index)
        }
    }

    fun onEntryClickedInPlaylistView(pos: Int) {

        val mainActivity = mContext as MainActivity

        // Jump to the tapped/clicked track
        val mediaPlayerTab
                = mainActivity.mediaPlayerTabs.get(mainActivity.selectedTabIndex)

        if(mediaPlayerTab == null) {
            Log.w(TAG,"oECIPQM !mPT")
        } else {
            val started = mediaPlayerTab.playbackQueue.playTrackAt(pos)
            if(!started) {
                Log.d(TAG,"oECIPQM !started")
            }
        }

        notifyDataSetChanged()
    }

    fun setMediaInQueueToHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "sMIQTH: " + position)
        //FileSystemNavigator currentFileSystemNavigator = null;

        val mainActivity = mContext as MainActivity
        val tabs = mainActivity.mediaPlayerTabs
        val mediaPlayerTab = tabs.get(mainActivity.selectedTabIndex)

        val queue = mediaPlayerTab.playbackQueue.mediaFilePathQueue
        if (position < 0) { // Sanity check
            Log.w(TAG, "sMIQTH pos: $position")
            return
        }

        if(queue.size <= position) {
            // Make sure that the row is empty
            holder.fileName.setText("")
            holder.secondaryRow.setText("")
            holder.itemView.setBackgroundColor(0xff2b2b2b.toInt())
            setFileTypeIcon(holder,0)
            return
        }

        val path = queue.get(position)
        Log.d(TAG, "sMIQTH " + path)

        val f = File(path)
        if (f != null) {
            setMediaFileToViewHolder(f,holder)
        } else {
            clearViewHolder(holder)
        }

        var color: Int
        var textColor: Int
        if(position == mediaPlayerTab.playbackQueue.getPointedMediaIndex()) {
            //color = 0xff2d393d.toInt() // playing
            color = 0xff2b2b2b.toInt() // Use the same color for playing track
            textColor = R.color.colorAccent
        } else {
            color = 0xff2b2b2b.toInt() // queued
            textColor = R.color.textColorPrimary
        }
        holder.itemView.setBackgroundColor(color)

        holder.fileName.setTextColor(ContextCompat.getColor(mContext,textColor))

        holder.parentLayout.setOnClickListener() { _ ->
            val pos = holder.getAdapterPosition()

            Log.d(TAG, "playlist onClick " + pos + " " + f.getName())

            onEntryClickedInPlaylistView(pos)
        }
    }

    private fun getMetadataCache(): MetadataCache? {
        return MediaPlayerTab.getSelected()?.metadataCache
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

        // item views change on scroll so remembering the path like this does not work.
        //internal var path: String = ""

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

        private const val TAG = "RecyclerViewAdapter"

        @JvmStatic
        fun updateMediaMetadata(entry: File,
                                metaTags: HashMap<Int, String>?,
                                holder: ViewHolder,
                                cacheTags: Boolean) {

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

            // Track duration
            var secondRow = ""
            val duration = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (duration != null) {
                val hhmmss = HorizonUtils.millisecondsToHhmmssOrMmss(duration.toLong())
                secondRow = "$hhmmss"
            }
            holder.secondaryRow.setText(secondRow)

            if(cacheTags) {
                val tab = MediaPlayerTab.getSelected()
                if(metaTags != null) {
                    tab?.metadataCache?.tags?.put(entry.name, metaTags)
                }
            }
        }
    }
}
