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
                val tabs = mainActivity.mediaPlayerTabs
                val tab = tabs.get(mainActivity.currentlyPlayedQueueIndex)
                return tab.playbackQueue.mediaFilePathQueue.size
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

    fun setCurrentFileSystemNavigator(currentFileSystemNavigator: FileSystemNavigator) {
        this.currentFileSystemNavigator = currentFileSystemNavigator
    }

    //@NonNull
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.v(TAG, "onCVH")
        val view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_listitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: called.")

        if (viewMode == 0) {
            setFileOrDirectoryToHolder(holder, position)
        } else if (viewMode == 1) {
            setMediaInQueueToHolder(holder, position)
        } else {
            Log.e(TAG, "!!vM")
        }
    }

    fun setFileOrDirectoryToHolder(holder: ViewHolder, position: Int) {
        Log.v(TAG, "sFODTH")
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
        val st = mainActivity.getMediaFileStatus(entry!!.getPath())
        var color = -0xff01
        if (st == 0) {
            color = -0x9090a
        } else if (st == 1) { // playing
            color = -0x513370
        } else if (st == 2) { // queued
            color = -0x4b2d6a
        }
        holder.itemView.setBackgroundColor(color)

        // Set the icon based on the file type
        if (entry!!.isDirectory()) {

            // Set the name of the directory
            Log.d(TAG, "setting text: " + entry!!.getName())
            holder.fileName.setText(entry!!.getName())

            if (holder.fileTypeIcon != null) {
                Log.v(TAG, "setting the round icon.")
                holder.fileTypeIcon!!.setImageResource(R.drawable.ic_file)
            }
        } else {
            // We are dealing with a file.

            // See if this one is a media file, e.g. mp3
            val isMediaFile = HorizonUtils.isMediaFile(entry!!.getName())
            if (isMediaFile) {
                if (HorizonOptions.showMetaTagTitles) {
                    val tags = intArrayOf(MediaMetadataRetriever.METADATA_KEY_TITLE, MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, MediaMetadataRetriever.METADATA_KEY_DURATION)
                    //                    String title = HorizonUtils.getMediaFileTitle(entry);
                    val metaTags = HorizonUtils.getMediaFileMetaTags(entry, tags)
                    val title = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    if (title != null && 0 < title.length) {
                        // The media file has a meta tag; use the title instead of its file name
                        holder.fileName.setText("[T] " + title!!)
                    } else {
                        // Does not have the title tag; just set the file name
                        holder.fileName.setText(entry!!.getName())
                    }

                    var secondRow = ""
                    val track = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                    if (track != null) {
                        secondRow = String.format("(%s)", track)
                    } else {
                        secondRow = "(-)"
                    }

                    val duration = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    if (duration != null) {
                        val hhmmss = HorizonUtils.millisecondsToHhmmss(duration.toLong())
                        secondRow += ", $hhmmss"
                    }
                    holder.secondaryRow.setText(secondRow)

                } else {
                    // Option is set not to get meta tag; just set the file name
                    holder.fileName.setText(entry!!.getName())
                }
            } else {
                // Do nothing; don't display it unless it's a media file.
            }
            if (holder.fileTypeIcon != null) {
                Log.v(TAG, "setting the square icon.")
                holder.fileTypeIcon!!.setImageResource(R.drawable.ic_file)
            }
        }

        val navigator = currentFileSystemNavigator

        holder.parentLayout.setOnClickListener(object : View.OnClickListener() {
            override fun onClick(view: View) {
                Log.d(TAG, "onClick: clicked")

                Toast.makeText(mContext, entry!!.getName(), Toast.LENGTH_SHORT).show()

                // Update the tab label
                mainActivity.setSelectedTabLabel(entry!!.getName())

                if (viewMode == 0) {
                    onEntryClickedInFileSystemViewMode(entry, pos, navigator, mainActivity)
                } else if (viewMode == 1) {
                    onEntryClickedInPlayQueueMode(entry)
                }
            }
        })
    }

    fun onEntryClickedInFileSystemViewMode(entry: File?,
                                           pos: Int,
                                           navigator: FileSystemNavigator?,
                                           mainActivity: MainActivity) {
        if (entry!!.isDirectory()) {
            navigator?.moveToChild(pos)
            refreshDirectoryContentList(entry!!.getPath())

            mainActivity.updateFloatingActionButtonVisibility()

        } else if (entry!!.isFile()) {
            if (HorizonUtils.isMediaFile(entry!!.getName())) {
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
                player!!.clearQueue()
                player!!.addToQueue(entry!!.getPath())
                player!!.startCurrentlyPointedMediaInQueue()
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

    fun onEntryClickedInPlayQueueMode(entry: File?) {

        // Jump to the tapped/clicked track
    }

    fun setMediaInQueueToHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "sMIQTH")
        //FileSystemNavigator currentFileSystemNavigator = null;

        val pos = holder.getAdapterPosition()

        val mainActivity = mContext as MainActivity
        val tabs = mainActivity.mediaPlayerTabs
        val mediaPlayerTab = tabs.get(mainActivity.currentlyPlayedQueueIndex)

        val queue = mediaPlayerTab.playbackQueue.mediaFilePathQueue
        if (pos < 0 || queue.size <= pos) {
            Log.w(TAG, "sMIQTH pos: $pos")
            return
        }
        val path = queue.get(pos)


        val f = File(path)
        if (f != null) {
            holder.fileName.setText(f.getName())
        } else {
            holder.fileName.setText("!f")
        }
        holder.secondaryRow.setText("In Queue")
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

        internal var fileTypeIcon: ImageView? = null

        internal var fileName: TextView

        internal var secondaryRow: TextView

        internal var parentLayout: ConstraintLayout

        init {

            fileName = itemView.findViewById(R.id.file_name)
            secondaryRow = itemView.findViewById(R.id.secondary_row)
            parentLayout = itemView.findViewById(R.id.parent_layout)

            // Icon for showing the file type
            fileTypeIcon = itemView.findViewById(R.id.file_type_icon)
            fileTypeIcon!!.setImageResource(R.drawable.ic_file)

            Log.v(TAG, "vh ctor fileName: $fileName")
        }
    }

    companion object {

        private val TAG = "RecyclerViewAdapter"
    }
}
