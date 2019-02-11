package space.nyanko.nyankoapplication;

import android.os.Bundle;
import android.app.Service;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.service.media.MediaBrowserService.BrowserRoot;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.AudioManager;
import android.text.TextUtils;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.MediaBrowserCompat;
import androidx.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.MediaMetadataCompat.Builder;
import android.view.KeyEvent;
import android.util.Log;
import java.io.File;
import java.util.List;
import java.util.Set;


/**
 * @brief This class manages MediaSession and MediaPlayer, plus a few more
 *        core componets of the media player app.
 *
 *
 *
 *
 */
public class BackgroundAudioService extends MediaBrowserServiceCompat {

    private static final String TAG = "BackgroundAudioService";

    public static final String ACTION_PLAY = "play";
    public static final String ACTION_PLAY_PAUSE = "playPause";
    public static final String ACTION_PREV_TRACK = "prevTrack";
    public static final String ACTION_NEXT_TRACK = "nextTrack";
    public static final String ACTION_STOP = "stop";


    private static BackgroundAudioService self;

    private MediaPlayer mediaPlayer;

    private MediaSessionCompat mediaSession = null;

    private PlaybackStateCompat playbackState;

    private Builder metadataBuilder;

    /**
     * Might not need this one perhaps
     */
    private Playback currentlySelected;

    /**
     * Points to the currently played playback queue
     */
    private Playback currentlyPlayed;

    private MyMediaSessionCallback mediaSessionCallback;

    private BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"oR");
            //if( mMediaPlayer != null && mMediaPlayer.isPlaying() ) {
            //    mMediaPlayer.pause();
            //}
        }
    };

    class MyMediaSessionCallback extends MediaSessionCompat.Callback implements AudioManager.OnAudioFocusChangeListener{

        @Override
        public void onPrepare() {
            Log.d(TAG,"oP");
        }
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            super.onMediaButtonEvent(mediaButtonEvent);
            Log.d(TAG,"oMBE: " + mediaButtonEvent.toString());
            Bundle extras = mediaButtonEvent.getExtras();
            Log.d(TAG,"extras: " + ((extras!=null) ? extras.toString() : "null"));
            if(extras==null) {
                Log.d(TAG,"!ex");
            } else {
                for(String key : extras.keySet()) {
                    Object value = extras.get(key);
                    Log.d(TAG, String.format("%s %s (%s)",
                            key, value.toString(), value.getClass().getName()));
                }
                Object obj = extras.get("android.intent.extra.KEY_EVENT");
                if(obj==null) {
                    Log.d(TAG,"!obj");
                } else {
                    KeyEvent keyEvent = (KeyEvent)obj;
                    if(keyEvent==null) {
                        Log.d(TAG,"!kE");
                    } else {
                        int action = keyEvent.getAction();
                        int code = keyEvent.getKeyCode();
                        Log.d(TAG,String.format("kE a: %d, kc: %d",action,code));
                        if(action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                            Log.d(TAG,"mp.pause");
                            mediaPlayer.pause();
                        } else if(action == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_MEDIA_PLAY) {
                            Log.d(TAG,"mp.start");
                            mediaPlayer.start();
                        }
                    }
                }
            }
            Set<String> cats = mediaButtonEvent.getCategories();
            Log.d(TAG,"categories: " + ((cats!=null) ? cats.toString() : "null"));
            Log.d(TAG,"data uri: " + mediaButtonEvent.getDataString() );
            return super.onMediaButtonEvent(mediaButtonEvent);
        }
        @Override
        public void onPause() {
            Log.d(TAG,"onPause");
            super.onPause();
        }
        @Override
        public void onPlay() {
            Log.d(TAG,"onPlay");
            super.onPlay();
            if( !retrievedAudioFocus() ) {
                return;
            }
        }
        @Override
        public void onStop() {
            Log.d(TAG,"onStop");
            super.onStop();
        }
        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG,"oPFMI: " + mediaId);
            super.onPlayFromMediaId(mediaId,extras);
        }

        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG,"oAFC: " + focusChange);

            //MediaPlayer mediaPlayer = Playback.getMediaPlayer();

            if(mediaPlayer == null) {
                Log.d(TAG,"!mP");
                return;
            }

            switch( focusChange ) {
                case AudioManager.AUDIOFOCUS_LOSS: {
                    // a loss of audio focus of unknown duration
                    Log.d(TAG,"oAFC AL");

                    if( mediaPlayer.isPlaying() ) {
                        mediaPlayer.stop();
                    }
                    break;
                }
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: {
                    Log.d(TAG,"oAFC ALT");
                    mediaPlayer.pause();
                    break;
                }
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                    Log.d(TAG,"oAFC ALTCD");
                    if( mediaPlayer != null ) {
                        mediaPlayer.setVolume(0.3f, 0.3f);
                    }
                    break;
                }
                case AudioManager.AUDIOFOCUS_GAIN: {
                    Log.d(TAG,"oAFC AG");
                    if( mediaPlayer != null ) {
//                        if( !mediaPlayer.isPlaying() ) {
//                            mediaPlayer.start();
//                        }
//                        mediaPlayer.setVolume(1.0f, 1.0f);
                    }
                    break;
                }
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT: {
                    Log.d(TAG,"oAFC AGT");
                    // Not sure what to do with this one...
                    break;
                }
            }
        }
    }

    public BackgroundAudioService() {
        super();
        Log.d(TAG,"ctor");
    }

    public static BackgroundAudioService getInstance() {
        return self;
    }

    public void setCurrentlySelectedPlaybackQueue(Playback selectedPlaybackQueue) {
        currentlySelected = selectedPlaybackQueue;
    }

    public void setCurrentlyPlayedPlaybackQueue(Playback playedPlaybackQueue) {
        currentlyPlayed = playedPlaybackQueue;
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
        }

        return null;
    }

    // Not important for general audio service, required for class
    @Override
    public void onLoadChildren(String parentId, Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"oC");

        if(self == null) {
            self = this;
        } else {
            Log.e(TAG,"More than one BAService has been created.");
        }

        initMediaPlayer();
        initMediaSession();
        initNoisyReceiver();

        if(mediaSession != null) {
            // Init notification/lock screen controls
            LockScreenMediaControl.createNotificationChannel(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"oSC: " + flags + ", " + startId);
        KeyEvent keyEvent = MediaButtonReceiver.handleIntent(mediaSession, intent);

        if(keyEvent == null) {
            Log.d(TAG,"!kE");
            // custom actions we defined for the notification
            handleIntent(intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");

        unregisterReceiver(noisyReceiver);

        self = null;
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG,"oTR");
        super.onTaskRemoved(rootIntent);

        hideMediaControls();

        // Stop the service
        // In the debug phase, it's convenient to have the service destroyed every time
        // so that we can get a fresh start every time we run the app.
        // Whether to provide this as an option for the production is a matter of debate.
        stopSelf();
    }

    public void initMediaPlayer() {

        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

                Log.d(TAG, "playback complete");

                if (currentlyPlayed == null) {
                    Log.d(TAG, "No playback queue set");
                    return;
                }

                currentlyPlayed.onCompletion(mp);

                // Since the call above usually starts the new track,
                // we need to update the informaiton (title, album, etc.)
                // on the notification
                updateMediaControls();
                showMediaControls();
            }
        });

        Playback.setMediaPlayer(mediaPlayer);
    }

    public void initMediaSession() {
        ComponentName mediaButtonReceiver
                = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mediaSession = new MediaSessionCompat(
                getApplicationContext(), "mySessionTag", mediaButtonReceiver, null);

        mediaSessionCallback = new MyMediaSessionCallback();
        MediaSessionCompat.Callback msc = mediaSessionCallback;
        mediaSession.setCallback(msc);
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
               |MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

        // Register a media button receiver
        Log.d(TAG,"mBI");
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mediaSession.setMediaButtonReceiver(pendingIntent);

        mediaSession.setActive(true);

        metadataBuilder = new android.support.v4.media.MediaMetadataCompat.Builder();
    }

    private void initNoisyReceiver() {
        Log.d(TAG,"iNR");
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(noisyReceiver, filter);
    }

    /**
     * @biref Attempts to retrieve audio focus
     *
     *
     * @return true if the audio focus was granted
     */
    public boolean retrievedAudioFocus() {
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        int result = audioManager.requestAudioFocus(mediaSessionCallback,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_GAIN;
    }

    public void setMetadata() {

        if(mediaSession == null) {
            Log.d(TAG,"sMd !mP");
            return;
        }
        if(metadataBuilder == null) {
            Log.d(TAG,"sMd !mB");
            return;
        }
        // Update metadata
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "album_title");
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, "track_title");
        MediaMetadataCompat metadata = metadataBuilder.build();
        mediaSession.setMetadata(metadata);
    }

    private String getMediaTitle(String mediaFilePath) {
        if(mediaFilePath == null) {
            return "";
        }

        File f = new File(mediaFilePath);
        String title = HorizonUtils.getMediaFileTitle(f);
        if(title != null && !title.equals("")) {
            return title;
        } else {
            return f.getName();
        }

    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if(action == ACTION_PLAY_PAUSE) {
            if(mediaPlayer == null || currentlyPlayed == null) {
                return;
            }
            if(mediaPlayer.isPlaying()) {
                currentlyPlayed.pause();
            } else {
                currentlyPlayed.resume();
            }
            // Play if track is paused, or pause if it is playing
            Log.d(TAG,"a:p/p");
        } else if(action == ACTION_PREV_TRACK) {
            Log.d(TAG,"a:prev");
            if(currentlyPlayed != null) {
                boolean result = currentlyPlayed.playPrevTrack();
                updateMediaControls();
                showMediaControls();
            } else {
                Log.d(TAG,"a:prev cP!")
            }
        } else if(action == ACTION_NEXT_TRACK) {
            Log.d(TAG,"a:next");
            if(currentlyPlayed != null) {
                boolean result = currentlyPlayed.playNextTrack();
                updateMediaControls();
                showMediaControls();
            } else {
                Log.d(TAG,"a:next cP!")
            }
        }
    }

    public void updateMediaControls() {
        if(currentlyPlayed != null) {
            String mediaName = currentlyPlayed.getCurrentlyPlayedMediaName();
            String title = getMediaTitle(mediaName);
            LockScreenMediaControl.init(this, mediaSession, title);
        }
    }

    public void showMediaControls() {
        Log.d(TAG,"sMCs");

        LockScreenMediaControl.show(this);
    }

    public void hideMediaControls() {
        Log.d(TAG,"hMCs");

        LockScreenMediaControl.hide(this);
    }
}
