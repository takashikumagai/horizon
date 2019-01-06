package space.nyanko.nyankoapplication;

import android.util.Log;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

import java.io.IOException;
import java.util.ArrayList;

/**
 * \brief Stors the playback information
 *
 * - TODO: rename this to MyMediaPlayer
 * - Maps to each tab.
 * - There are as many playback instanes as the number of tabs.
 *
 */
public class Playback {

    private static final String TAG = "Playback";

    private static MediaPlayer mediaPlayer = new MediaPlayer();

    private static int initialized = 0;

    private static Playback currentPlayer;

    // Play position in milliseconds
    private int position = 0;

    /**
     * \brief Stores pathnames of playable media files (e.g. .mp3, .wav)
     *
     * Files are played starting from the first element to the last element
     */
    public ArrayList<String> mediaFilePathQueue = new ArrayList<String>();

    // 0 stopped
    // 1 playing
    // 2 paused
    int state = 0;

    public Playback() {
        if(initialized == 0) {
            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {

                    Log.d(TAG, "playback complete");

                    if (currentPlayer == null) {
                        Log.d(TAG, "No player set");
                        return;
                    }

                    //currentPlayer.startNextInQueue();
                }
            });
            initialized = 1;
        }
    }

    public static MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void clearQueue() {
        Log.d(TAG, "clearing queue");
        mediaFilePathQueue.clear();
    }

    public void addToQueue(String mediaFilePath) {
        Log.d(TAG, "addToQueue");
        if(500 <= mediaFilePathQueue.size()) {
            Log.w(TAG, "Too many songs/vids in the queue (they reached 500).");
        }
        mediaFilePathQueue.add(mediaFilePath);
    }

    public void startFirstInQueue() {
        if(mediaFilePathQueue.size() == 0) {
            Log.d(TAG, "queue is empty");
            return;
        }

        if(mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        try {

            Log.d(TAG, "setDataSource: " + mediaFilePathQueue.get(0));
            mediaPlayer.setDataSource(mediaFilePathQueue.get(0));
            Log.d(TAG, "prepare");
            mediaPlayer.prepare();
            Log.d(TAG, "starting");
            mediaPlayer.start();
        } catch(IOException ioe) {
            Log.e(TAG, "caught an IO exception: " + ioe.toString());
        } catch(Exception e) {
            Log.e(TAG, "caught an exception: " + e.toString());
        }
    }

    /**
     * \brief Pop front first and then play the one 2nd from the head
     *
     * Note that this function removes the first eleemnt from the queue
     */
    public void startNextInQueue() {
        if(mediaFilePathQueue.size() == 0) {
            return;
        }

        // Pop the first eleemnt
        mediaFilePathQueue.remove(0);

        startFirstInQueue();
    }
}
