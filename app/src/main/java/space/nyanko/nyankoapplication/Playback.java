package space.nyanko.nyankoapplication;

import android.util.Log;
import android.media.MediaPlayer;

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

    private static Playback currentPlayer;

    /**
     * Set by the service
     */
    private static MediaPlayer mediaPlayer;

    // 0 stopped
    // 1 playing
    // 2 paused
    int state = 0;

    /**
     * @brief Index to the currently played or paused media file in queue
     *
     */
    public int pointedMediaIndex = -1;

    // Play position in milliseconds
    public int position = 0;

    /**
     * \brief Stores pathnames of playable media files (e.g. .mp3, .wav)
     *
     * Files are played starting from the first element to the last element
     */
    public ArrayList<String> mediaFilePathQueue = new ArrayList<String>();

    public Playback() {
    }

    public static MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public static void setMediaPlayer(MediaPlayer mediaPlayer) {
        Playback.mediaPlayer = mediaPlayer;
    }

    public void clearQueue() {
        Log.d(TAG, "clearing queue");
        mediaFilePathQueue.clear();
        pointedMediaIndex = -1;
    }

    public void startCurrentlyPointedMediaInQueue() {

        MediaPlayer mediaPlayer = getMediaPlayer();
        if(mediaPlayer == null) {
            Log.w(TAG,"!mP");
            return;
        }

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

        // Notify the service once we start playing tracks in a queue
        // - Or to be more exact, we give the service a reference to the instance
        //   storing the playback information (queue, currently played track, etc)
        // - Note that this particular instance of Playback survivies the activity destruction
        //   and recreation
        BackgroundAudioService service = BackgroundAudioService.getInstance();
        if(service == null) {
            Log.w(TAG,"!service");
        } else {
            service.setCurrentPlaybackQueue(currentPlayer);
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


    // Play the next media in queue

    public void onCompletion(MediaPlayer mp) {

        if(currentPlayer==null) {
            Log.d(TAG,"!cP");
            return;
        }

        if(pointedMediaIndex < mediaFilePathQueue.size() - 1) {
            pointedMediaIndex += 1;
            startCurrentlyPointedMediaInQueue();
        }
    }

    public static void setCurrentPlayer(Playback currentPlayer) {
        Playback.currentPlayer = currentPlayer;

        // Notify the service
        //BackgroundAudioService.setCurrentPlaybackQueue(currentPlayer);
    }
}
