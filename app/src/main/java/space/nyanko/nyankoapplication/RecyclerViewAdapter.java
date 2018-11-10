package space.nyanko.nyankoapplication;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter";

    private Context mContext;

    private DirectoryNavigator directoryNavigator;

    public void refreshDirectoryContentList(String directoryPath) {
        directoryNavigator.setCurrentDirectory(directoryPath);
        notifyDataSetChanged();
    }

    public RecyclerViewAdapter(Context context, DirectoryNavigator directoryNavigator) {
        mContext = context;

        this.directoryNavigator = directoryNavigator;
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

        final int pos = holder.getAdapterPosition();

        ArrayList<File> entries = directoryNavigator.getCurrentDirectoryEntries();
        final File entry = entries.get(pos);
        holder.fileName.setText(entry.getName());

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked");

                Toast.makeText(mContext, entry.getName(), Toast.LENGTH_SHORT).show();

                if( entry.isDirectory() ) {
                    refreshDirectoryContentList(entry.getPath());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return directoryNavigator.getCurrentDirectoryEntries().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView fileName;
        RelativeLayout parentLayout;

        public ViewHolder(View itemView) {
            super(itemView);

            fileName = itemView.findViewById(R.id.file_name);
            parentLayout = itemView.findViewById(R.id.parent_layout);
        }
    }
}
