package space.nyanko.nyankoapplication;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class HorizonUtils {

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
}
