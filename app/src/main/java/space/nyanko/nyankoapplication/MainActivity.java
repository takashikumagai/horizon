package space.nyanko.nyankoapplication;

import android.Manifest;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.content.Intent;
//import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.util.ArrayList;

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

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
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

        View playingTrackName = findViewById(R.id.playing_track_name);
        playingTrackName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "oC");
                switchToPlaybackQueueView();
            }
        });
        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());

        // Some C/C++ functions access filesystem so request the user file r/w permissions
        requestAppPermissions();

        int numInitialTabs = 1;
        for(int i=0; i<numInitialTabs; i++) {
            mediaPlayerTabs.add( new MediaPlayerTab() );
        }

        initRecyclerView();

        setTabListeners();

        for(int i=0; i<numInitialTabs; i++) {
            mediaPlayerTabs.get(i).getPlaybackQueue().setRecyclerViewAdapter(recyclerViewAdapter);
        }
        currentPlayerIndex = 0;
        Playback.setCurrentPlayer(
                mediaPlayerTabs.get(currentPlayerIndex).getPlaybackQueue()
        );

        initPlayeQueueMediaControlButtons();

        // Start the main service of the app
        // This service survives even after this activity is destroyed, e.g. by user
        // swiping it from the task list
        Intent serviceIntent = new Intent(this,BackgroundAudioService.class);
        startService(serviceIntent);

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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"oD");

        // For now we stop the service so that we can test their behavior from the start
        // every time we close and restart the app
        //Intent serviceIntent = new Intent(this,BackgroundAudioService.class);
        //stopService(serviceIntent);
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
            Log.d(TAG,"R.id.close_tab");
            TabLayout tabLayout = findViewById(R.id.tabLayout);
            int pos = tabLayout.getSelectedTabPosition();
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

            if(currentPlayerIndex != tabLayout.getSelectedTabPosition()) {
                Log.w(TAG,"urrentPlayerIndex != tabLayout.getSelectedTabPosition()");
                currentPlayerIndex = tabLayout.getSelectedTabPosition();
            }

            if(currentPlayerIndex < mediaPlayerTabs.size()) {
                Playback.setCurrentPlayer(
                        mediaPlayerTabs.get(currentPlayerIndex).getPlaybackQueue());
            } else {
                Log.w(TAG,"mPTs.size<=cPI");
            }

            if(tabLayout.getTabCount() == 0) {
                Log.d(TAG,"All tabs removed");

                // Notify recycler view adapter because otherwise the file list will remain
                // as no new tab is selected and as such onTabSelected() is not called.
                recyclerViewAdapter.notifyDataSetChanged();
            }

            return true;
        } else if(id == R.id.new_tab) {
            Log.d(TAG,"R.id.new_tab");
            TabLayout tabLayout = findViewById(R.id.tabLayout);
            tabLayout.addTab(tabLayout.newTab());
            TabLayout.Tab newTab = tabLayout.getTabAt(tabLayout.getTabCount()-1);
            if(newTab == null) {
                Log.d(TAG,"!newTab");
                return true;
            }
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

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initRecyclerView() {
        Log.d(TAG,"initRecyclerView called");

        //mFileNames.add(stringFromJNI());
        //String entries = listDir();
        //Log.d(TAG,entries);
        //String[] names = entries.split("\\n");
        //for(String entry : names) {
        //    mFileNames.add(entry);
        //}
        //mFileNames.add("123");
        //mFileNames.add("456");
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerViewAdapter = new RecyclerViewAdapter(this);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerViewAdapter.setCurrentFileSystemNavigator(
                mediaPlayerTabs.get(0).getFileSystemNavigator()
        );
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

    //public boolean areSamePaths(String path1, String path2) {
    //    return new File(path1).equals(new File(path2));
    //}


    public ArrayList<MediaPlayerTab> getMediaPlayerTabs() {
        return mediaPlayerTabs;
    }

    public void onBackPressed() {

        if(recyclerViewAdapter == null) {
            Log.w(TAG,"rVA");
            return;
        }

        int viewMode = recyclerViewAdapter.getViewMode();
        if(viewMode == 0) {
            FileSystemNavigator navigator = getCurrentFileSystemNavigator();
            int ret = navigator.moveToParent();

            // Show/hide FAB depending on whether the directory contains
            // one or more media files.
            updateFloatingActionButtonVisibility();

            recyclerViewAdapter.notifyDataSetChanged();

            setSelectedTabLabel(navigator.getCurrentDirectoryName());

            if (ret != 0) {
                super.onBackPressed();
            }
        } else if(viewMode == 1) {
            switchToFileSystemView();
        } else {
            Log.e(TAG,"oBP !!vM");
        }
//        if( areSamePaths(DirectoryNavigation.getNavigator().getCurrentDirectory(), "/storage/emulated/0") ) {
//            super.onBackPressed();
//        } else {
//            DirectoryNavigation.moveToParentDirectory();
//            if(recyclerViewAdapter != null) {
//                recyclerViewAdapter.notifyDataSetChanged();
//            }
//        }
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

    public void onMediaStartRequestedOnScreen() {
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
        }

//        View ptc = findViewById(R.id.playing_track_control);
//        ptc.setVisibility(View.VISIBLE);
    }

    public void switchToPlaybackQueueView() {

        // Hide the playing track control and show the play queue
        // media control.
        hidePlayingTrackControl();
        showPlaybackQueueControl();


        recyclerViewAdapter.setViewMode(1);

        recyclerViewAdapter.notifyDataSetChanged();

//        MediaPlayerTab mediaPlayerTab = getCurrentMediaPlayerTab();
//        if(mediaPlayerTab != null) {
//            mediaPlayerTab.setMode(1);
//        } else {
//            Log.d(TAG,"!mPT");
//        }
    }

    public void switchToFileSystemView() {

        if(currentlyPlayedQueueIndex < 0 || mediaPlayerTabs.size() <= currentlyPlayedQueueIndex) {
            Log.d(TAG,"sTFSV !!cPQI");
            return;
        }

        Playback playbackQueue
                = mediaPlayerTabs.get(currentlyPlayedQueueIndex).getPlaybackQueue();

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
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
                    Log.w(TAG, "fSN.sz<=cPI");
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

                if (mediaPlayerTabs.size() <= currentPlayerIndex) {
                    Log.w(TAG, "mPTs.sz<=cPI");
                    return;
                }

                onMediaStartRequestedOnScreen();
                Playback player = mediaPlayerTabs.get(currentPlayerIndex).getPlaybackQueue();
                player.clearQueue();
                player.addToQueue(mediaFiles);
                player.startCurrentlyPointedMediaInQueue();
                onMediaStartedOnScreen();

                switchToPlaybackQueueView();

                // Update background colors of queued tracks
                recyclerViewAdapter.notifyDataSetChanged();

                //if( mediaPlayer != null && mediaPlayer.isPlaying() ) {
                //    mediaPlayer.stop();
                //}
            }
        });
        fab.setVisibility(View.GONE);
    }

    public void initPlayeQueueMediaControlButtons() {
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

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    //public native String listDir();
    //public native String moveToParentDirectory();
    //public native String getCurrentDirectoryEntries();
}
