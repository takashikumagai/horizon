package space.nyanko.nyankoapplication;

import android.util.Log;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

import java.io.File;
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

    /**
     * @brief Index to the currently played or paused media file in queue
     *
     */
    private int pointedMediaIndex = -1;

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

                    // Play the next media in queue
                    if(pointedMediaIndex < mediaFilePathQueue.size() - 1) {
                        pointedMediaIndex += 1;
                        currentPlayer.startCurrentlyPointedMediaInQueue();
                    }
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
        pointedMediaIndex = -1;
    }

    public void startCurrentlyPointedMediaInQueue() {

        if(pointedMediaIndex < 0) {
            Log.d(TAG, "pMI<0");
            return;
        }

        if(mediaFilePathQueue.size() <= pointedMediaIndex) {
            Log.d(TAG, "q.size<=pMI");
            return;
        }

        if(mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        String mediaFilepath = "";
        try {
            mediaFilepath = mediaFilePathQueue.get(pointedMediaIndex);
            Log.d(TAG, "resetting");
            mediaPlayer.reset();
            Log.d(TAG, "setDataSource: " + mediaFilepath);
            mediaPlayer.setDataSource(mediaFilepath);
            Log.d(TAG, "prepare");
            mediaPlayer.prepare();
            Log.d(TAG, "starting");
            mediaPlayer.start();
        } catch(IOException ioe) {
            Log.e(TAG, "caught an IO exception: " + ioe.toString() + " File: " + mediaFilepath);
        } catch(Exception e) {
            Log.e(TAG, "caught an exception: " + e.toString() + " File: " + mediaFilepath);
        }
    }

    public void addToQueue(String mediaFilePath) {
        Log.d(TAG, "addToQueue");
        if(500 <= mediaFilePathQueue.size()) {
            Log.w(TAG, "Too many songs/vids in the queue (they reached 500).");
        }
        mediaFilePathQueue.add(mediaFilePath);

        if(mediaFilePathQueue.size() == 1) {
            // Queue just got one media file; point to it
            pointedMediaIndex = 0;
        }
    }

    public void addToQueue(ArrayList<File> mediaFiles) {
        for(File file : mediaFiles) {
            mediaFilePathQueue.add(file.getPath());
        }

        if(0 < mediaFiles.size() && pointedMediaIndex == -1) {
            pointedMediaIndex = 0;
        }
    }
//    public void startFirstInQueue() {
//        if(mediaFilePathQueue.size() == 0) {
//            Log.d(TAG, "queue is empty");
//            return;
//        }
//
//        startCurrentlyPointedMediaInQueue();
//    }

    /**
     * \brief Pop front first and then play the one 2nd from the head
     *
     * Note that this function removes the first eleemnt from the queue
     */
//    public void startNextInQueue() {
//        if(mediaFilePathQueue.size() == 0) {
//            return;
//        }
//
//        // Pop the first eleemnt
//        mediaFilePathQueue.remove(0);
//
//        startFirstInQueue();
//    }

    public boolean isPointed(String filePath) {
        if(pointedMediaIndex < 0 || mediaFilePathQueue.size() <= pointedMediaIndex) {
            return false;
        }

        if(mediaFilePathQueue.get(pointedMediaIndex).equals(filePath)) {
            return true;
        }

        return false;
    }

    public boolean isInQueue(String filePath) {
        for(String pathInQueue : mediaFilePathQueue) {
            if(pathInQueue.equals(filePath)) {
                return true;
            }
        }

        return false;
    }

    public static void setCurrentPlayer(Playback currentPlayer) {
        Playback.currentPlayer = currentPlayer;
    }
}
