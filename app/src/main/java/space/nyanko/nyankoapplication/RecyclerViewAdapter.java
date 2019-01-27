package space.nyanko.nyankoapplication;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaMetadataRetriever;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
//import android.widget.RelativeLayout;
import android.support.constraint.ConstraintLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter";

    /**
     * @brief Ref to MainActivity
     */
    private Context mContext;

    private FileSystemNavigator currentFileSystemNavigator;

    //private DirectoryNavigator directoryNavigator;

    /**
     * @breif 0: filesystem, 1: play queue
     *
     */
    private int viewMode = 0;

    public RecyclerViewAdapter(Context context) {//, DirectoryNavigator directoryNavigator) {
        mContext = context;

        //this.directoryNavigator = directoryNavigator;
    }

    public void refreshDirectoryContentList(String directoryPath) {
        //directoryNavigator.setCurrentDirectory(directoryPath);
        //DirectoryNavigation.changeDirectory(directoryPath);
        notifyDataSetChanged();
    }

    public void setCurrentFileSystemNavigator(FileSystemNavigator currentFileSystemNavigator) {
        this.currentFileSystemNavigator = currentFileSystemNavigator;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG,"onCVH");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_listitem, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called.");

        if(viewMode == 0) {
            setFileOrDirectoryToHolder(holder, position);
        } else if (viewMode == 1) {
            setMediaInQueueToHolder(holder, position);
        } else {
            Log.e(TAG, "!!vM");
        }
    }

    public void setFileOrDirectoryToHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG,"sFODTH");
        //FileSystemNavigator currentFileSystemNavigator = null;

        final int pos = holder.getAdapterPosition();

        if(currentFileSystemNavigator == null) {
            Log.d(TAG,"!cFSN");
            return;
        }

        ArrayList<File> entries = currentFileSystemNavigator.getCurrentDirectoryEntries();
        final File entry = entries.get(pos);
        if(entry == null) {
            Log.d(TAG,"onBVH: !entry");
            return;
        }

        final MainActivity mainActivity = (MainActivity)mContext;
        int st = mainActivity.getMediaFileStatus(entry.getPath());
        int color = 0xffff00ff;
        if(st == 0) {
            color = 0xfff6f6f6;
        } else if(st == 1) { // playing
            color = 0xffaecc90;
        } else if(st == 2) { // queued
            color = 0xffb4d296;
        }
        holder.itemView.setBackgroundColor(color);

        // Set the icon based on the file type
        if( entry.isDirectory() ) {

            // Set the name of the directory
            Log.d(TAG,"setting text: " + entry.getName());
            holder.fileName.setText(entry.getName());

            if(holder.fileTypeIcon != null) {
                Log.d(TAG,"setting the round icon.");
                holder.fileTypeIcon.setImageResource(R.drawable.ic_file);
            }
        } else {
            // We are dealing with a file.

            // See if this one is a media file, e.g. mp3
            boolean isMediaFile = HorizonUtils.isMediaFile(entry.getName());
            if(isMediaFile) {
                if(HorizonOptions.showMetaTagTitles) {
                    int tags[] = {
                            MediaMetadataRetriever.METADATA_KEY_TITLE,
                            MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER,
                            MediaMetadataRetriever.METADATA_KEY_DURATION
                    };
//                    String title = HorizonUtils.getMediaFileTitle(entry);
                    HashMap<Integer,String> metaTags
                            = HorizonUtils.getMediaFileMetaTags(entry,tags);
                    String title = metaTags.get(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    if(title != null && 0 < title.length()) {
                        // The media file has a meta tag; use the title instead of its file name
                        holder.fileName.setText("[T] " + title);
                    } else {
                        // Does not have the title tag; just set the file name
                        holder.fileName.setText(entry.getName());
                    }

                    String secondRow = "";
                    String track = metaTags.get(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
                    if(track != null) {
                        secondRow = String.format("(%s)",track);
                    } else {
                        secondRow = "(-)";
                    }

                    String duration = metaTags.get(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if(duration != null) {
                        String hhmmss = HorizonUtils.millisecondsToHhmmss(Long.parseLong(duration));
                        secondRow += ", " + hhmmss;
                    }
                    holder.secondaryRow.setText(secondRow);

                } else {
                    // Option is set not to get meta tag; just set the file name
                    holder.fileName.setText(entry.getName());
                }
            } else {
                // Do nothing; don't display it unless it's a media file.
            }
            if(holder.fileTypeIcon != null) {
                Log.d(TAG,"setting the square icon.");
                holder.fileTypeIcon.setImageResource(R.drawable.ic_file);
            }
        }

        final FileSystemNavigator navigator = currentFileSystemNavigator;

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked");

                Toast.makeText(mContext, entry.getName(), Toast.LENGTH_SHORT).show();

                // Update the tab label
                mainActivity.setSelectedTabLabel(entry.getName());

                if(viewMode == 0) {
                    onEntryClickedInFileSystemViewMode(entry, pos, navigator, mainActivity);
                } else if(viewMode == 1) {
                    onEntryClickedInPlayQueueMode(entry);
                }
            }
        });
    }

    public void onEntryClickedInFileSystemViewMode(File entry,
                                                   int pos,
                                                   FileSystemNavigator navigator,
                                                   MainActivity mainActivity) {
        if( entry.isDirectory() ) {
            navigator.moveToChild(pos);
            refreshDirectoryContentList(entry.getPath());

            mainActivity.updateFloatingActionButtonVisibility();

        } else if( entry.isFile() ) {
            if( HorizonUtils.isMediaFile(entry.getName()) ) {
                // A playable media file, e.g. an mp3 fle, was tapped/clicked
                Log.d(TAG, "is media file");
                Playback player = mainActivity.getPlayerOfSelectedTab();
                if(player == null) {
                    Log.d(TAG, "!player");
                    return;
                }
                mainActivity.onMediaStartRequestedOnScreen();
                player.clearQueue();
                player.addToQueue(entry.getPath());
                player.startCurrentlyPointedMediaInQueue();
                mainActivity.onMediaStartedOnScreen();

                mainActivity.showPlayingTrackControl();

                notifyDataSetChanged();

                //resetBackgroundColors(view);

                // Change the BG color of the playing track.
                //view.setBackgroundColor(0xffb4d296);

                //Button btn = (Button)findViewById(R.id.play_pause);
                //btn.setText("||");
                //Log.d(TAG, "started playing");
            }
        }
    }

    public void onEntryClickedInPlayQueueMode(File entry) {

        // Jump to the tapped/clicked track
    }

    public void setMediaInQueueToHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG,"sMIQTH");
        //FileSystemNavigator currentFileSystemNavigator = null;

        final int pos = holder.getAdapterPosition();

        final MainActivity mainActivity = (MainActivity)mContext;
        ArrayList<MediaPlayerTab> tabs = mainActivity.getMediaPlayerTabs();
        MediaPlayerTab mediaPlayerTab = tabs.get(mainActivity.getCurrentlyPlayedQueueIndex());

        ArrayList<String> queue = mediaPlayerTab.getPlaybackQueue().getMediaFilePathQueue();
        if(pos < 0 || queue.size() <= pos) {
            Log.w(TAG,"sMIQTH pos: " + pos);
            return;
        }
        String path = queue.get(pos);


        File f = new File(path);
        if(f != null) {
            holder.fileName.setText(f.getName());
        } else {
            holder.fileName.setText("!f");
        }
        holder.secondaryRow.setText("In Queue");
    }

    @Override
    public int getItemCount() {

        if(viewMode == 0) {
            if(currentFileSystemNavigator != null) {
                return currentFileSystemNavigator.getNumCurrentDirectoryEntries();
            } else {
                Log.d(TAG,"gIC: !cFSN.");
                return 0;
            }
        } else if(viewMode == 1){
            final MainActivity mainActivity = (MainActivity)mContext;
            ArrayList<MediaPlayerTab> tabs = mainActivity.getMediaPlayerTabs();
            MediaPlayerTab tab = tabs.get(mainActivity.getCurrentlyPlayedQueueIndex());
            return tab.getPlaybackQueue().getMediaFilePathQueue().size();
        } else {
            Log.e(TAG,"gIC !!vM");
            return 0;
        }
    }

    int getViewMode() { return viewMode; }

    void setViewMode(int mode) {
        viewMode = mode;
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

    public class ViewHolder extends RecyclerView.ViewHolder{

        ImageView fileTypeIcon;

        TextView fileName;

        TextView secondaryRow;

        ConstraintLayout parentLayout;

        public ViewHolder(View itemView) {
            super(itemView);

            fileName = itemView.findViewById(R.id.file_name);
            secondaryRow = itemView.findViewById(R.id.secondary_row);
            parentLayout = itemView.findViewById(R.id.parent_layout);

            // Icon for showing the file type
            fileTypeIcon = itemView.findViewById(R.id.file_type_icon);
            fileTypeIcon.setImageResource(R.drawable.ic_file);

            Log.v(TAG, "vh ctor fileName: " + fileName );
        }
    }
}
