package space.nyanko.nyankoapplication;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter";

    private Context mContext;

    private FileSystemNavigator currentFileSystemNavigator;

    //private DirectoryNavigator directoryNavigator;

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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_listitem, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG,"onBindViewHolder: called.");

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
        holder.fileName.setText(entry.getName());

        // Set the icon based on the file type
        if( entry.isDirectory() ) {
            if(holder.fileTypeIcon != null) {
                Log.d(TAG,"setting the round icon.");
                holder.fileTypeIcon.setImageResource(R.drawable.ic_file);
            }
        } else {
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
                MainActivity mainActivity = (MainActivity)mContext;
                mainActivity.setSelectedTabLabel(entry.getName());

                if( entry.isDirectory() ) {
                    navigator.moveToChild(pos);
                    refreshDirectoryContentList(entry.getPath());
                } else if( entry.isFile() ) {
                    if( isMediaFile(entry.getName()) ) {

                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {

        if(currentFileSystemNavigator != null) {
            return currentFileSystemNavigator.getNumCurrentDirectoryEntries();
        } else {
            Log.d(TAG,"gIC: !cFSN.");
            return 0;
        }
//        return DirectoryNavigation.getNavigator().getCurrentDirectoryEntries().size();
    }

    private String getExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i+1);
        } else {
            return "";
        }
    }

    private boolean isMediaFile(String fileName) {
        String ext = getExtension(fileName);
        if(ext.equals("mp3")) {
            return true;
        } else {
            return false;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView fileName;
        ImageView fileTypeIcon;
        RelativeLayout parentLayout;

        public ViewHolder(View itemView) {
            super(itemView);

            fileName = itemView.findViewById(R.id.file_name);
            parentLayout = itemView.findViewById(R.id.parent_layout);

            // Icon for showing the file type
            fileTypeIcon = itemView.findViewById(R.id.file_type_icon);
            fileTypeIcon.setImageResource(R.drawable.ic_file);

            Log.v(TAG, "vh ctor fileName: " + fileName );
        }
    }
}
