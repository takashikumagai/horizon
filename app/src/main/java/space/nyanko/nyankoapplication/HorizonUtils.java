package space.nyanko.nyankoapplication;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import android.media.MediaMetadataRetriever;

public class HorizonUtils {

    private static final String TAG = "MainActivity";

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
}
