package space.nyanko.nyankoapplication;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import android.media.MediaMetadataRetriever;

public class HorizonUtils {

    private static final String TAG = "HorizonUtils";

    public static String getExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i+1);
        } else {
            return "";
        }
    }

    public static boolean isMediaFile(String fileName) {
        String ext = getExtension(fileName);
        if(ext.equals("mp3") || ext.equals("ogg") || ext.equals("flac") || ext.equals("wav")) {
            return true;
        } else {
            return false;
        }
    }

    public static ArrayList<File> pickMediaFiles(ArrayList<File> filesAndDirs) {
        ArrayList<File> mediaFiles = new ArrayList<File>();
        for(File entry : filesAndDirs) {
            if(isMediaFile(entry.getName())) {
                mediaFiles.add(entry);
            }
        }

        return mediaFiles;
    }

    /**
     * @brief Returns the title metadata
     *
     * @param f
     * @return Title or null if the media tag was not found.
     */
    public static String getMediaFileTitle(File f) {
        Log.d(TAG,"gMFT");

        if(f==null) {
            return "";
        }

        if(isMediaFile(f.getName())) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(f.getPath());
            String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if(title == null) {
                Log.d(TAG,"!t");
                return null;
            }

            return title;
        }

        Log.d(TAG,"!iMF");
        return "";
    }

    /**
     * @brief Retrieves metadata tags from a media file, e.g. an mp3 file.
     *
     * @param f
     * @return a hashmap where each entry is an integer representing a tag and the tag value,
     * or null if arg is invalid
     */
    public static HashMap<Integer,String> getMediaFileMetaTags(File f, int[] tags) {
        Log.d(TAG,"gMFMTs");

        HashMap<Integer,String> tagMaps = new HashMap<Integer,String>();

        if(f==null) {
            return null;
        }

        if(isMediaFile(f.getName())) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(f.getPath());
            for(int tag : tags) {
                String value = mmr.extractMetadata(tag);
                tagMaps.put(tag,value);
            }
        } else {
            Log.d(TAG,"not a media file");
        }
        return tagMaps;
    }

    public static String millisecondsToHhmmss(long milliseconds) {
        return
        String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(milliseconds),
                TimeUnit.MILLISECONDS.toMinutes(milliseconds) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)));
    }
}
