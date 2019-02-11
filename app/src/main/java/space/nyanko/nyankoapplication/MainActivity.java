package space.nyanko.nyankoapplication;

import android.Manifest;
import android.app.PendingIntent;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaPlayer;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.io.File;
import java.util.ArrayList;

@SuppressLint("RestrictedApi")
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    /**
     * @brief Index to the tab where a track is playing
     *
     * - Note that this can be any tab. Example: the user starts playing an album
     *   on one tab and then switches to another for browsing other media files.
     *   In this case, this variable points to the first tab.
     *
     */
    private int currentlyPlayedQueueIndex = -1;

    /**
     * @brief Index to the currently selected tab, i.e. tab on the screen
     *
     */
    private int currentPlayerIndex = -1;

    //==================== Model ====================

    private ArrayList<MediaPlayerTab> mediaPlayerTabs = new ArrayList<MediaPlayerTab>();

    private RecyclerViewAdapter recyclerViewAdapter = null;

    //private ServiceConnection serviceConnection = null;

    private static int tabIdCounter = 1000;

    public class EmptyClass {
        EmptyClass() {
            Log.d(TAG,"ctor of EmptyClass");
        }
    }

    private static EmptyClass emptyClass;

    public MainActivity() {
        super();
        Log.d(TAG,"ctor");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"s.oC");
        super.onCreate(savedInstanceState);
        Log.d(TAG,"oC");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initFloatingActionButton();

        hidePlaybackQueueControl();
        hidePlayingTrackControl();

        TextView playingTrackName = (TextView)findViewById(R.id.playing_track_name);
        playingTrackName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "oC");
                switchToPlaybackQueueView();
            }
        });

        // Some C/C++ functions access filesystem so request the user file r/w permissions
        requestAppPermissions();

        Log.d(TAG,"num mptabs: " + mediaPlayerTabs.size());
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        Log.d(TAG,"num tablayout tabs: " + tabLayout.getTabCount());

        int numInitialTabs = 1;

        if(savedInstanceState == null) {
            for(int i=0; i<numInitialTabs; i++) {
                mediaPlayerTabs.add( new MediaPlayerTab() );
            }

            tabLayout.addTab(tabLayout.newTab().setText(
                    mediaPlayerTabs.get(0).getFileSystemNavigator().getCurrentDirectoryName()
            ));
        }

        initRecyclerView();

        if(savedInstanceState == null) {
            if(0 < mediaPlayerTabs.size()) {
                recyclerViewAdapter.setCurrentFileSystemNavigator(
                        mediaPlayerTabs.get(0).getFileSystemNavigator()
                );
            }
        }

        setTabListeners();

        if(savedInstanceState == null) {
            for(int i=0; i<numInitialTabs; i++) {
                Playback playbackTracker = mediaPlayerTabs.get(i).getPlaybackQueue();
                playbackTracker.setRecyclerViewAdapter(recyclerViewAdapter);
                playbackTracker.setPlayingTrackName(playingTrackName);
            }
            currentPlayerIndex = 0;
            Playback.setCurrentPlayer(
                    mediaPlayerTabs.get(currentPlayerIndex).getPlaybackQueue()
            );
        }

        initPlayQueueMediaControlButtons();

        // TODO: do this only once at startup
        if(savedInstanceState == null) {
            if(BackgroundAudioService.getInstance() == null) {
                // Start a new service, or restart it.
                startAudioService();
            } else {
                // Could this be possible?
                Log.d(TAG,"oC service != null");
            }
        }

//        serviceConnection = new ServiceConnection();
//        boolean res = bindService(serviceIntent,serviceConnection,0);
//        Log.d(TAG,"bS: " + res);
//        if(res) {
//            ;
//        }
        //serviceConnection

        View v = findViewById(R.id.new_tab);
        if(v == null) {
            Log.d(TAG,"!v");
        }

        final Button newTabButton = (Button)v;
        if(newTabButton == null) {
            Log.d(TAG,"!newTabButton");
        }

        //newTabButton.setOnClickListener(new View.OnClickListener() {
        //    public void onClick(View v) {
        //        Log.d(TAG,"newTabButton.onClick");
        //    }
        //});

        if(emptyClass == null) {
            emptyClass = new EmptyClass();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"oDestroy");

        // For now we stop the service so that we can test their behavior from the start
        // every time we close and restart the app
        //Intent serviceIntent = new Intent(this,BackgroundAudioService.class);
        //stopService(serviceIntent);


    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putSerializable("mediaPlayerTabs",mediaPlayerTabs);
        savedInstanceState.putInt("currentPlayerIndex",currentPlayerIndex);
        savedInstanceState.putInt("currentlyPlayedQueueIndex",currentlyPlayedQueueIndex);

        // Save the view hierarchy
        super.onSaveInstanceState(savedInstanceState);

        Log.d(TAG,"oSIS");
    }

    /**
     * This is called AFTER onCreate() to restore things
     *
     *
     * @param savedInstanceState
     */
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        mediaPlayerTabs = (ArrayList<MediaPlayerTab>)savedInstanceState.getSerializable("mediaPlayerTabs");
        currentPlayerIndex = savedInstanceState.getInt("currentPlayerIndex");
        currentlyPlayedQueueIndex = savedInstanceState.getInt("currentlyPlayedQueueIndex");

        Log.d(TAG,"oRIS");

        if(recyclerViewAdapter == null) {
            Log.e(TAG,"!!!!!!!!!!!!!!!!!!!!!! recyclerViewAdapter !!!!!!!!!!!!!!!!!!!!!!");
        }

        TextView playingTrackName = (TextView)findViewById(R.id.playing_track_name);

        Log.d(TAG,"oRIS mPTs.sz: " + mediaPlayerTabs.size());
        for(MediaPlayerTab mptab: mediaPlayerTabs) {

            // recyclerViewAdapter has already been re-created in onCreate()
            mptab.getPlaybackQueue().setRecyclerViewAdapter(recyclerViewAdapter);
            mptab.getPlaybackQueue().setPlayingTrackName(playingTrackName);

            //mptab.getName();
            TabLayout tabLayout = findViewById(R.id.tabLayout);
            tabLayout.addTab(tabLayout.newTab().setText(
                    mptab.getFileSystemNavigator().getCurrentDirectoryName()
            ));
        }

        if(0 <= currentPlayerIndex && currentPlayerIndex < mediaPlayerTabs.size()) {
            // Set the FS navigator of the currently selected tab
            recyclerViewAdapter.setCurrentFileSystemNavigator(
                    mediaPlayerTabs.get(currentPlayerIndex).getFileSystemNavigator()
            );

            // Restore the vie mode
            int viewMode = mediaPlayerTabs.get(currentPlayerIndex).getViewMode();
            recyclerViewAdapter.setViewMode(viewMode);

            // Show the controls suited for each view mode
            if(viewMode == 0) {
                switchToFileSystemView();
            } else if(viewMode == 1){
                switchToPlaybackQueueView();
            } else {
                Log.e(TAG,"!vM: " + viewMode);
            }
        }

        recyclerViewAdapter.notifyDataSetChanged();

        updateFloatingActionButtonVisibility();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"oP");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"oR");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Log.d(TAG,"R.id.action_settings");
            return true;
        } else if(id == R.id.close_tab) {

            return closeTab();

        } else if(id == R.id.new_tab) {
            Log.d(TAG,"R.id.new_tab");
            TabLayout tabLayout = findViewById(R.id.tabLayout);
            tabLayout.addTab(tabLayout.newTab());
            TabLayout.Tab newTab = tabLayout.getTabAt(tabLayout.getTabCount()-1);
            if(newTab == null) {
                Log.d(TAG,"!newTab");
                return true;
            }
            //newTab.setId(tabIdCounter);
            tabIdCounter += 1;
            newTab.setText("newtab");
//            View child = tabLayout.getChildAt(tabLayout.getTabCount()-1);
//            if(child == null) {
//                Log.d(TAG,"!child");
//                return true;
//            }
//            child.setOnLongClickListener( new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    Log.d(TAG,"onLC");
//                    return true;
//                }
//            });

            // Add an FS navigator and a player

            mediaPlayerTabs.add( new MediaPlayerTab() );
            mediaPlayerTabs.get(mediaPlayerTabs.size()-1).getPlaybackQueue()
                    .setRecyclerViewAdapter(recyclerViewAdapter);

            TextView playingTrackName = (TextView)findViewById(R.id.playing_track_name);
            mediaPlayerTabs.get(mediaPlayerTabs.size()-1).getPlaybackQueue()
                    .setPlayingTrackName(playingTrackName);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startAudioService() {

        // Start the main service of the app
        // This service survives even after this activity is destroyed, e.g. by user
        // swiping it from the task list
        Log.d(TAG,"sAS starting service");
        Intent serviceIntent = new Intent(this,BackgroundAudioService.class);

        // Note that there is always only one instance of the service;
        // multiple calls of startService does not result in multiple instance
        // of the service.
        startService(serviceIntent);
    }

    private boolean closeTab() {
        Log.d(TAG,"R.id.close_tab");
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        int pos = tabLayout.getSelectedTabPosition();
        Log.d(TAG,"tabpos: " + pos);
        if(pos < 0) {
            Log.d(TAG,"R.id.ct pos<0");
            return false;
        }
        TabLayout.Tab tab = tabLayout.getTabAt(pos);
        if(tab==null) {
            Log.d(TAG,"R.id.ct !tab");
            return false;
        }

        if(pos < mediaPlayerTabs.size()) {
            mediaPlayerTabs.remove(pos);
        } else {
            Log.w(TAG,"!!!fsn.size");
        }

        Log.d(TAG,"removing tab");
        tabLayout.removeTab(tab);
        // If there are any tab(s) left, onTabSelected has already been invoked
        Log.d(TAG,"tab removed");

        if(tabLayout.getTabCount() == 0) {
            Log.d(TAG,"All tabs removed");
            currentPlayerIndex = -1;
            Playback.setCurrentPlayer(null);

            recyclerViewAdapter.setCurrentFileSystemNavigator(null);
            // Notify recycler view adapter because otherwise the file list will remain
            // as no new tab is selected and as such onTabSelected() is not called.
            recyclerViewAdapter.notifyDataSetChanged();

            return true;
        }

        int newSelectedTabPosition = tabLayout.getSelectedTabPosition();

        if(newSelectedTabPosition < 0) {
            Log.d(TAG,"nSTP<0");
            // There are one or more tabs left but none is selected
        }
        else if(currentPlayerIndex != newSelectedTabPosition) {
            Log.w(TAG,"urrentPlayerIndex != tabLayout.getSelectedTabPosition()");
            currentPlayerIndex = newSelectedTabPosition;
        }

        if(0 <= currentPlayerIndex && currentPlayerIndex < mediaPlayerTabs.size()) {
            Playback.setCurrentPlayer(
                    mediaPlayerTabs.get(currentPlayerIndex).getPlaybackQueue());
        } else {
            Log.w(TAG,"mPTs.size<=cPI");
        }

        return true;
    }

    private void initRecyclerView() {
        Log.d(TAG,"initRecyclerView called");

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerViewAdapter = new RecyclerViewAdapter(this);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private FileSystemNavigator getCurrentFileSystemNavigator() {

        if(currentPlayerIndex < 0
                || mediaPlayerTabs.size() <= currentPlayerIndex ) {
            Log.w(TAG,"invalid cFSNI");
            return null;
        }

        return mediaPlayerTabs.get(currentPlayerIndex).getFileSystemNavigator();
    }

    private void setTabListeners() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                Log.d(TAG,"onTabSelected called: " + tab.getText() + " (pos: " + pos + ")");

                if(pos < 0 || mediaPlayerTabs.size() <= pos) {
                    Log.w(TAG,"oTS: invalid tab pos");
                    return;
                }

                currentPlayerIndex = pos;

                recyclerViewAdapter.setCurrentFileSystemNavigator(
                        mediaPlayerTabs.get(pos).getFileSystemNavigator());
                recyclerViewAdapter.setViewMode(mediaPlayerTabs.get(pos).getViewMode());
                recyclerViewAdapter.notifyDataSetChanged();

                Playback.setCurrentPlayer(
                        mediaPlayerTabs.get(pos).getPlaybackQueue());

//              switchTab(pos);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                Log.d(TAG,"onTabUnselected called: " + tab.getText() + " (pos: " + pos + ")");

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                Log.d(TAG,"onTabReselected called: " + tab.getText() + " (pos: " + pos + ")");

            }
        });
    }

    public ArrayList<MediaPlayerTab> getMediaPlayerTabs() {
        return mediaPlayerTabs;
    }

    public void onBackPressed() {
        Log.d(TAG,"oBP");

        if(recyclerViewAdapter == null) {
            Log.w(TAG,"rVA");
            return;
        }

        int viewMode = recyclerViewAdapter.getViewMode();
        if(viewMode == 0) {
            FileSystemNavigator navigator = getCurrentFileSystemNavigator();
            int ret = 0;
            if(navigator != null) {
                // Try moving to the parent point
                // ret = 0: successfully moved to the parent point
                // ret = -1: was already at the root point
                ret = navigator.moveToParent();
            } else {
                // This happens when there are no tabs
                ret = -1;
            }

            if(ret == 0) {
                // Moved to the parent directory/point

                // Show/hide FAB depending on whether the directory contains
                // one or more media files.
                updateFloatingActionButtonVisibility();

                recyclerViewAdapter.notifyDataSetChanged();

                setSelectedTabLabel(navigator.getCurrentDirectoryName());
            } else {
                // We have been already at the root of the tree so
                // moving up to a parent point never happened
                // -> Hand over the control to the super class.
                Log.d(TAG,"s.oBP");
                super.onBackPressed();
            }
        } else if(viewMode == 1) {
            // User pressed the back button while the app is in the play queue view mode.
            // -> Go back to the file system view.
            switchToFileSystemView();
        } else {
            Log.e(TAG,"oBP !!vM");
        }
    }

    public void setSelectedTabLabel(String text) {

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        int pos = tabLayout.getSelectedTabPosition();
        if(pos == -1) {
            // No selected tab
            Log.d(TAG, "pos<0");
        } else {
            TabLayout.Tab tab = tabLayout.getTabAt(pos);
            if(tab != null) {
                tab.setText(text);
            }
        }
    }

    public Playback getPlayerOfSelectedTab() {

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        if(tabLayout == null) {
            return null;
        }
        int pos = tabLayout.getSelectedTabPosition();
        if(pos < 0 || mediaPlayerTabs.size() <= pos) {
            // No selected tab
            Log.d(TAG, "pos<0");
            return null;
        } else {
            return mediaPlayerTabs.get(pos).getPlaybackQueue();
        }
    }

    FloatingActionButton getFloatingActionButton() {
        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
        if(fab==null) { // Sanity check
            Log.e(TAG, "!fab");
        }
        return fab;
    }

    public void updateFloatingActionButtonVisibility() {
        int index = currentPlayerIndex;
        if(index < 0 || mediaPlayerTabs.size() <= index) {
            Log.d(TAG,"uFABV !i" + index);
            return;
        }

        AbstractDirectoryNavigator navigator
                = mediaPlayerTabs.get(index).getFileSystemNavigator().getCurrentNavigator();

        if(navigator == null) {
            Log.d(TAG,"uFABV !cn");
            return;
        }

        FloatingActionButton fab = getFloatingActionButton();
        if(navigator.isAtLeastOneMediaFilePresent()) {
            Log.d(TAG, "fab visible");
            fab.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "fab gone");
            fab.setVisibility(View.GONE);
        }
    }

    /**
     * @brief Returns the status of the given media file
     *
     *
     * @param filePath
     * @return 1: currently playing, 2: queued, 0: else
     */
    public int getMediaFileStatus(String filePath) {

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        int tabPos = tabLayout.getSelectedTabPosition();
        if(tabPos < 0 || mediaPlayerTabs.size() <= tabPos) {
            return 0;
        }

        Playback player = mediaPlayerTabs.get(tabPos).getPlaybackQueue();
        if(player.isPointed(filePath)) {
            return 1;
        } else if(player.isInQueue(filePath)){
            return 2;
        } else {
            return 0;
        }
    }

    private void requestAppPermissions() {
        Log.d(TAG,"rAPs");

        if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG,"SDK_INT < LOLLIPOP");
            return;
        }

        if(hasReadPermissions() && hasWritePermissions()) {
            Log.d(TAG,"Read/write permissions already granted");
            return;
        }


        int myRequestCode = 12;

        Log.d(TAG,"Requesting read/write permissions");
        ActivityCompat.requestPermissions(this,
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, myRequestCode); // your request code
    }

    private boolean hasReadPermissions() {
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean hasWritePermissions() {
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    //public MediaPlayer getMediaPlayer() {
    //    return mediaPlayer;
    //}

    public boolean onMediaStartRequestedOnScreen() {

        BackgroundAudioService service = BackgroundAudioService.getInstance();
        if(service == null) {
            Log.e(TAG,"oMSROS !service");
            startAudioService();
            service = BackgroundAudioService.getInstance();
            if(service == null) {
                Log.e(TAG,"oMSROS Failed to restart service");
                return false;
            }

            // Since we created a service, we have to set the reference to currently played queue.
            // Since this function is for play request made on screen, we assume that
            // currently selected tab == playing tab
            if( 0 <= currentPlayerIndex && currentPlayerIndex < mediaPlayerTabs.size() ) {

                service.setCurrentlyPlayedPlaybackQueue(
                        mediaPlayerTabs.get(currentPlayerIndex).getPlaybackQueue()
                );
            } else {
                Log.d(TAG,"oMSROS !cPQI");
            }
        }
        boolean granted = service.retrievedAudioFocus();
        if(!granted) {
            return false;
        }

        if(currentlyPlayedQueueIndex != currentPlayerIndex) {
            // currently played tab != selected tab.
            if(0 <= currentlyPlayedQueueIndex) {
                if(currentlyPlayedQueueIndex < mediaPlayerTabs.size()) {
                    mediaPlayerTabs.get(currentlyPlayedQueueIndex).getPlaybackQueue()
                            .saveCurrentPlaybackPosition();
                } else {
                    Log.w(TAG,"sz<=cPQI");
                }
            }
        }

        return true;
    }

    public void onMediaStartedOnScreen() {

        if(currentPlayerIndex < 0 || mediaPlayerTabs.size() <= currentPlayerIndex) {
            Log.w(TAG,"cPI: "+currentPlayerIndex);
            return;
        }

        currentlyPlayedQueueIndex = currentPlayerIndex;

        // Notify the service once we start playing tracks in a queue
        // - Or to be more exact, we give the service a reference to the instance
        //   storing the playback information (queue, currently played track, etc)
        // - Note that this particular instance of Playback survivies the activity destruction
        //   and recreation
        BackgroundAudioService service = BackgroundAudioService.getInstance();
        if(service == null) {
            Log.w(TAG,"!service");
        } else {
            service.setCurrentlyPlayedPlaybackQueue(
                    mediaPlayerTabs.get(currentlyPlayedQueueIndex).getPlaybackQueue()
            );

            service.updateMediaControls();
            service.showMediaControls();
        }

//        View ptc = findViewById(R.id.playing_track_control);
//        ptc.setVisibility(View.VISIBLE);
    }

    public void switchToPlaybackQueueView() {

        // Save the view mode
        mediaPlayerTabs.get(currentPlayerIndex).setViewMode(1);

        // Hide the playing track control and show the play queue
        // media control.
        hidePlayingTrackControl();
        showPlaybackQueueControl();

        recyclerViewAdapter.setViewMode(1);

        recyclerViewAdapter.notifyDataSetChanged();

        if(currentPlayerIndex < 0 || mediaPlayerTabs.size() <= currentPlayerIndex) {
            Log.d(TAG,"sTFSV !!cPQI");
        } else {
            // Save the view mode to the tab instance.
            // We'll use this info to save the view mode on per-tab basis,
            // and restore the mode for each tab every time the user swtiches tabs.
            mediaPlayerTabs.get(currentPlayerIndex).setViewMode(1);
        }
//        MediaPlayerTab mediaPlayerTab = getCurrentMediaPlayerTab();
//        if(mediaPlayerTab != null) {
//            mediaPlayerTab.setMode(1);
//        } else {
//            Log.d(TAG,"!mPT");
//        }
    }

    public void switchToFileSystemView() {

        if(currentPlayerIndex < 0 || mediaPlayerTabs.size() <= currentPlayerIndex) {
            Log.d(TAG,"sTFSV !!cPI");
            return;
        }

        // Save the view mode
        mediaPlayerTabs.get(currentPlayerIndex).setViewMode(0);

        Playback playbackQueue
                = mediaPlayerTabs.get(currentPlayerIndex).getPlaybackQueue();

        if(0 < playbackQueue.getMediaFilePathQueue().size()) {
            showPlayingTrackControl();
        }
        hidePlaybackQueueControl();

        recyclerViewAdapter.setViewMode(0);
        recyclerViewAdapter.notifyDataSetChanged();
    }

    public void showPlaybackQueueControl() {
        View pc = findViewById(R.id.playback_control);
        pc.setVisibility(View.VISIBLE);
    }
    public void hidePlaybackQueueControl() {
        View pc = findViewById(R.id.playback_control);
        pc.setVisibility(View.GONE);
    }


    public int getCurrentlyPlayedQueueIndex() {
        return currentlyPlayedQueueIndex;
    }

    public void initFloatingActionButton() {
        Log.d(TAG, "iFAB");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {

            /**
             * When FAB is clicked, the app
             * - Collects media files from the current directory
             * - Puts them in the queue
             * - Starts playing them
             * @param view
             */
            @Override
            public void onClick(View view) {
                Log.d(TAG, "fab.oC");
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                if (currentPlayerIndex < 0) {
                    Log.w(TAG, "fab.oC.cPI<0");
                    return;
                }

                if (mediaPlayerTabs.size() <= currentPlayerIndex) {
                    Log.w(TAG, "mPTs.sz<=cPI");
                    return;
                }

                ArrayList<File> filesAndDirs
                        = mediaPlayerTabs.get(currentPlayerIndex).getFileSystemNavigator()
                        .getCurrentDirectoryEntries();

                // Put all the media files in the current directory to the queue and start playing
                ArrayList<File> mediaFiles = HorizonUtils.pickMediaFiles(filesAndDirs);
                Log.d(TAG, "mFs.sz: " + mediaFiles.size());

                if (mediaFiles.size() == 0) {
                    Log.w(TAG, "0!mFs.sz");
                    return;
                }

                boolean readyToPlay = onMediaStartRequestedOnScreen();
                if(!readyToPlay) {
                    Log.d(TAG, "!rTP");
                    return;
                }
                Playback player = mediaPlayerTabs.get(currentPlayerIndex).getPlaybackQueue();
                player.clearQueue();
                player.addToQueue(mediaFiles);
                player.startCurrentlyPointedMediaInQueue();
                onMediaStartedOnScreen();

                switchToPlaybackQueueView();

                // Update background colors of queued tracks
                // This is done in switchToPlaybackQueueView() so commented out.
                //recyclerViewAdapter.notifyDataSetChanged();

                //if( mediaPlayer != null && mediaPlayer.isPlaying() ) {
                //    mediaPlayer.stop();
                //}
            }
        });

        fab.setImageBitmap(textAsBitmap("▶", 40, Color.WHITE));

        fab.setVisibility(View.GONE);
    }

    public void initPlayQueueMediaControlButtons() {
        Button btn = (Button)findViewById(R.id.play_pause_button);
        if(btn == null) {
            Log.d(TAG,"!play/pause btn");
            return;
        }

        btn.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "btn pressed (play/pause)");
                    Button self = (Button) view;//findViewById(R.id.play_pause);

                    MediaPlayer mediaPlayer = Playback.getMediaPlayer();
                    // Both isPlaying() and pause() can throw IllegalStateException.

                    try {
                        if (mediaPlayer.isPlaying()) {
                            Log.d(TAG, "btn pausing");
                            mediaPlayer.pause();
                            self.setText("▶");
                        } else {
                            Log.d(TAG, "btn playing/resuming");
                            mediaPlayer.start(); // Start/resume playback
                            self.setText("||");
                        }
                    } catch (IllegalStateException ise) {
                        Log.d(TAG, "ise caught");
                    } catch (Exception e) {
                        Log.d(TAG, "Exception caught");
                    }
                }
            });

        Button prevTrackBtn = (Button)findViewById(R.id.prev_track);
        if(prevTrackBtn == null) {
            Log.d(TAG,"!pTB");
            return;
        }

        prevTrackBtn.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "btn:p (prev)");
                    Button self = (Button) view;

                    if(currentPlayerIndex < 0 || mediaPlayerTabs.size() <= currentPlayerIndex) {
                        return;
                    }

                    onMediaStartRequestedOnScreen();
                    MediaPlayerTab tab = mediaPlayerTabs.get(currentPlayerIndex);
                    tab.getPlaybackQueue().playPrevTrack();
                    onMediaStartedOnScreen();
                }
            });

        Button nextTrackBtn = (Button)findViewById(R.id.next_track);
        if(nextTrackBtn == null) {
            Log.d(TAG,"!nTB");
            return;
        }

        nextTrackBtn.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "btn:p (next)");
                    Button self = (Button) view;

                    if(currentPlayerIndex < 0 || mediaPlayerTabs.size() <= currentPlayerIndex) {
                        return;
                    }

                    onMediaStartRequestedOnScreen();
                    MediaPlayerTab tab = mediaPlayerTabs.get(currentPlayerIndex);
                    tab.getPlaybackQueue().playNextTrack();
                    onMediaStartedOnScreen();
                }
            });
    }

    public void showPlayingTrackControl() {
        Log.d(TAG, "sPTB");

        // Show the playing track info and control (pause/resume)
        View ptc = findViewById(R.id.playing_track_control);
        ptc.setVisibility(View.VISIBLE);
    }
    public void hidePlayingTrackControl() {
        Log.d(TAG, "hPTC");

        // Show the playing track info and control (pause/resume)
        View ptc = findViewById(R.id.playing_track_control);
        ptc.setVisibility(View.GONE);
    }

    //method to convert your text to image
    public static Bitmap textAsBitmap(String text, float textSize, int textColor) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline = -paint.ascent(); // ascent() is negative
        int width = (int) (paint.measureText(text) + 0.0f); // round
        int height = (int) (baseline + paint.descent() + 0.0f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }
}
