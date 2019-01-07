package space.nyanko.nyankoapplication;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v1Tag;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

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
        if(ext.equals("mp3")) {
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

    public static String getMediaFileTitle(File f) {
        Log.d(TAG,"gMFT");

        if(f==null) {
            return "";
        }

        try {
            String ext = getExtension(f.getName());
            if(ext.equals("mp3")) {
                Mp3File mp3file = new Mp3File(f.getPath());
                if(mp3file.hasId3v1Tag()) {
                    ID3v1 id3v1Tag = mp3file.getId3v1Tag();
                    return id3v1Tag.getTitle();
                } else if(mp3file.hasId3v2Tag()) {
                    ID3v2 id3v2Tag = mp3file.getId3v2Tag();
                    return id3v2Tag.getTitle();
                } else {
                    Log.d(TAG,"not id3 tag found");
                    return "";
                }
            } else {
                Log.d(TAG,"ext!=mp3");
                return "";
            }
        } catch(IOException ioe) {
            Log.e(TAG, "ioe msg: " + ioe.getMessage() + " File: " + f.getPath());
        } catch(UnsupportedTagException ute) {
            Log.e(TAG,"UnsupportedTagException");
        } catch(InvalidDataException ide) {
            Log.e(TAG,"InvalidDataException");
        }

        return "";
    }
}
