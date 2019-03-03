package space.nyanko.nyankoapplication

import android.Manifest
import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import com.google.android.material.tabs.TabLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.SeekBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import android.view.View
import android.view.ViewGroup
import android.view.Menu
import android.view.MenuItem
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.util.ArrayList

@SuppressLint("RestrictedApi")
class MainActivity : AppCompatActivity() {

    /**
     * @brief Index to the tab where a track is playing
     *
     * - Note that this can be any tab. Example: the user starts playing an album
     * on one tab and then switches to another for browsing other media files.
     * In this case, this variable points to the first tab.
     */
    var currentlyPlayedQueueIndex = -1
        private set

    /**
     * @brief Index to the currently selected tab, i.e. tab on the screen
     */
    private var currentPlayerIndex = -1

    //==================== Model ====================

    var mediaPlayerTabs = ArrayList<MediaPlayerTab>()
        private set

    private var recyclerViewAdapter: RecyclerViewAdapter? = null

    var folderViewPlayingTrackTime: TextView? = null
    var folderViewSeekBar: SeekBar? = null
    private var isTrackingSeekBar: Boolean = false

    private var playingTrackName: TextView? = null


    private val handler = Handler()

    private val currentFileSystemNavigator: FileSystemNavigator?
        get() {

            if (currentPlayerIndex < 0 || mediaPlayerTabs.size <= currentPlayerIndex) {
                Log.w(TAG, "invalid cFSNI")
                return null
            }

            return mediaPlayerTabs.get(currentPlayerIndex).fileSystemNavigator
        }

    // No selected tab
    val playerOfSelectedTab: Playback?
        get() {

            val tabLayout: TabLayout = findViewById(R.id.tabLayout) ?: return null
            val pos = tabLayout!!.selectedTabPosition
            if (pos < 0 || mediaPlayerTabs.size <= pos) {
                Log.d(TAG, "pos<0")
                return null
            } else {
                return mediaPlayerTabs.get(pos).playbackQueue
            }
        }

    internal// Sanity check
    val floatingActionButton: FloatingActionButton?
        get() {
            val fab = findViewById(R.id.fab) as FloatingActionButton
            if (fab == null) {
                Log.e(TAG, "!fab")
            }
            return fab
        }

    private val mediaPlayer: MediaPlayer?
        get() {
            val service = BackgroundAudioService.instance
            if (service == null) {
                Log.d(TAG, "gMP !service")
                return null
            }

            return service!!.mediaPlayer
        }

    inner class MyFabBehavior : FloatingActionButton.Behavior() {

        override fun onDependentViewChanged(parent: CoordinatorLayout,
                                   child: FloatingActionButton,
                                   dependency: View): Boolean {
            Log.d(TAG, "fab_behavior oDVC")
            return super.onDependentViewChanged(parent, child, dependency)
        }

        override fun onLayoutChild(parent: CoordinatorLayout,
                          child: FloatingActionButton,
                          layoutDirection: Int): Boolean {
            Log.d(TAG, "fab_behavior oLC")
            return super.onLayoutChild(parent, child, layoutDirection)
        }

    }

    init {
        Log.d(TAG, "ctor")
    }

    override protected fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "s.oC")
        super.onCreate(savedInstanceState)
        Log.d(TAG, "oC")
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        initFloatingActionButton()

        hidePlaybackQueueControl()
        hidePlayingTrackControl()

        playingTrackName = findViewById(R.id.playing_track_name) as TextView
        playingTrackName!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                Log.d(TAG, "oC")
                switchToPlaybackQueueView()
            }
        })

        // Some C/C++ functions access filesystem so request the user file r/w permissions
        requestAppPermissions()

        Log.d(TAG, "num mptabs: " + mediaPlayerTabs.size)
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        Log.d(TAG, "num tablayout tabs: " + tabLayout.getTabCount())

        initRecyclerView()

        if (savedInstanceState == null) {
            Log.d(TAG, "!sIS")

            // saved state == null: there are a couple of possibilities,
            // but the states will not be restored via bundle instance and
            // we are on our own to restore the app state.
            val f = File(getFilesDir(), APPLICATION_STATE_FILE_NAME)
            if (f.exists()) {
                Log.d(TAG, "oC apf")
                // De-serialize the app state from file
                restoreStateFromFile()
            } else {
                Log.d(TAG, "oC !apf " + mediaPlayerTabs.size) // a sanity check; should be 0
                // There is no saved state file either;
                // Add an initial tab.
                val numInitialTabs = 1

                // We do this when the user launches the app for the very first time,
                // or the appstate file was deleted for some reason, e.g. user deleting
                // it on purpose.
                mediaPlayerTabs.clear()
                for (i in 0 until numInitialTabs) {
                    mediaPlayerTabs.add(MediaPlayerTab())
                }

                currentPlayerIndex = 0
            }
            Log.d(TAG, "restored: $currentPlayerIndex")
            Log.d(TAG, "mptabs: " + mediaPlayerTabs.size)

            // Assuming all tab are now restored, we set the recycler view
            // references to playback instances.
            for (mptab in mediaPlayerTabs) {
                val playbackTracker = mptab.playbackQueue
                playbackTracker.setRecyclerViewAdapter(recyclerViewAdapter)
                playbackTracker.setPlayingTrackName(playingTrackName)
            }

            // Add tablayout tabs
            for (mptab in mediaPlayerTabs) {
                val newTab = tabLayout.newTab()
                newTab.setText(mptab.fileSystemNavigator.currentDirectoryName)
                Log.d(TAG, "adding tab. index: $currentPlayerIndex")
                tabLayout.addTab(newTab)
            }

            if (0 <= currentPlayerIndex && currentPlayerIndex < mediaPlayerTabs.size) {
                Playback.setCurrentPlayer(
                        mediaPlayerTabs.get(currentPlayerIndex).playbackQueue
                )
                recyclerViewAdapter!!.setCurrentFileSystemNavigator(
                        mediaPlayerTabs.get(currentPlayerIndex).fileSystemNavigator
                )

                Log.d(TAG, "Selecting a tab: $currentPlayerIndex")
                // Select the tab that had been selected before the app was destroyed
                val tab = tabLayout.getTabAt(currentPlayerIndex)
                tab?.select()
            } else {
                Log.w(TAG, "oC !cPI: $currentPlayerIndex")
            }
        }

        // Call this after tabLayout.addTab() in the block above in order to avoid
        // the invocation of onTabSelected() as the first tab added via tabLayout.addTab()
        // becomes the selected tab and it invokes tab listener's onTabSelected callback.
        setTabListeners()

        initPlayQueueMediaControlButtons()

        // TODO: do this only once at startup
        if (savedInstanceState == null) {
            if (BackgroundAudioService.instance == null) {
                // Start a new service, or restart it.
                startAudioService()
            } else {
                // Could this be possible?
                Log.d(TAG, "oC service != null")
            }
        }

        //        serviceConnection = new ServiceConnection();
        //        boolean res = bindService(serviceIntent,serviceConnection,0);
        //        Log.d(TAG,"bS: " + res);
        //        if(res) {
        //            ;
        //        }
        //serviceConnection

        folderViewPlayingTrackTime = findViewById(R.id.playing_track_time) as TextView

        folderViewSeekBar = findViewById(R.id.playing_track_seek_bar) as SeekBar
        folderViewSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.d(TAG, "sb.oPC: " + seekBar.getProgress())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                Log.d(TAG, "sb.onStartTT: " + seekBar.getProgress())
                isTrackingSeekBar = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                Log.d(TAG, "sb.onStopTT: " + seekBar.getProgress())
                val service = BackgroundAudioService.instance
                if (service != null) {
                    val mediaPlayer = service!!.mediaPlayer
                    if (mediaPlayer != null) {
                        mediaPlayer!!.seekTo(seekBar.getProgress())
                    }
                }

                isTrackingSeekBar = false
            }
        })

        runOnUiThread(object : Runnable {

            override fun run() {
                val mediaPlayer = mediaPlayer
                if (mediaPlayer == null) {
                    Log.d(TAG, "rout !mP")
                    return
                }

                updateSeekbarProgressAndTime(mediaPlayer)

                // Add this runnable to the message queue
                handler.postDelayed(this, 3000)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "oDestroy")

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val stateSaved = sharedPref.getInt("state_saved_to_bundle", 0)
        if (stateSaved == 0) {
            saveStateToFile()
        }

        // Reset the flag
        setStateSavedToBundle(0)

        // For now we stop the service so that we can test their behavior from the start
        // every time we close and restart the app
        //Intent serviceIntent = new Intent(this,BackgroundAudioService.class);
        //stopService(serviceIntent);


    }

    override protected fun onSaveInstanceState(savedInstanceState: Bundle) {

        savedInstanceState.putSerializable("mediaPlayerTabs", mediaPlayerTabs)
        savedInstanceState.putInt("currentPlayerIndex", currentPlayerIndex)
        savedInstanceState.putInt("currentlyPlayedQueueIndex", currentlyPlayedQueueIndex)

        // Save the view hierarchy
        super.onSaveInstanceState(savedInstanceState)

        setStateSavedToBundle(1)

        Log.d(TAG, "oSIS")
    }

    /**
     * This is called AFTER onCreate() to restore things
     *
     *
     * @param savedInstanceState
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState)

        mediaPlayerTabs = savedInstanceState.getSerializable("mediaPlayerTabs") as ArrayList<MediaPlayerTab>
        currentPlayerIndex = savedInstanceState.getInt("currentPlayerIndex")
        currentlyPlayedQueueIndex = savedInstanceState.getInt("currentlyPlayedQueueIndex")

        Log.d(TAG, "oRIS")

        if (recyclerViewAdapter == null) {
            Log.e(TAG, "!!!!!!!!!!!!!!!!!!!!!! recyclerViewAdapter !!!!!!!!!!!!!!!!!!!!!!")
        }

        Log.d(TAG, "oRIS mPTs.sz: " + mediaPlayerTabs.size)
        for (mptab in mediaPlayerTabs) {

            // recyclerViewAdapter has already been re-created in onCreate()
            mptab.playbackQueue.setRecyclerViewAdapter(recyclerViewAdapter)
            mptab.playbackQueue.setPlayingTrackName(playingTrackName)

            //mptab.getName();
            val tabLayout: TabLayout = findViewById(R.id.tabLayout)
            tabLayout.addTab(tabLayout.newTab().setText(
                    mptab.fileSystemNavigator.currentDirectoryName
            ))
        }

        if (0 <= currentPlayerIndex && currentPlayerIndex < mediaPlayerTabs.size) {
            // Set the FS navigator of the currently selected tab
            recyclerViewAdapter!!.setCurrentFileSystemNavigator(
                    mediaPlayerTabs.get(currentPlayerIndex).fileSystemNavigator
            )

            // Restore the vie mode
            val viewMode = mediaPlayerTabs.get(currentPlayerIndex).viewMode
            recyclerViewAdapter!!.viewMode = viewMode

            // Show the controls suited for each view mode
            if (viewMode == 0) {
                switchToFileSystemView()
            } else if (viewMode == 1) {
                switchToPlaybackQueueView()
            } else {
                Log.e(TAG, "!vM: $viewMode")
            }
        }

        // Reset the flag
        // This is needed when onSaveInstanceState is called but onDestroy is not.
        setStateSavedToBundle(0)

        updateFloatingActionButtonVisibility()

        updatePlayingTrackControlPanel(mediaPlayer)
        updateSeekbarProgressAndTime(mediaPlayer)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")

        // Clear the flag
        setStateSavedToBundle(0)

        // Update the view (list of files and directories)
        // Do this both when onRIS is called and is not
        recyclerViewAdapter!!.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.getItemId()


        if (id == R.id.action_settings) {
            Log.d(TAG, "R.id.action_settings")
            return true
        } else if (id == R.id.close_tab) {

            return closeTab()

        } else if (id == R.id.new_tab) {
            Log.d(TAG, "R.id.new_tab")

            // Add a new media player tab
            mediaPlayerTabs.add(MediaPlayerTab())
            mediaPlayerTabs.get(mediaPlayerTabs.size - 1).playbackQueue
                    .setRecyclerViewAdapter(recyclerViewAdapter)

            mediaPlayerTabs.get(mediaPlayerTabs.size - 1).playbackQueue
                    .setPlayingTrackName(playingTrackName)

            // Add a new layout tab
            val tabTitle = mediaPlayerTabs.get(mediaPlayerTabs.size - 1)
                    .fileSystemNavigator.currentDirectoryName
            val newTab = addNewLayoutTab(tabTitle)
            if (newTab == null) {
                Log.d(TAG, "!newTab")
                return true
            }

            //newTab.setId(tabIdCounter);
            tabIdCounter += 1
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

            newTab?.select()

            onTabSelection(newTab)



            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun addNewLayoutTab(tabTitle: String): TabLayout.Tab? {
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        tabLayout.addTab(tabLayout.newTab())
        val newTab = tabLayout.getTabAt(tabLayout.getTabCount() - 1)
        newTab?.setText(tabTitle)
        return newTab;
    }

    private fun startAudioService() {

        // Start the main service of the app
        // This service survives even after this activity is destroyed, e.g. by user
        // swiping it from the task list
        Log.d(TAG, "sAS starting service")
        val serviceIntent = Intent(this, BackgroundAudioService::class.java)

        // Note that there is always only one instance of the service;
        // multiple calls of startService does not result in multiple instance
        // of the service.
        startService(serviceIntent)
    }

    private fun saveStateToFile() {
        Log.d(TAG, "saveStateToFile")
        val file = File(getFilesDir(), APPLICATION_STATE_FILE_NAME)
        try {
            val f = FileOutputStream(file.getPath())
            val stream = ObjectOutputStream(f)

            stream.writeInt(currentlyPlayedQueueIndex)
            stream.writeInt(currentPlayerIndex)
            stream.writeObject(mediaPlayerTabs)

            stream.close()
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

    }

    private fun setStateSavedToBundle(`val`: Int) {

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt("state_saved_to_bundle", `val`)
    }

    private fun restoreStateFromFile() {
        Log.d(TAG, "restoreStateFromFile")
        val file = File(getFilesDir(), APPLICATION_STATE_FILE_NAME)
        try {
            val f = FileInputStream(file.getPath())
            val stream = ObjectInputStream(f)

            currentlyPlayedQueueIndex = stream.readInt()
            currentPlayerIndex = stream.readInt()
            mediaPlayerTabs = stream.readObject() as ArrayList<MediaPlayerTab>
            Log.d(TAG,
                    String.format("cPQI %d, cPI %d", currentlyPlayedQueueIndex, currentPlayerIndex))
            stream.close()
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

    }

    private fun closeTab(): Boolean {
        Log.d(TAG, "R.id.close_tab")
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        val pos = tabLayout.selectedTabPosition
        Log.d(TAG, "tabpos: $pos")
        if (pos < 0) {
            Log.d(TAG, "R.id.ct pos<0")
            return false
        }
        val tab = tabLayout.getTabAt(pos)
        if (tab == null) {
            Log.d(TAG, "R.id.ct !tab")
            return false
        }

        if (pos < mediaPlayerTabs.size) {
            mediaPlayerTabs.removeAt(pos)
        } else {
            Log.w(TAG, "!!!fsn.size")
        }

        Log.d(TAG, "removing tab")
        tabLayout.removeTab(tab)
        // If there are any tab(s) left, onTabSelected has already been invoked
        Log.d(TAG, "tab removed")

        if (tabLayout.getTabCount() === 0) {
            Log.d(TAG, "All tabs removed")
            currentPlayerIndex = -1
            Playback.setCurrentPlayer(null)

            recyclerViewAdapter!!.setCurrentFileSystemNavigator(null)
            // Notify recycler view adapter because otherwise the file list will remain
            // as no new tab is selected and as such onTabSelected() is not called.
            recyclerViewAdapter!!.notifyDataSetChanged()

            return true
        }

        val newSelectedTabPosition = tabLayout.selectedTabPosition

        if (newSelectedTabPosition < 0) {
            Log.d(TAG, "nSTP<0")
            // There are one or more tabs left but none is selected
        } else if (currentPlayerIndex != newSelectedTabPosition) {
            Log.w(TAG, "urrentPlayerIndex != tabLayout.selectedTabPosition")
            currentPlayerIndex = newSelectedTabPosition
        }

        if (0 <= currentPlayerIndex && currentPlayerIndex < mediaPlayerTabs.size) {
            Playback.setCurrentPlayer(
                    mediaPlayerTabs.get(currentPlayerIndex).playbackQueue)
        } else {
            Log.w(TAG, "mPTs.size<=cPI")
        }

        return true
    }

    private fun initRecyclerView() {
        Log.d(TAG, "initRecyclerView called")

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerViewAdapter = RecyclerViewAdapter(this)
        recyclerView.setAdapter(recyclerViewAdapter)
        recyclerView.setLayoutManager(LinearLayoutManager(this))
    }

    /**
     * @brief Updates the selected tab and also updates the current tab index.
     *
     *
     *
     */
    private fun onTabSelection(tab: TabLayout.Tab) {
        val pos = tab.getPosition()
        Log.d(TAG, "onTabSelected called: " + tab.getText() + " (pos: " + pos + ")")

        if (pos < 0 || mediaPlayerTabs.size <= pos) {
            Log.w(TAG, "oTS: invalid tab pos")
            return
        }

        currentPlayerIndex = pos

        recyclerViewAdapter!!.setCurrentFileSystemNavigator(
                mediaPlayerTabs.get(pos).fileSystemNavigator)
        recyclerViewAdapter!!.viewMode = mediaPlayerTabs.get(pos).viewMode
        recyclerViewAdapter!!.notifyDataSetChanged()

        Playback.setCurrentPlayer(
                mediaPlayerTabs.get(pos).playbackQueue)

        //              switchTab(pos);
    }

    private fun setTabListeners() {
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                onTabSelection(tab);
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                val pos = tab.getPosition()
                Log.d(TAG, "onTabUnselected called: " + tab.getText() + " (pos: " + pos + ")")

            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                val pos = tab.getPosition()
                Log.d(TAG, "onTabReselected called: " + tab.getText() + " (pos: " + pos + ")")

            }
        })
    }

    override fun onBackPressed() {
        Log.d(TAG, "oBP")

        if (recyclerViewAdapter == null) {
            Log.w(TAG, "rVA")
            return
        }

        val viewMode = recyclerViewAdapter!!.viewMode
        if (viewMode == 0) {
            val navigator = currentFileSystemNavigator
            var ret = 0
            if (navigator != null) {
                // Try moving to the parent point
                // ret = 0: successfully moved to the parent point
                // ret = -1: was already at the root point
                ret = navigator!!.moveToParent()
            } else {
                // This happens when there are no tabs
                ret = -1
            }

            if (ret == 0) {
                // Moved to the parent directory/point

                // Show/hide FAB depending on whether the directory contains
                // one or more media files.
                updateFloatingActionButtonVisibility()

                recyclerViewAdapter!!.notifyDataSetChanged()

                setSelectedTabLabel(navigator!!.currentDirectoryName)
            } else {
                // We have been already at the root of the tree so
                // moving up to a parent point never happened
                // -> Hand over the control to the super class.
                Log.d(TAG, "s.oBP")
                super.onBackPressed()
            }
        } else if (viewMode == 1) {
            // User pressed the back button while the app is in the play queue view mode.
            // -> Go back to the file system view.
            switchToFileSystemView()
        } else {
            Log.e(TAG, "oBP !!vM")
        }
    }

    fun setSelectedTabLabel(text: String) {

        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        val pos = tabLayout.selectedTabPosition
        if (pos == -1) {
            // No selected tab
            Log.d(TAG, "pos<0")
        } else {
            val tab = tabLayout.getTabAt(pos)
            if (tab != null) {
                tab!!.setText(text)
            }
        }
    }

    fun updateFloatingActionButtonVisibility() {
        val index = currentPlayerIndex
        if (index < 0 || mediaPlayerTabs.size <= index) {
            Log.d(TAG, "uFABV !i$index")
            return
        }

        val navigator = mediaPlayerTabs.get(index).fileSystemNavigator.currentNavigator

        if (navigator == null) {
            Log.d(TAG, "uFABV !cn")
            return
        }

        val fab = floatingActionButton
        if (navigator!!.isAtLeastOneMediaFilePresent) {
            Log.d(TAG, "fab visible")
            fab!!.setVisibility(View.VISIBLE)

            fab!!.requestLayout()
        } else {
            Log.d(TAG, "fab gone")
            fab!!.setVisibility(View.GONE)
        }
    }

    /**
     * @brief Returns the status of the given media file
     *
     *
     * @param filePath
     * @return 1: currently playing, 2: queued, 0: else
     */
    fun getMediaFileStatus(filePath: String): Int {

        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        val tabPos = tabLayout.selectedTabPosition
        if (tabPos < 0 || mediaPlayerTabs.size <= tabPos) {
            return 0
        }

        val player = mediaPlayerTabs.get(tabPos).playbackQueue
        return if (player.isPointed(filePath)) {
            1
        } else if (player.isInQueue(filePath)) {
            2
        } else {
            0
        }
    }

    private fun requestAppPermissions() {
        Log.d(TAG, "rAPs")

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "SDK_INT < LOLLIPOP")
            return
        }

        if (hasReadPermissions() && hasWritePermissions()) {
            Log.d(TAG, "Read/write permissions already granted")
            return
        }


        val myRequestCode = 12

        Log.d(TAG, "Requesting read/write permissions")
        ActivityCompat.requestPermissions(this,
                arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), myRequestCode) // your request code
    }

    private fun hasReadPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) === PackageManager.PERMISSION_GRANTED
    }

    private fun hasWritePermissions(): Boolean {
        return ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) === PackageManager.PERMISSION_GRANTED
    }

    fun onMediaStartRequestedOnScreen(): Boolean {

        var service = BackgroundAudioService.instance
        if (service == null) {
            Log.e(TAG, "oMSROS !service")
            startAudioService()
            service = BackgroundAudioService.instance
            if (service == null) {
                Log.e(TAG, "oMSROS Failed to restart service")
                return false
            }

            // Since we created a service, we have to set the reference to currently played queue.
            // Since this function is for play request made on screen, we assume that
            // currently selected tab == playing tab
            if (0 <= currentPlayerIndex && currentPlayerIndex < mediaPlayerTabs.size) {

                service!!.setCurrentlyPlayedPlaybackQueue(
                        mediaPlayerTabs.get(currentPlayerIndex).playbackQueue
                )
            } else {
                Log.d(TAG, "oMSROS !cPQI")
            }
        }
        val granted = service!!.retrievedAudioFocus()
        if (!granted) {
            return false
        }

        if (currentlyPlayedQueueIndex != currentPlayerIndex) {
            // currently played tab != selected tab.
            if (0 <= currentlyPlayedQueueIndex) {
                if (currentlyPlayedQueueIndex < mediaPlayerTabs.size) {
                    mediaPlayerTabs.get(currentlyPlayedQueueIndex).playbackQueue
                            .saveCurrentPlaybackPosition()
                } else {
                    Log.w(TAG, "sz<=cPQI")
                }
            }
        }

        return true
    }

    fun onMediaStartedOnScreen() {

        if (currentPlayerIndex < 0 || mediaPlayerTabs.size <= currentPlayerIndex) {
            Log.w(TAG, "cPI: $currentPlayerIndex")
            return
        }

        currentlyPlayedQueueIndex = currentPlayerIndex

        // Notify the service once we start playing tracks in a queue
        // - Or to be more exact, we give the service a reference to the instance
        //   storing the playback information (queue, currently played track, etc)
        // - Note that this particular instance of Playback survivies the activity destruction
        //   and recreation
        val service = BackgroundAudioService.instance
        if (service == null) {
            Log.w(TAG, "!service")
        } else {
            service!!.setCurrentlyPlayedPlaybackQueue(
                    mediaPlayerTabs.get(currentlyPlayedQueueIndex).playbackQueue
            )

            service!!.updateMediaControls()
            service!!.showMediaControls()

            updatePlayingTrackControlPanel(mediaPlayer)
        }


        //        View ptc = findViewById(R.id.playing_track_control);
        //        ptc.setVisibility(View.VISIBLE);
    }

    /**
     * @brief Called every second or two to update the seek bar and time display
     *
     *
     *
     * @param mediaPlayer
     */
    fun updateSeekbarProgressAndTime(mediaPlayer: MediaPlayer?) {

        val pos = mediaPlayer!!.getCurrentPosition()

        if (folderViewSeekBar != null) {
            folderViewSeekBar!!.setProgress(pos)
        }

        val time = HorizonUtils.millisecondsToHhmmssOrMmss(pos.toLong())

        // Update the current position, i.e. the 'AA:AA' part in 'AA:AA / BB:BB'
        if (folderViewPlayingTrackTime != null) {
            val text = folderViewPlayingTrackTime!!.getText().toString()
            val separator = text.indexOf(" / ")
            if (0 <= separator) {
                folderViewPlayingTrackTime!!.setText(time + text.substring(separator))
            }
        }
    }

    /**
     * @brief Find better name than 'playing track control panel'
     */
    fun updatePlayingTrackControlPanel(mediaPlayer: MediaPlayer?) {
        Log.d(TAG, "uPTCP")

        val duration = mediaPlayer!!.getDuration()
        if (folderViewSeekBar != null) {
            folderViewSeekBar!!.setMax(duration)
        }

        val time = HorizonUtils.millisecondsToHhmmssOrMmss(duration.toLong())

        // Update the duration, i.e. the 'BB:BB' part in 'AA:AA / BB:BB'
        if (folderViewPlayingTrackTime != null) {
            val text = folderViewPlayingTrackTime!!.getText().toString()
            val separator = text.indexOf(" / ")
            if (0 <= separator) {
                folderViewPlayingTrackTime!!.setText(text.substring(0, separator + 3) + time)
            }
        }

        val name: String?
        if (0 <= currentlyPlayedQueueIndex && currentlyPlayedQueueIndex < mediaPlayerTabs.size) {
            val currentlyPlayed = mediaPlayerTabs.get(currentlyPlayedQueueIndex).playbackQueue
            if (currentlyPlayed != null) {
                name = currentlyPlayed!!.currentlyPlayedMediaName

                if (playingTrackName != null && name != null) {
                    playingTrackName!!.setText(name)
                } else {
                    Log.w(TAG, "uPTCP !pTN")
                }
            } else {
                Log.w(TAG, "uPTCP !cP")
            }
        }
    }

    fun switchToPlaybackQueueView() {

        // Save the view mode
        mediaPlayerTabs.get(currentPlayerIndex).viewMode = 1

        // Hide the playing track control and show the play queue
        // media control.
        hidePlayingTrackControl()
        showPlaybackQueueControl()

        recyclerViewAdapter!!.viewMode = 1

        recyclerViewAdapter!!.notifyDataSetChanged()

        if (currentPlayerIndex < 0 || mediaPlayerTabs.size <= currentPlayerIndex) {
            Log.d(TAG, "sTFSV !!cPQI")
        } else {
            // Save the view mode to the tab instance.
            // We'll use this info to save the view mode on per-tab basis,
            // and restore the mode for each tab every time the user swtiches tabs.
            mediaPlayerTabs.get(currentPlayerIndex).viewMode = 1
        }
        //        MediaPlayerTab mediaPlayerTab = getCurrentMediaPlayerTab();
        //        if(mediaPlayerTab != null) {
        //            mediaPlayerTab.setMode(1);
        //        } else {
        //            Log.d(TAG,"!mPT");
        //        }
    }

    fun switchToFileSystemView() {

        if (currentPlayerIndex < 0 || mediaPlayerTabs.size <= currentPlayerIndex) {
            Log.d(TAG, "sTFSV !!cPI")
            return
        }

        // Save the view mode
        mediaPlayerTabs.get(currentPlayerIndex).viewMode = 0

        val playbackQueue = mediaPlayerTabs.get(currentPlayerIndex).playbackQueue

        if (0 < playbackQueue.mediaFilePathQueue.size) {
            showPlayingTrackControl()
        }
        hidePlaybackQueueControl()

        recyclerViewAdapter!!.viewMode = 0
        recyclerViewAdapter!!.notifyDataSetChanged()
    }

    fun showPlaybackQueueControl() {
        val pc: LinearLayout = findViewById(R.id.playback_control)
        pc.setVisibility(View.VISIBLE)
    }

    fun hidePlaybackQueueControl() {
        val pc: LinearLayout = findViewById(R.id.playback_control)
        pc.setVisibility(View.GONE)
    }

    fun initFloatingActionButton() {
        Log.d(TAG, "iFAB")

        val fab = findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener(object : View.OnClickListener {

            /**
             * When FAB is clicked, the app
             * - Collects media files from the current directory
             * - Puts them in the queue
             * - Starts playing them
             * @param view
             */
            override fun onClick(view: View) {
                Log.d(TAG, "fab.oC")
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()

                if (currentPlayerIndex < 0) {
                    Log.w(TAG, "fab.oC.cPI<0")
                    return
                }

                if (mediaPlayerTabs.size <= currentPlayerIndex) {
                    Log.w(TAG, "mPTs.sz<=cPI")
                    return
                }

                val filesAndDirs = mediaPlayerTabs.get(currentPlayerIndex).fileSystemNavigator
                        .currentDirectoryEntries

                // Put all the media files in the current directory to the queue and start playing
                val mediaFiles = HorizonUtils.pickMediaFiles(filesAndDirs)
                Log.d(TAG, "mFs.sz: " + mediaFiles.size)

                if (mediaFiles.size === 0) {
                    Log.w(TAG, "0!mFs.sz")
                    return
                }

                val readyToPlay = onMediaStartRequestedOnScreen()
                if (!readyToPlay) {
                    Log.d(TAG, "!rTP")
                    return
                }
                val player = mediaPlayerTabs.get(currentPlayerIndex).playbackQueue
                player.clearQueue()
                player.addToQueue(mediaFiles)
                player.startCurrentlyPointedMediaInQueue()
                onMediaStartedOnScreen()

                switchToPlaybackQueueView()

                // Update background colors of queued tracks
                // This is done in switchToPlaybackQueueView() so commented out.
                //recyclerViewAdapter.notifyDataSetChanged();

                //if( mediaPlayer != null && mediaPlayer.isPlaying() ) {
                //    mediaPlayer.stop();
                //}
            }
        })

        fab.setImageBitmap(textAsBitmap("▶", 40f, Color.WHITE))

        val params = fab.getLayoutParams()
        Log.v(TAG, "fab layout params: " + params::class)
        val coordinatorLayoutParams = params as CoordinatorLayout.LayoutParams

        coordinatorLayoutParams.setBehavior(MyFabBehavior())
        if (fab.isInLayout()) {
            Log.w(TAG, "IS IN LAYOUT")
        } else {
            fab.requestLayout()
        }

        fab.setVisibility(View.GONE)
    }

    fun initPlayQueueMediaControlButtons() {
        val btn = findViewById(R.id.play_pause_button) as Button
        if (btn == null) {
            Log.d(TAG, "!play/pause btn")
            return
        }

        btn!!.setOnClickListener(
                object : View.OnClickListener {
                    override fun onClick(view: View) {
                        Log.d(TAG, "btn pressed (play/pause)")
                        val self = view as Button//findViewById(R.id.play_pause);

                        val mediaPlayer = Playback.mediaPlayer
                        // Both isPlaying() and pause() can throw IllegalStateException.

                        try {
                            if(mediaPlayer == null) {
                                return
                            }

                            if (mediaPlayer.isPlaying()) {
                                Log.d(TAG, "btn pausing")
                                mediaPlayer.pause()
                                self.setText("▶")
                            } else {
                                Log.d(TAG, "btn playing/resuming")
                                mediaPlayer.start() // Start/resume playback
                                self.setText("||")
                            }
                        } catch (ise: IllegalStateException) {
                            Log.d(TAG, "ise caught")
                        } catch (e: Exception) {
                            Log.d(TAG, "Exception caught")
                        }

                    }
                })

        val prevTrackBtn = findViewById(R.id.prev_track) as Button
        if (prevTrackBtn == null) {
            Log.d(TAG, "!pTB")
            return
        }

        prevTrackBtn!!.setOnClickListener(
                object : View.OnClickListener {
                    override fun onClick(view: View) {
                        Log.d(TAG, "btn:p (prev)")
                        val self = view as Button

                        if (currentPlayerIndex < 0 || mediaPlayerTabs.size <= currentPlayerIndex) {
                            return
                        }

                        onMediaStartRequestedOnScreen()
                        val tab = mediaPlayerTabs.get(currentPlayerIndex)
                        tab.playbackQueue.playPrevTrack()
                        onMediaStartedOnScreen()
                    }
                })

        val nextTrackBtn = findViewById(R.id.next_track) as Button
        if (nextTrackBtn == null) {
            Log.d(TAG, "!nTB")
            return
        }

        nextTrackBtn!!.setOnClickListener(
                object : View.OnClickListener {
                    override fun onClick(view: View) {
                        Log.d(TAG, "btn:p (next)")
                        val self = view as Button

                        if (currentPlayerIndex < 0 || mediaPlayerTabs.size <= currentPlayerIndex) {
                            return
                        }

                        onMediaStartRequestedOnScreen()
                        val tab = mediaPlayerTabs.get(currentPlayerIndex)
                        tab.playbackQueue.playNextTrack()
                        onMediaStartedOnScreen()
                    }
                })
    }

    fun showPlayingTrackControl() {
        Log.d(TAG, "sPTB")

        // Show the playing track info and control (pause/resume)
        val ptc: LinearLayout = findViewById(R.id.playing_track_control)
        ptc.setVisibility(View.VISIBLE)
    }

    fun hidePlayingTrackControl() {
        Log.d(TAG, "hPTC")

        // Show the playing track info and control (pause/resume)
        val ptc: LinearLayout = findViewById(R.id.playing_track_control)
        ptc.setVisibility(View.GONE)
    }

    companion object {

        private val TAG = "MainActivity"

        //private ServiceConnection serviceConnection = null;

        private var tabIdCounter = 1000

        private val APPLICATION_STATE_FILE_NAME = "appstate"

        //method to convert your text to image
        fun textAsBitmap(text: String, textSize: Float, textColor: Int): Bitmap {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.setTextSize(textSize)
            paint.setColor(textColor)
            paint.setTextAlign(Paint.Align.LEFT)
            val baseline = -paint.ascent() // ascent() is negative
            val width = (paint.measureText(text) + 0.0f).toInt() // round
            val height = (baseline + paint.descent() + 0.0f).toInt()
            val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            val canvas = Canvas(image)
            canvas.drawText(text, 0f, baseline, paint)
            return image
        }
    }
}
