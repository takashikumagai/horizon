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

    private MediaPlayer mediaPlayer = new MediaPlayer();

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                if( mediaPlayer != null && mediaPlayer.isPlaying() ) {
                    mediaPlayer.stop();
                }
            }
        });

        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());

        // Some C/C++ functions access filesystem so request the user file r/w permissions
        requestAppPermissions();

        for(int i=0; i<5; i++) {
            playbacks.add( new Playback() );

            fileSystemNavigators.add( new FileSystemNavigator() );
            fileSystemNavigators.get(i).initialize();
        }
        currentFileSystemNavigatorIndex = 0;

        initRecyclerView();

        setTabListeners();
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

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    //public native String listDir();
    //public native String moveToParentDirectory();
    //public native String getCurrentDirectoryEntries();
}
