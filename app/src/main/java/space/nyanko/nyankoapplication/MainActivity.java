package space.nyanko.nyankoapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
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

    //private MediaPlayer mediaPlayer = new MediaPlayer();

    private ArrayList<Playback> playbacks = new ArrayList<Playback>();

    private int currentFileSystemNavigatorIndex = -1;

    //==================== Model ====================

    private ArrayList<FileSystemNavigator> fileSystemNavigators = new ArrayList<FileSystemNavigator>();

    //private String currentDirectory = "/storage/emulated/0";
    //private DirectoryNavigator directoryNavigator = new DirectoryNavigator();

    private RecyclerViewAdapter recyclerViewAdapter = null;

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                //if( mediaPlayer != null && mediaPlayer.isPlaying() ) {
                //    mediaPlayer.stop();
                //}
            }
        });
        fab.setVisibility(View.GONE);

        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());

        // Some C/C++ functions access filesystem so request the user file r/w permissions
        requestAppPermissions();

        for(int i=0; i<1; i++) {
            playbacks.add( new Playback() );

            fileSystemNavigators.add( new FileSystemNavigator() );
            fileSystemNavigators.get(i).initialize();
        }
        currentFileSystemNavigatorIndex = 0;

        initRecyclerView();

        setTabListeners();

        Button btn = (Button)findViewById(R.id.play_pause_button);
        if(btn != null) {
            Log.d(TAG,"!play/pause btn");
        }

        btn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "btn pressed (play/pause)");
                Button self = (Button) view;//findViewById(R.id.play_pause);

                MediaPlayer mediaPlayer = Playback.getMediaPlayer();
                // Both isPlaying() and pause() can throw IllegalStateException.

                try {
                    if (mediaPlayer.isPlaying()) {
                        Log.d(TAG, "pausing");
                        mediaPlayer.pause();
                        self.setText("▶");
                    } else {
                        Log.d(TAG, "playing/resuming");
                        mediaPlayer.start(); // Start/resume playback
                        self.setText("||");
                    }
                } catch (IllegalStateException ise) {
                    Log.d(TAG,"ise caught");
                } catch (Exception e) {
                    Log.d(TAG,"Exception caught");
                }
            }
        });

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

            tabLayout.removeTab(tab);
            if(pos < fileSystemNavigators.size()) {
                fileSystemNavigators.remove(pos);
            } else {
                Log.w(TAG,"!!!fsn.size");
            }
            if(pos < playbacks.size()) {
                playbacks.remove(pos);
            } else {
                Log.w(TAG,"!!!playbacks.size");
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

            fileSystemNavigators.add( new FileSystemNavigator() );
            fileSystemNavigators.get(fileSystemNavigators.size() - 1).initialize();

            playbacks.add( new Playback() );

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

        recyclerViewAdapter.setCurrentFileSystemNavigator(fileSystemNavigators.get(0));
    }

    private FileSystemNavigator getCurrentFileSystemNavigator() {

        if(currentFileSystemNavigatorIndex < 0
                || fileSystemNavigators.size() <= currentFileSystemNavigatorIndex ) {
            Log.w(TAG,"invalid cFSNI");
            return null;
        }

        return fileSystemNavigators.get(currentFileSystemNavigatorIndex);
    }

    private void setTabListeners() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                Log.d(TAG,"onTabSelected called: " + tab.getText() + " (pos: " + pos + ")");

                if(pos < 0 || fileSystemNavigators.size() <= pos) {
                    Log.w(TAG,"oTS: invalid tab pos");
                    return;
                }

                currentFileSystemNavigatorIndex = pos;

                recyclerViewAdapter.setCurrentFileSystemNavigator(fileSystemNavigators.get(pos));
                recyclerViewAdapter.notifyDataSetChanged();

//                switchTab(pos);
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

    public void onBackPressed() {

        FileSystemNavigator navigator = getCurrentFileSystemNavigator();
        int ret = navigator.moveToParent();
        
        if(recyclerViewAdapter != null) {
            recyclerViewAdapter.notifyDataSetChanged();
        }

        setSelectedTabLabel(navigator.getCurrentDirectoryName());

        if(ret != 0) {
            super.onBackPressed();
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
        if(pos < 0 || playbacks.size() <= pos) {
            // No selected tab
            Log.d(TAG, "pos<0");
            return null;
        } else {
            return playbacks.get(pos);
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

    public ArrayList<Playback> getPlaybacks() {
        return playbacks;
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
