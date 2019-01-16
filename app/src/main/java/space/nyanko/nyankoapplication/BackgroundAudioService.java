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
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.view.KeyEvent;
import android.util.Log;
import java.util.List;
import java.util.Set;


public class BackgroundAudioService extends MediaBrowserServiceCompat {

    private static final String TAG = "BackgroundAudioService";

    private static BackgroundAudioService self;

    private MediaPlayer mediaPlayer;

    private MediaSessionCompat mediaSession = null;

    private Playback currentPlayback;

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
            if( !successfullyRetrievedAudioFocus() ) {
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
                    if( mediaPlayer.isPlaying() ) {
                        mediaPlayer.stop();
                    }
                    break;
                }
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: {
                    mediaPlayer.pause();
                    break;
                }
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                    if( mediaPlayer != null ) {
                        mediaPlayer.setVolume(0.3f, 0.3f);
                    }
                    break;
                }
                case AudioManager.AUDIOFOCUS_GAIN: {
                    if( mediaPlayer != null ) {
                        if( !mediaPlayer.isPlaying() ) {
                            mediaPlayer.start();
                        }
                        mediaPlayer.setVolume(1.0f, 1.0f);
                    }
                    break;
                }
            }
        }

        private boolean successfullyRetrievedAudioFocus() {
            AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

            int result = audioManager.requestAudioFocus(this,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            return result == AudioManager.AUDIOFOCUS_GAIN;
        }
    }

    public BackgroundAudioService() {
        super();
        Log.d(TAG,"ctor");
    }

    public static BackgroundAudioService getInstance() {
        return self;
    }

    public void setCurrentPlaybackQueue(Playback playbackQueue) {
        currentPlayback = playbackQueue;
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"oSC: " + flags + ", " + startId);
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"oD");
        self = null;
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG,"oTR");
        super.onTaskRemoved(rootIntent);

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

                if (currentPlayback == null) {
                    Log.d(TAG, "No playback queue set");
                    return;
                }

                currentPlayback.onCompletion(mp);
            }
        });

        Playback.setMediaPlayer(mediaPlayer);
    }

    public void initMediaSession() {
        ComponentName mediaButtonReceiver
                = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mediaSession = new MediaSessionCompat(
                getApplicationContext(), "mySessionTag", mediaButtonReceiver, null);

        MediaSessionCompat.Callback msc = new MyMediaSessionCallback();
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
    }

    private void initNoisyReceiver() {
        Log.d(TAG,"iNR");
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(noisyReceiver, filter);
    }
}