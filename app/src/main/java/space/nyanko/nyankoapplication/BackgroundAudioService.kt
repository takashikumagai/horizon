package space.nyanko.nyankoapplication

import android.os.Bundle
import android.app.Service
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ComponentName
import android.service.media.MediaBrowserService.BrowserRoot
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.AudioManager
import android.text.TextUtils
import androidx.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.Builder
import android.view.KeyEvent
import android.util.Log
import java.io.File


/**
 * @brief This class manages MediaSession and MediaPlayer, plus a few more
 * core componets of the media player app.
 */
class BackgroundAudioService : MediaBrowserServiceCompat() {

    var mediaPlayer: MediaPlayer? = null
        private set

    private var mediaSession: MediaSessionCompat? = null

    private val playbackState: PlaybackStateCompat? = null

    private var metadataBuilder: Builder? = null

    /**
     * Might not need this one perhaps
     */
    private var currentlySelected: Playback? = null

    /**
     * Points to the currently played playback queue
     */
    private var currentlyPlayed: Playback? = null

    private var mediaSessionCallback: MyMediaSessionCallback? = null

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "oR")
            //if( mMediaPlayer != null && mMediaPlayer.isPlaying() ) {
            //    mMediaPlayer.pause();
            //}
        }
    }

    internal inner class MyMediaSessionCallback : MediaSessionCompat.Callback(), AudioManager.OnAudioFocusChangeListener {

        override fun onPrepare() {
            Log.d(TAG, "oP")
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            super.onMediaButtonEvent(mediaButtonEvent)
            Log.d(TAG, "oMBE: " + mediaButtonEvent.toString())
            val extras = mediaButtonEvent.getExtras()
            Log.d(TAG, "extras: " + if (extras != null) extras!!.toString() else "null")
            if (extras == null) {
                Log.d(TAG, "!ex")
            } else {
                for (key in extras!!.keySet()) {
                    val value = extras!!.get(key)
                    Log.d(TAG, String.format("%s %s (%s)",
                            key, value.toString(), value.javaClass.getName()))
                }
                val obj = extras!!.get("android.intent.extra.KEY_EVENT")
                if (obj == null) {
                    Log.d(TAG, "!obj")
                } else {
                    val keyEvent = obj as KeyEvent
                    if (keyEvent == null) {
                        Log.d(TAG, "!kE")
                    } else {
                        val action = keyEvent!!.getAction()
                        val code = keyEvent!!.getKeyCode()
                        Log.d(TAG, String.format("kE a: %d, kc: %d", action, code))
                        if (action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                            Log.d(TAG, "mp.pause")
                            mediaPlayer!!.pause()
                        } else if (action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_MEDIA_PLAY) {
                            Log.d(TAG, "mp.start")
                            mediaPlayer!!.start()
                        }
                    }
                }
            }
            val cats = mediaButtonEvent.getCategories()
            Log.d(TAG, "categories: " + if (cats != null) cats!!.toString() else "null")
            Log.d(TAG, "data uri: " + mediaButtonEvent.getDataString())
            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        override fun onPause() {
            Log.d(TAG, "onPause")
            super.onPause()
        }

        override fun onPlay() {
            Log.d(TAG, "onPlay")
            super.onPlay()
            if (!retrievedAudioFocus()) {
                return
            }
        }

        override fun onStop() {
            Log.d(TAG, "onStop")
            super.onStop()
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle) {
            Log.d(TAG, "oPFMI: $mediaId")
            super.onPlayFromMediaId(mediaId, extras)
        }

        override fun onAudioFocusChange(focusChange: Int) {
            Log.d(TAG, "oAFC: $focusChange")

            //MediaPlayer mediaPlayer = Playback.getMediaPlayer();

            if (mediaPlayer == null) {
                Log.d(TAG, "!mP")
                return
            }

            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    // a loss of audio focus of unknown duration
                    Log.d(TAG, "oAFC AL")

                    if (mediaPlayer!!.isPlaying()) {
                        mediaPlayer!!.stop()
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    Log.d(TAG, "oAFC ALT")
                    mediaPlayer!!.pause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    Log.d(TAG, "oAFC ALTCD")
                    if (mediaPlayer != null) {
                        mediaPlayer!!.setVolume(0.3f, 0.3f)
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    Log.d(TAG, "oAFC AG")
                    if (mediaPlayer != null) {
                        //                        if( !mediaPlayer.isPlaying() ) {
                        //                            mediaPlayer.start();
                        //                        }
                        //                        mediaPlayer.setVolume(1.0f, 1.0f);
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                    Log.d(TAG, "oAFC AGT")
                }// Not sure what to do with this one...
            }
        }
    }

    init {
        Log.d(TAG, "ctor")
    }

    fun setCurrentlySelectedPlaybackQueue(selectedPlaybackQueue: Playback) {
        currentlySelected = selectedPlaybackQueue
    }

    fun setCurrentlyPlayedPlaybackQueue(playedPlaybackQueue: Playback) {
        currentlyPlayed = playedPlaybackQueue
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return if (TextUtils.equals(clientPackageName, getPackageName())) {
            BrowserRoot(getString(R.string.app_name), null)
        } else null

    }

    // Not important for general audio service, required for class
    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(null)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "oC")

        if (instance == null) {
            instance = this
        } else {
            Log.e(TAG, "More than one BAService has been created.")
        }

        initMediaPlayer()
        initMediaSession()
        initNoisyReceiver()

        if (mediaSession != null) {
            // Init notification/lock screen controls
            LockScreenMediaControl.createNotificationChannel(this)
        }

        // Make this a foreground service

        // We need a notification so just make an empty one for now.
        LockScreenMediaControl.init(this, mediaSession, "")

        startForeground(
                LockScreenMediaControl.NOTIFICATION_ID,
                LockScreenMediaControl.notification
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "oSC: $flags, $startId")
        val keyEvent = MediaButtonReceiver.handleIntent(mediaSession, intent)

        if (keyEvent == null) {
            Log.d(TAG, "!kE")
            // custom actions we defined for the notification
            handleIntent(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")

        unregisterReceiver(noisyReceiver)

        instance = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        Log.d(TAG, "oTR")
        super.onTaskRemoved(rootIntent)

        // Do NOT hide the media controls (notification)
        //hideMediaControls();

        // Stop the service
        // In the debug phase, it's convenient to have the service destroyed every time
        // so that we can get a fresh start every time we run the app.
        // Whether to provide this as an option for the production is a matter of debate.
        //stopSelf();
    }

    fun initMediaPlayer() {

        mediaPlayer = MediaPlayer()

        mediaPlayer!!.setOnCompletionListener(object : OnCompletionListener {

            override fun onCompletion(mp: MediaPlayer) {

                Log.d(TAG, "playback complete")

                if (currentlyPlayed == null) {
                    Log.d(TAG, "No playback queue set")
                    return
                }

                currentlyPlayed!!.onCompletion(mp)

                // Since the call above usually starts the new track,
                // we need to update the informaiton (title, album, etc.)
                // on the notification
                updateMediaControls()
                showMediaControls()
            }
        })

        Playback.mediaPlayer = mediaPlayer
    }

    fun initMediaSession() {
        val mediaButtonReceiver = ComponentName(getApplicationContext(), MediaButtonReceiver::class.java)
        mediaSession = MediaSessionCompat(
                getApplicationContext(), "mySessionTag", mediaButtonReceiver, null)

        mediaSessionCallback = MyMediaSessionCallback()
        val msc = mediaSessionCallback
        mediaSession!!.setCallback(msc)
        mediaSession!!.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        // Register a media button receiver
        Log.d(TAG, "mBI")
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0)
        mediaSession!!.setMediaButtonReceiver(pendingIntent)

        mediaSession!!.setActive(true)

        metadataBuilder = android.support.v4.media.MediaMetadataCompat.Builder()
    }

    private fun initNoisyReceiver() {
        Log.d(TAG, "iNR")
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)
    }

    /**
     * @biref Attempts to retrieve audio focus
     *
     *
     * @return true if the audio focus was granted
     */
    fun retrievedAudioFocus(): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val result = audioManager.requestAudioFocus(mediaSessionCallback,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        return result == AudioManager.AUDIOFOCUS_GAIN
    }

    fun setMetadata() {

        if (mediaSession == null) {
            Log.d(TAG, "sMd !mP")
            return
        }
        if (metadataBuilder == null) {
            Log.d(TAG, "sMd !mB")
            return
        }
        // Update metadata
        metadataBuilder!!.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "album_title")
        metadataBuilder!!.putString(MediaMetadataCompat.METADATA_KEY_TITLE, "track_title")
        val metadata = metadataBuilder!!.build()
        mediaSession!!.setMetadata(metadata)
    }

    private fun getMediaTitle(mediaFilePath: String?): String {
        if (mediaFilePath == null) {
            return ""
        }

        val f = File(mediaFilePath)
        val title = HorizonUtils.getMediaFileTitle(f)
        return if (title != null && !title!!.equals("")) {
            title
        } else {
            f.getName()
        }

    }

    private fun handleIntent(intent: Intent) {
        val action = intent.getAction()
        if (action === ACTION_PLAY_PAUSE) {
            if (mediaPlayer == null || currentlyPlayed == null) {
                return
            }
            if (mediaPlayer!!.isPlaying()) {
                currentlyPlayed!!.pause()
            } else {
                currentlyPlayed!!.resume()
            }
            // Play if track is paused, or pause if it is playing
            Log.d(TAG, "a:p/p")
        } else if (action === ACTION_PREV_TRACK) {
            Log.d(TAG, "a:prev")
            if (currentlyPlayed != null) {
                val result = currentlyPlayed!!.playPrevTrack()
                updateMediaControls()
                showMediaControls()
            } else {
                Log.d(TAG, "a:prev cP!")
            }
        } else if (action === ACTION_NEXT_TRACK) {
            Log.d(TAG, "a:next")
            if (currentlyPlayed != null) {
                val result = currentlyPlayed!!.playNextTrack()
                updateMediaControls()
                showMediaControls()
            } else {
                Log.d(TAG, "a:next cP!")
            }
        }
    }

    fun updateMediaControls() {
        if (currentlyPlayed != null) {
            val mediaName = currentlyPlayed!!.currentlyPlayedMediaPath
            val title = getMediaTitle(mediaName)
            LockScreenMediaControl.init(this, mediaSession, title)
        }
    }

    fun showMediaControls() {
        Log.d(TAG, "sMCs")

        LockScreenMediaControl.show(this)
    }

    fun hideMediaControls() {
        Log.d(TAG, "hMCs")

        LockScreenMediaControl.hide(this)
    }

    companion object {

        private val TAG = "BackgroundAudioService"

        val ACTION_PLAY = "play"
        val ACTION_PLAY_PAUSE = "playPause"
        val ACTION_PREV_TRACK = "prevTrack"
        val ACTION_NEXT_TRACK = "nextTrack"
        val ACTION_STOP = "stop"


        var instance: BackgroundAudioService? = null
            private set
    }
}
