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
import android.support.constraint.ConstraintLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.Serializable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MediaPlayerTab implements Serializable {
    private static final String TAG = "MediaPlayerTab";

    private FileSystemNavigator fileSystemNavigator;

    /**
     * @breif 0: filesystem, 1: play queue
     *
     */
    private int viewMode = 0;

    private Playback playbackQueue;

    MediaPlayerTab() {
        fileSystemNavigator = new FileSystemNavigator();
        fileSystemNavigator.initialize();

        playbackQueue = new Playback();
    }

    public FileSystemNavigator getFileSystemNavigator() {
        return fileSystemNavigator;
    }

    public Playback getPlaybackQueue() {
        return playbackQueue;
    }
}