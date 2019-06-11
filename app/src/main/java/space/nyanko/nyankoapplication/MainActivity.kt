package space.nyanko.nyankoapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import com.google.android.material.tabs.TabLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.SeekBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import android.view.View
import android.view.Menu
import android.view.MenuItem

import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.util.ArrayList

@SuppressLint("RestrictedApi")
class MainActivity : AppCompatActivity(), BackgroundAudioService.AudioServiceCallbacks, Playback.MediaPlayerCallback {

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
    var selectedTabIndex: Int
        get() = MediaPlayerTab.selectedTabIndex
        set(value) {
            MediaPlayerTab.selectedTabIndex = value
        }

    //==================== Model ====================

    var mediaPlayerTabs: ArrayList<MediaPlayerTab> = ArrayList()
        get() = MediaPlayerTab.tabs
        private set

    private var recyclerViewAdapter: RecyclerViewAdapter? = null

    var folderViewPlayingTrackTime: TextView? = null
    var folderViewSeekBar: SeekBar? = null
    private var isTrackingSeekBar: Boolean = false

    private var playingTrackName: TextView? = null

    private var backgroundAudioService: BackgroundAudioService? = null
        get() {
            val service = BackgroundAudioService.instance
            if (service == null) {
                Log.d(TAG, "gBAS !service")
                return null
            } else {
                return service
            }
        }

    private var boundToService: Boolean = false

    /**
     * Callbacks for service binding, passed to bindService()
     * */
    private var serviceConnection: ServiceConnection? = null

    private val currentFileSystemNavigator: FileSystemNavigator?
        get() {

            if (selectedTabIndex < 0 || mediaPlayerTabs.size <= selectedTabIndex) {
                Log.w(TAG, "invalid cFSNI")
                return null
            }

            return mediaPlayerTabs.get(selectedTabIndex).fileSystemNavigator
        }

    // No selected tab
    val playerOfSelectedTab: Playback?
        get() {

            val tabLayout: TabLayout = findViewById(R.id.tabLayout) ?: return null
            val pos = tabLayout.selectedTabPosition
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

    val resumeFab: FloatingActionButton?
        get() {
            val fab = findViewById(R.id.resume) as FloatingActionButton
            if (fab == null) {
                Log.e(TAG, "!resumeFab")
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

            return service.mediaPlayer
        }

    private var mediaInfoPopup: MediaInfoPopupWindow? = null

    private var seekBarUpdateAgent: SeekBarUpdateAgent = SeekBarUpdateAgent()

    inner class MyFabBehavior : FloatingActionButton.Behavior() {

        override fun onDependentViewChanged(parent: CoordinatorLayout,
                                            child: FloatingActionButton,
                                            dependency: View): Boolean {
            //Log.d(TAG, "fab_behavior oDVC")
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

        val currentActivity: MainActivity = this

        serviceConnection = object : ServiceConnection {

            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                Log.d(TAG, "onServiceConnected")
                // cast the IBinder and get MyService instance
                val binder = service as BackgroundAudioService.LocalBinder
                val audioService = binder.service
                backgroundAudioService = audioService
                boundToService = true
                audioService.setAudioServiceCallbacks(currentActivity)//@MyActivity)
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                Log.d(TAG, "onServiceDisconnected")
                boundToService = false
            }
        }
    }

    override protected fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "s.oC")
        super.onCreate(savedInstanceState)
        Log.d(TAG, "oC t" + Thread.currentThread().id)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        initFloatingActionButton()

        initResumeFab()

        initPlayingTrackControl()

        hidePlaybackQueueControl()
        hidePlayingTrackControl()

        playingTrackName = findViewById(R.id.playing_track_name) as TextView
        playingTrackName!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                Log.d(TAG, "oC")
                switchToPlaylistView()
            }
        })

        // Some C/C++ functions access filesystem so request the user file r/w permissions
        val permissionManager = AppPermissionManager()
        permissionManager.requestAppPermissions(this)

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

                selectedTabIndex = 0
            }
            Log.d(TAG, "restored: $selectedTabIndex")
            Log.d(TAG, "mptabs: " + mediaPlayerTabs.size)

            setViewsToPlaybackManagers()

            restoreTabLayoutTabs()

            if (0 <= selectedTabIndex && selectedTabIndex < mediaPlayerTabs.size) {
                recyclerViewAdapter!!.setCurrentFileSystemNavigator(
                        mediaPlayerTabs.get(selectedTabIndex).fileSystemNavigator
                )

                Log.d(TAG, "Selecting a tab: $selectedTabIndex")
                // Select the tab that had been selected before the app was destroyed
                val tab = tabLayout.getTabAt(selectedTabIndex)
                tab?.select()
            } else {
                Log.w(TAG, "oC !cPI: $selectedTabIndex")
            }
        }

        // Call this after tabLayout.addTab() in the block above in order to avoid
        // the invocation of onTabSelected() as the first tab added via tabLayout.addTab()
        // becomes the selected tab and it invokes tab listener's onTabSelected callback.
        setTabListeners()

        initPlaylistViewMediaControlButtons()

        // TODO: do this only once at startup
        if (savedInstanceState == null) {
            if (BackgroundAudioService.instance == null) {
                // Start a new service, or restart it.
                startAudioService()
            } else {
                // Could this be possible?
                Log.d(TAG, "oC service != null")
                BackgroundAudioService.instance?.updateMediaControls()
            }
        }

        folderViewPlayingTrackTime = findViewById(R.id.playing_track_time) as TextView

        initSeekBar();

        mediaInfoPopup = MediaInfoPopupWindow(this)

        setSystemUiVisibilityChangeListener()

        //LockScreenMediaControl.guideUserToEnableNotificationAccess(this,applicationContext)

        backgroundAudioService?.updateMediaControls()

        Playback.mediaPlayerCallback = this

        seekBarUpdateAgent.enable(this)
    }

    override protected fun onStart() {
        super.onStart()
        Log.d(TAG, "oStart")
        // bind to Service
        val intent = Intent(this, BackgroundAudioService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override protected fun onStop() {
        super.onStop()
        Log.d(TAG, "oStop: ${currentlyPlayedQueueIndex} ${selectedTabIndex} ${mediaPlayerTabs.size}")
        // Unbind from service
        if (boundToService) {
            backgroundAudioService?.setAudioServiceCallbacks(null) // unregister
            unbindService(serviceConnection)
            boundToService = false
        }

        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val stateSaved = sharedPref.getInt("state_saved_to_bundle", 0)
        if (stateSaved == 0) {
            saveStateToFile()
        }

        // Reset the flag
        setStateSavedToBundle(0)
    }

    /**
     * onDestroy() is *NOT* always called:
     * - http://developer.android.com/reference/android/app/Activity.html#onDestroy%28%29
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "oDestroy")

        // For now we stop the service so that we can test their behavior from the start
        // every time we close and restart the app
        //Intent serviceIntent = new Intent(this,BackgroundAudioService.class);
        //stopService(serviceIntent);

        Playback.mediaPlayerCallback = null
    }

    override protected fun onSaveInstanceState(savedInstanceState: Bundle) {

        savedInstanceState.putSerializable("mediaPlayerTabs", mediaPlayerTabs)
        savedInstanceState.putInt("selectedTabIndex", selectedTabIndex)
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
        selectedTabIndex = savedInstanceState.getInt("selectedTabIndex")
        currentlyPlayedQueueIndex = savedInstanceState.getInt("currentlyPlayedQueueIndex")

        Log.d(TAG, "oRIS")

        if (recyclerViewAdapter == null) {
            Log.e(TAG, "!!!!!!!!!!!!!!!!!!!!!! recyclerViewAdapter !!!!!!!!!!!!!!!!!!!!!!")
        }

        Log.d(TAG, "oRIS mPTs.sz: " + mediaPlayerTabs.size)
        val savedTabIndex = selectedTabIndex
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)

        setViewsToPlaybackManagers()

        restoreTabLayoutTabs()

        // In the for loop above, the first call of tabLayout.addTab() creates the first tab
        // and selects the created tab, which causes the onSelection() to be triggered and
        // selectedTabIndex to be overwritten, so we manually restore the value here
        selectedTabIndex = savedTabIndex

        if (0 <= selectedTabIndex && selectedTabIndex < mediaPlayerTabs.size) {
            // Set the FS navigator of the currently selected tab
            recyclerViewAdapter!!.setCurrentFileSystemNavigator(
                    mediaPlayerTabs.get(selectedTabIndex).fileSystemNavigator
            )

            // Restore the vie mode
            val viewMode = mediaPlayerTabs.get(selectedTabIndex).viewMode
            recyclerViewAdapter!!.viewMode = viewMode

            // Show the controls suited for each view mode
            if (viewMode == 0) {
                switchToFolderView()
            } else if (viewMode == 1) {
                switchToPlaylistView()
            } else {
                Log.e(TAG, "!vM: $viewMode")
            }
        }

        if (0 <= selectedTabIndex && selectedTabIndex < tabLayout.tabCount) {
            Log.d(TAG, "Re-selecting tab " + selectedTabIndex)
            val tab = tabLayout.getTabAt(selectedTabIndex)
            if (tab == null) {
                Log.d(TAG, "oRIS !tab")
            } else {
                tab.select()
            }
        }

        // Reset the flag
        // This is needed when onSaveInstanceState is called but onDestroy is not.
        setStateSavedToBundle(0)

        updateFloatingActionButtonVisibility()

        updateResumeButtonVisibility()

        updateSeekbarProgressAndTime()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    /**
     * Note that this method is called not only when the app is resumed but also when
     * it is first launched.
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")

        // Clear the flag
        setStateSavedToBundle(0)

        // Update the view (list of files and directories)
        // Do this both when onRIS is called and is not
        recyclerViewAdapter!!.notifyDataSetChanged()

        val tab: MediaPlayerTab? = getCurrentlySelectedMediaPlayerTab()
        val viewMode: Int? = tab?.viewMode
        if (viewMode == null) {
            Log.d(TAG, "oR vM!")
        } else if (viewMode == 0) {
            switchToFolderView()
        } else if (viewMode == 1) {
            switchToPlaylistView()
        }

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

            newTab.select()

            onTabSelection(newTab)



            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {

        // handle when the user pull down the notification bar where
        // (hasFocus will ='false') & if the user pushed the
        // notification bar back to the top, then (hasFocus will ='true')
        if (!hasFocus) {
            Log.i(TAG, "Notification bar is pulled down")
        } else {
            Log.i(TAG, "Notification bar is pushed up")
        }
        super.onWindowFocusChanged(hasFocus)
    }

    private fun setSystemUiVisibilityChangeListener() {
        Log.d(TAG,"sSUVCL")
        // Detecting if the user swipe from the top down to the phone screen to show up the status bar
        // (without showing the notification area); so we could set mStatusBarShow flat to true to allow
        // us hide the status bar for the next onClick event
        val decorView = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            // Note that system bars will only be "visible" if none of the
            // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                Log.d(TAG,"status bar shown")
            } else {
                Log.d(TAG,"status bar hidden")
            }
        }
    }

    private fun setViewsToPlaybackManagers() {
        // Assuming all tab are now restored, we set the recycler view
        // references to playback instances.
        for (mptab in mediaPlayerTabs) {
            val playbackTracker = mptab.playbackQueue
            playbackTracker.setRecyclerViewAdapter(recyclerViewAdapter)
            playbackTracker.setPlayingTrackName(playingTrackName)
        }
    }

    private fun restoreTabLayoutTabs() {
        // Add tablayout tabs
        // Unlike other views, these tabs need to be manually re-created.
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        for (mptab in mediaPlayerTabs) {
            val newTab = tabLayout.newTab()
            newTab.setText(mptab.fileSystemNavigator.currentDirectoryName)
            Log.d(TAG, "adding tab. index: $selectedTabIndex")
            tabLayout.addTab(newTab)
        }
        Log.d(TAG, "rTLT tabs: ${tabLayout.tabCount}")
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

        val service = BackgroundAudioService.instance
        if (service == null) {
            Log.e(TAG, "sAS !service")
            return
        }

        // Since we created a service, we have to set the reference to currently played queue.
        // Since this function is for play request made on screen, we assume that
        // currently selected tab == playing tab
        if (0 <= selectedTabIndex && selectedTabIndex < mediaPlayerTabs.size) {

            service.setCurrentlyPlayedPlaybackQueue(
                    mediaPlayerTabs.get(selectedTabIndex).playbackQueue
            )
        } else {
            Log.d(TAG, "sAS !cPQI")
        }
    }

    private fun saveStateToFile() {
        Log.d(TAG, "sSTF: ${currentlyPlayedQueueIndex} ${selectedTabIndex} ${mediaPlayerTabs.size}")
        val file = File(getFilesDir(), APPLICATION_STATE_FILE_NAME)
        try {
            val f = FileOutputStream(file.getPath())
            val stream = ObjectOutputStream(f)

            stream.writeInt(currentlyPlayedQueueIndex)
            stream.writeInt(selectedTabIndex)
            stream.writeObject(mediaPlayerTabs)

            stream.close()
        } catch (e: Exception) {
            Log.e(TAG, "sSTF!: " + e.toString())
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
            selectedTabIndex = stream.readInt()
            //mediaPlayerTabs = stream.readObject() as ArrayList<MediaPlayerTab>
            val tabs = stream.readObject() as ArrayList<MediaPlayerTab>
            Log.d(TAG, String.format("cPQI %d, cPI %d, tabs: %d",
                    currentlyPlayedQueueIndex,
                    selectedTabIndex,
                    tabs.size))

            // Remove all the tabs if there are any
            mediaPlayerTabs.clear()

            // Load the tabs from file
            // Replace the tab on which a track has been playing
            // because the playback state has been changed
            for(i in 0 until tabs.size) { // tabs.size is excluded
                if(i == currentlyPlayedQueueIndex) {
                    val currentlyPlayed = BackgroundAudioService.instance?.getCurrentlyPlayedPlaybackQueue()
                    if(currentlyPlayed != null) {
                        var newTab = MediaPlayerTab()
                        newTab.fileSystemNavigator = tabs[i].fileSystemNavigator
                        newTab.playbackQueue = currentlyPlayed
                        mediaPlayerTabs.add(newTab)
                    } else {
                        mediaPlayerTabs.add(tabs[i])
                    }
                } else {
                    mediaPlayerTabs.add(tabs[i])
                }
            }
            stream.close()
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

    }

    private fun closeTab(): Boolean {
        Log.d(TAG, "closeTab")
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        val pos = tabLayout.selectedTabPosition
        Log.d(TAG, "tabpos: $pos")
        if (pos < 0) {
            Log.d(TAG, "ct pos<0")
            return false
        }
        val tab = tabLayout.getTabAt(pos)
        if (tab == null) {
            Log.d(TAG, "ct !tab")
            return false
        }

        // If the closing tab is also the playing tab
        // - Stop the playback
        // - Hide the notification
        // - Hide playing track control and playlist view control
        if(pos == currentlyPlayedQueueIndex) {
            Log.d(TAG, "ct pos==cPQI")
            currentlyPlayedQueueIndex = -1
            mediaPlayer?.stop()
            val service = BackgroundAudioService.instance
            if(service != null) {
                service.clearMediaControls()
                service.hideMediaControls()
            }

            hidePlayingTrackControl()
            hidePlaybackQueueControl()
            hideSeekBar()
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
            selectedTabIndex = -1

            recyclerViewAdapter!!.setCurrentFileSystemNavigator(null)
            // Notify recycler view adapter because otherwise the file list will remain
            // as no new tab is selected and as such onTabSelected() is not called.
            recyclerViewAdapter!!.notifyDataSetChanged()

            // Hide FABs
            floatingActionButton?.setVisibility(View.GONE)
            resumeFab?.setVisibility(View.GONE)

            return true
        }

        val newSelectedTabPosition = tabLayout.selectedTabPosition

        if (newSelectedTabPosition < 0) {
            Log.d(TAG, "nSTP<0")
            // There are one or more tabs left but none is selected
        } else if (selectedTabIndex != newSelectedTabPosition) {
            Log.w(TAG, "urrentPlayerIndex != tabLayout.selectedTabPosition")
            selectedTabIndex = newSelectedTabPosition
        }

        if (0 <= selectedTabIndex && selectedTabIndex < mediaPlayerTabs.size) {
        } else {
            Log.w(TAG, "mPTs.size<=cPI")
        }

        return true
    }

    private fun initRecyclerView() {
        Log.d(TAG, "initRecyclerView")

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
        Log.d(TAG, "onTabSelection: " + tab.getText() + " " + pos)

        if (pos < 0 || mediaPlayerTabs.size <= pos) {
            Log.w(TAG, "oTS: invalid tab pos")
            return
        }

        selectedTabIndex = pos

        recyclerViewAdapter!!.setCurrentFileSystemNavigator(
                mediaPlayerTabs.get(pos).fileSystemNavigator)

        val navigator = mediaPlayerTabs.get(pos).fileSystemNavigator
        setTitle(navigator?.currentDirectoryName ?: "")

        // These are called in switchToFolderView() so no need
        // to called them here.
        //recyclerViewAdapter!!.viewMode = mediaPlayerTabs.get(pos).viewMode
        //recyclerViewAdapter!!.notifyDataSetChanged()

        // When the tab is selected, the view is always in the folder view mode
        mediaPlayerTabs.get(pos).viewMode = 0

        switchToFolderView()
    }

    private fun setTabListeners() {
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                onTabSelection(tab);
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                val pos = tab.getPosition()
                Log.d(TAG, "onTabUnselected: " + tab.getText() + " " + pos)

                // Set the view mode of unselected tab to Folder View
                if(0 <= pos && pos < mediaPlayerTabs.size) {
                    mediaPlayerTabs.get(pos).viewMode = 0
                }

                // hiding playlist control & showing playing track control
                // are done in onTabSelection() via switchToFolderView()
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                val pos = tab.getPosition()
                Log.d(TAG, "onTabReselected: " + tab.getText() + " " + pos)

            }
        })
    }

    override fun onBackPressed() {
        Log.d(TAG, "oBP")

        if (recyclerViewAdapter == null) {
            Log.w(TAG, "!rVA")
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
                ret = navigator.moveToParent()
                MediaPlayerTab.getSelected()?.metadataCache?.clear()
            } else {
                // This happens when there are no tabs
                ret = -1
            }

            if (ret == 0) {
                // Moved to the parent directory/point

                setTitle(navigator?.currentDirectoryName ?: "")

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
            switchToFolderView()
        } else {
            Log.e(TAG, "oBP !!vM")
        }
    }

    fun resume() {
        Log.d(TAG, "resume")

        if(selectedTabIndex < 0 || mediaPlayerTabs.size <= selectedTabIndex) {
            Log.d(TAG, "resume !cPI: $selectedTabIndex")
            return
        }

        val mediaPlayerTab = mediaPlayerTabs.get(selectedTabIndex)
        if(mediaPlayerTab.playbackQueue.mediaFilePathQueue.size == 0) {
            Log.d(TAG,"resume q.0")
            return
        }

        val readyToPlay = onMediaStartRequestedOnScreen()
        if (!readyToPlay) {
            Log.d(TAG, "resume !rTP")
            return
        }
        val player = mediaPlayerTabs.get(selectedTabIndex).playbackQueue
        player.resumeCurrentlyPointedMediaInQueue()
        onMediaStartedOnScreen()

        switchToPlaylistView()
    }

    fun updateResumeButtonVisibility() {
        Log.d(TAG, "uRBV")

        val mediaPlayerTab = getCurrentlySelectedMediaPlayerTab()

        if(mediaPlayerTab == null) {
            Log.d(TAG, "uRBV !mPT")
            return
        }

        val playQueue = mediaPlayerTab.playbackQueue
        if(playQueue == null) {
            Log.d(TAG, "uRBV!!!")
            return
        }

        if(0 < playQueue.mediaFilePathQueue.size) {
            resumeFab?.setVisibility(View.VISIBLE)
        } else {
            resumeFab?.setVisibility(View.GONE)
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
                tab.setText(text)
            }
        }
    }

    fun updateFloatingActionButtonVisibility() {
        val index = selectedTabIndex
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
        if (navigator.isAtLeastOneMediaFilePresent) {
            Log.d(TAG, "fab visible")
            fab?.setVisibility(View.VISIBLE)

            fab?.requestLayout()
        } else {
            Log.d(TAG, "fab gone")
            fab?.setVisibility(View.GONE)
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

    public fun getCurrentlySelectedMediaPlayerTab(): MediaPlayerTab? {
        if (0 <= selectedTabIndex && selectedTabIndex < mediaPlayerTabs.size) {
            return mediaPlayerTabs[selectedTabIndex]
        } else {
            Log.d(TAG, "gCSMPT: " + selectedTabIndex + ", " + mediaPlayerTabs.size)
            return null
        }
    }

    fun onMediaStartRequestedOnScreen(): Boolean {
        Log.e(TAG, "oMSROS")

        var service = BackgroundAudioService.instance
        if (service == null) {
            Log.e(TAG, "oMSROS !service")
            startAudioService()
            service = BackgroundAudioService.instance
            if (service == null) {
                Log.e(TAG, "oMSROS Failed to restart service")
                return false
            }
        }

        Log.d(TAG, "oMSROS indices: $selectedTabIndex $currentlyPlayedQueueIndex")
        if (currentlyPlayedQueueIndex != selectedTabIndex) {
            // currently played tab != selected tab.
            if (0 <= currentlyPlayedQueueIndex) {
                if (currentlyPlayedQueueIndex < mediaPlayerTabs.size) {
                    Log.d(TAG, "saving playback pos for " + currentlyPlayedQueueIndex)
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
        Log.d(TAG, "oMSOS " + selectedTabIndex)

        if (selectedTabIndex < 0 || mediaPlayerTabs.size <= selectedTabIndex) {
            Log.w(TAG, "cPI: $selectedTabIndex")
            return
        }

        currentlyPlayedQueueIndex = selectedTabIndex

        // Notify the service once we start playing tracks in a queue
        // - Or to be more exact, we give the service a reference to the instance
        //   storing the playback information (queue, currently played track, etc)
        // - Note that this particular instance of Playback survivies the activity destruction
        //   and recreation
        val service = BackgroundAudioService.instance
        if (service == null) {
            Log.w(TAG, "!service")
        } else {
            service.setCurrentlyPlayedPlaybackQueue(
                    mediaPlayerTabs.get(currentlyPlayedQueueIndex).playbackQueue
            )

            service.updateMediaControls()
            service.showMediaControls()

            updatePlayingTrackControlPanel(mediaPlayer)
        }

        showSeekBar()

        //        View ptc = findViewById(R.id.playing_track_control);
        //        ptc.setVisibility(View.VISIBLE);
    }

    override fun onAudioPlay() {
        Log.d(TAG, "onAudioPlay")
        updatePlayingTrackPlayPauseButton(true)
        updatePlaylistViewPlayPauseButton(true)
    }

    override fun onAudioPause() {
        Log.d(TAG, "onAudioPause")
        updatePlayingTrackPlayPauseButton(false)
        updatePlaylistViewPlayPauseButton(false)
    }

    override fun onAudioStop() {
        Log.d(TAG, "onAudioStop")
        updatePlayingTrackPlayPauseButton(false)
        updatePlaylistViewPlayPauseButton(false)
    }

    override fun onMediaStarted(started: Boolean) {
        super.onMediaStarted(started)

        // Repaint GUI as a currently played track has just been changed.
        // or the failed-to-start track might need to show the status on its view
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter!!.notifyDataSetChanged()
        } else {
            Log.e(TAG, "oMS !rVA")
        }

        if(started) {
            updatePlayingTrackControlPanel(this.mediaPlayer)
        }
    }

    /**
     * @brief Called every second or two to update the seek bar and time display
     *
     *
     *
     * @param mediaPlayer
     */
    fun updateSeekbarProgressAndTime() {

        val mediaPlayer = this.mediaPlayer

        if (mediaPlayer == null) {
            Log.w(TAG, "uSPAT !mP")
            return
        }

        val pos = mediaPlayer.getCurrentPosition()
        //Log.d(TAG, "uSPAT " + pos)

        folderViewSeekBar?.setProgress(pos)

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
     *
     * - Updates the track duration information (time and seekbar)
     *   - Note that this function does not update the current position
     * - Updates the playing track title
     */
    fun updatePlayingTrackControlPanel(mediaPlayer: MediaPlayer?) {
        Log.d(TAG, "uPTCP " + currentlyPlayedQueueIndex)

        if (mediaPlayer == null) {
            return
        }

        // Duration
        val duration = mediaPlayer.getDuration()
        if(duration == -1) {
            // Duration was not available
            folderViewSeekBar?.max = 0
            folderViewPlayingTrackTime?.text = "\uD83D\uDC31 / \uD83D\uDC08"
        } else {
            Log.d(TAG, "sB max " + duration)
            folderViewSeekBar?.setMax(duration)

            val time = HorizonUtils.millisecondsToHhmmssOrMmss(duration.toLong())

            // Update the duration, i.e. the 'BB:BB' part in 'AA:AA / BB:BB'
            if (folderViewPlayingTrackTime != null) {
                val text = folderViewPlayingTrackTime!!.getText().toString()
                val separator = text.indexOf(" / ")
                if (0 <= separator) {
                    folderViewPlayingTrackTime!!.setText(text.substring(0, separator + 3) + time)
                }
            }

        }

        // Track title
        val name: String?
        if (0 <= currentlyPlayedQueueIndex && currentlyPlayedQueueIndex < mediaPlayerTabs.size) {
            val currentlyPlayed = mediaPlayerTabs.get(currentlyPlayedQueueIndex).playbackQueue
            if (currentlyPlayed != null) {
                name = currentlyPlayed.currentlyPlayedMediaName

                if (playingTrackName != null && name != null) {
                    playingTrackName!!.setText(name)
                } else {
                    Log.w(TAG, "uPTCP !pTN")
                    playingTrackName?.text = "\uD83D\uDE40" // 🙀
                }
            } else {
                Log.w(TAG, "uPTCP !cP")
                playingTrackName?.text = "\uD83D\uDC3C" // 🐼
            }
        } else {
            Log.d(TAG,"uPTCP !cPQI " + currentlyPlayedQueueIndex);
            playingTrackName?.text = "\uD83D\uDC31" // 🐱
        }
    }

    fun switchToPlaylistView() {
        Log.d(TAG, "sTPV")

        // Save the view mode
        mediaPlayerTabs.get(selectedTabIndex).viewMode = 1

        // Hide the playing track control and show the play queue
        // media control, after updating the play/pause button
        hidePlayingTrackControl()
        updatePlaylistViewPlayPauseButton(isMediaPlaying())
        showPlaybackQueueControl()

        showSeekBar()

        recyclerViewAdapter!!.viewMode = 1

        recyclerViewAdapter!!.notifyDataSetChanged()

        if (selectedTabIndex < 0 || mediaPlayerTabs.size <= selectedTabIndex) {
            Log.d(TAG, "sTFSV !!cPQI")
        } else {
            // Save the view mode to the tab instance.
            // We'll use this info to save the view mode on per-tab basis,
            // and restore the mode for each tab every time the user swtiches tabs.
            mediaPlayerTabs.get(selectedTabIndex).viewMode = 1
        }
        //        MediaPlayerTab mediaPlayerTab = getCurrentMediaPlayerTab();
        //        if(mediaPlayerTab != null) {
        //            mediaPlayerTab.setMode(1);
        //        } else {
        //            Log.d(TAG,"!mPT");
        //        }
    }

    fun switchToFolderView() {
        Log.d(TAG,"sTFV")

        if (selectedTabIndex < 0 || mediaPlayerTabs.size <= selectedTabIndex) {
            Log.d(TAG, "sTFSV !!cPI")
            return
        }

        // Save the view mode
        mediaPlayerTabs.get(selectedTabIndex).viewMode = 0

//        val playbackQueue = mediaPlayerTabs.get(selectedTabIndex).playbackQueue
        val playbackQueue = getCurrentlyPlayingTab()?.playbackQueue

        // Show the playing track control if the play queue has at least
        // one track
        if (playbackQueue != null && 0 < playbackQueue.mediaFilePathQueue.size) {
            showPlayingTrackControl()
            showSeekBar()
        }

        hidePlaybackQueueControl()

        recyclerViewAdapter!!.viewMode = 0
        recyclerViewAdapter!!.notifyDataSetChanged()

        updateFloatingActionButtonVisibility()

        updateResumeButtonVisibility()
    }

    fun showPlaybackQueueControl() {
        val pc: LinearLayout = findViewById(R.id.playback_control)
        pc.setVisibility(View.VISIBLE)

        updatePlayingTrackPlayPauseButton(isMediaPlaying())
    }

    fun hidePlaybackQueueControl() {
        val pc: LinearLayout = findViewById(R.id.playback_control)
        pc.setVisibility(View.GONE)
    }

    fun showSeekBar() {
        folderViewSeekBar = findViewById(R.id.playing_track_seek_bar) as SeekBar
        folderViewSeekBar?.visibility = View.VISIBLE;
    }

    fun hideSeekBar() {
        folderViewSeekBar = findViewById(R.id.playing_track_seek_bar) as SeekBar
        folderViewSeekBar?.visibility = View.GONE;
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
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show()

                if (selectedTabIndex < 0) {
                    Log.w(TAG, "fab.oC.cPI<0")
                    return
                }

                if (mediaPlayerTabs.size <= selectedTabIndex) {
                    Log.w(TAG, "mPTs.sz<=cPI")
                    return
                }

                val filesAndDirs = mediaPlayerTabs.get(selectedTabIndex).fileSystemNavigator
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
                    Log.d(TAG, "fab.oC !rTP")
                    return
                }
                val player = mediaPlayerTabs.get(selectedTabIndex).playbackQueue
                player.clearQueue()
                player.addToQueue(mediaFiles)
                player.resetSavedPlaybackPosition()
                val started = player.playCurrentlyPointedMediaInQueue()
                if(started) {
                    onMediaStartedOnScreen()

                    switchToPlaylistView()
                }

                // Update background colors of queued tracks
                // This is done in switchToPlaylistView() so commented out.
                //recyclerViewAdapter.notifyDataSetChanged();

                //if( mediaPlayer != null && mediaPlayer.isPlaying() ) {
                //    mediaPlayer.stop();
                //}
            }
        })

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

    fun initResumeFab() {
        val fab = findViewById(R.id.resume) as FloatingActionButton
        fab.setOnClickListener(object : View.OnClickListener {

            override fun onClick(view: View) {
                resume()
            }
        })

        fab.setVisibility(View.GONE)
    }

    fun initPlayingTrackControl() {
        val btn = findViewById(R.id.playing_track_play_pause_button) as ImageButton?
        btn?.setOnClickListener(
                object : View.OnClickListener {
                    override fun onClick(view: View) {
                        Log.d(TAG, "play/pause")

                        togglePlayPauseState()

                        // The following is done in onAudioPlay callback
                        // called from the service
                        //updatePlayingTrackPlayPauseButton(playing)
                    }
                }
        )
    }

    fun initSeekBar() {

        folderViewSeekBar = findViewById(R.id.playing_track_seek_bar) as SeekBar?
        folderViewSeekBar?.max = 0
        folderViewSeekBar?.progress = 0
        folderViewSeekBar!!.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        //Log.d(TAG, "sb.oPC: " + seekBar.getProgress())
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                        Log.d(TAG, "sb.onStartTT: " + seekBar.getProgress())
                        isTrackingSeekBar = true
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        Log.d(TAG, "sb.onStopTT: " + seekBar.getProgress())
                        val service = BackgroundAudioService.instance
                        val mediaPlayer = service?.mediaPlayer
                        if (mediaPlayer != null) {
                            mediaPlayer.seekTo(seekBar.getProgress())
                        } else {
                            Log.d(TAG, "oSTT !mP")
                        }

                        isTrackingSeekBar = false
                    }
                })

        // Do not show the seek bar when the app is started.
        hideSeekBar()

    }

    fun updatePlayingTrackPlayPauseButton(playing: Boolean?) {
        val btn = findViewById(R.id.playing_track_play_pause_button) as ImageButton?
        if(playing == null) {
            btn?.setImageResource(R.drawable.stop)
        } else if(playing) {
            btn?.setImageResource(R.drawable.pause)
        } else {
            btn?.setImageResource(R.drawable.play)
        }
    }

    fun initPlaylistViewMediaControlButtons() {
        val btn = findViewById(R.id.play_pause_button) as ImageButton
        if (btn == null) {
            Log.d(TAG, "!play/pause btn")
            return
        }

        btn.setOnClickListener(
                object : View.OnClickListener {
                    override fun onClick(view: View) {
                        Log.d(TAG, "btn pressed (play/pause)")

                        togglePlayPauseState()
                    }
                })

        val prevTrackBtn = findViewById(R.id.prev_track) as ImageButton
        if (prevTrackBtn == null) {
            Log.d(TAG, "!pTB")
            return
        }

        prevTrackBtn.setOnClickListener(
                object : View.OnClickListener {
                    override fun onClick(view: View) {
                        Log.d(TAG, "btn:p (prev)")
                        val self = view as ImageButton

                        if (selectedTabIndex < 0 || mediaPlayerTabs.size <= selectedTabIndex) {
                            return
                        }

                        onMediaStartRequestedOnScreen()
                        val tab = mediaPlayerTabs.get(selectedTabIndex)
                        tab.playbackQueue.playPrevTrack()
                        onMediaStartedOnScreen()
                    }
                })

        val nextTrackBtn = findViewById(R.id.next_track) as ImageButton
        if (nextTrackBtn == null) {
            Log.d(TAG, "!nTB")
            return
        }

        nextTrackBtn.setOnClickListener(
                object : View.OnClickListener {
                    override fun onClick(view: View) {
                        Log.d(TAG, "btn:p (next)")
                        val self = view as ImageButton

                        if (selectedTabIndex < 0 || mediaPlayerTabs.size <= selectedTabIndex) {
                            return
                        }

                        onMediaStartRequestedOnScreen()
                        val tab = mediaPlayerTabs.get(selectedTabIndex)
                        tab.playbackQueue.playNextTrack()
                        onMediaStartedOnScreen()
                    }
                })
    }

    fun updatePlaylistViewPlayPauseButton(playing: Boolean?) {
        val btn = findViewById(R.id.play_pause_button) as ImageButton?
        if(playing == null) {
            btn?.setImageResource(R.drawable.stop)
        } else if(playing) {
            btn?.setImageResource(R.drawable.pause)
        } else {
            btn?.setImageResource(R.drawable.play)
        }
    }

    fun isMediaPlaying(): Boolean? {
        return Playback.mediaPlayer?.isPlaying()
    }

    fun getCurrentlyPlayingTab(): MediaPlayerTab? {
        Log.d(TAG, "gCPT " + selectedTabIndex)
        if(0 <= currentlyPlayedQueueIndex
                && currentlyPlayedQueueIndex < mediaPlayerTabs.size) {
            return mediaPlayerTabs.get(currentlyPlayedQueueIndex)
        } else {
            Log.d(TAG, "gCPT r!")
            return null
        }
    }

    /**
     *
     * @return true if the playback successfully (started play or resume),
     *         false if it failed to start
     */
    fun togglePlayPauseState(): Boolean? {
        Log.d(TAG, "tPPS")
        val mediaPlayer = Playback.mediaPlayer
        // Both isPlaying() and pause() can throw IllegalStateException.

        try {
            if(mediaPlayer == null) {
                Log.d(TAG, "tPPS !mp")
                return null
            }

            if (mediaPlayer.isPlaying()) {
                Log.d(TAG, "btn pausing")
                backgroundAudioService?.pause()
                return false
            } else {
                // Start/resume the playback
                Log.d(TAG, "btn playing/resuming")
                val started = backgroundAudioService?.play()
                return started
            }
        } catch (ise: IllegalStateException) {
            Log.d(TAG, "ise caught")
        } catch (e: Exception) {
            Log.d(TAG, "Exception caught")
        }

        return null
    }

    fun showPlayingTrackControl() {
        Log.d(TAG, "sPTB")

        // Update the track title and duration
        updatePlayingTrackControlPanel(this.mediaPlayer)
        updateSeekbarProgressAndTime()

        // Show the playing track info and control (pause/resume)
        val ptc: LinearLayout = findViewById(R.id.playing_track_control)
        ptc.setVisibility(View.VISIBLE)

        // Update button state (playing or paused)
        updatePlayingTrackPlayPauseButton(isMediaPlaying())
    }

    fun hidePlayingTrackControl() {
        Log.d(TAG, "hPTC")

        // Show the playing track info and control (pause/resume)
        val ptc: LinearLayout = findViewById(R.id.playing_track_control)
        ptc.setVisibility(View.GONE)
    }

    fun openMediaInfoPopup(mediaFile: File) {
        if(mediaInfoPopup == null) {
            Log.w(TAG,"!mIP")
            return
        }

        mediaInfoPopup?.open(this, mediaFile)
    }

    companion object {

        private val TAG = "MainActivity"

        //private ServiceConnection serviceConnection = null;

        private var tabIdCounter = 1000

        private val APPLICATION_STATE_FILE_NAME = "appstate"
    }
}
