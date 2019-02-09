package space.nyanko.nyankoapplication;

import android.util.Log;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

public class FileSystemNavigator implements Serializable {

    private static final String TAG = "FileSystemNavigator";

    private AbstractDirectoryNavigator navigator;

    private AbstractDirectoryNavigator currentNavigator;

    static AbstractDirectoryNavigator createNavigator(int type, String path) {
        if(type == 1)
            return new StorageSelector();
        else if(type == 2)
            return new DirectoryNavigator(path);
        else {
            Log.d(TAG, "invalid type");
            return null;
        }
    }

    int initialize() {

        navigator = new StorageSelector();
        currentNavigator = navigator;
        return 0;
    }

    int initialize(String path) {

        return -1;

//        try() {
//
//        } catch(Exception e) {
//
//        }
//        if( dirs == null ) {
//            return -1;
//        }
    }

    AbstractDirectoryNavigator getNavigator() { return navigator; }

    AbstractDirectoryNavigator getCurrentNavigator() { return currentNavigator; }

    String getCurrentDirectoryName() {
        if(currentNavigator != null) {
            return currentNavigator.getCurrentDirectoryName();
        } else {
            return "";
        }
    }

    ArrayList<File> getCurrentDirectoryEntries() {

        if(currentNavigator != null) {
            return currentNavigator.getCurrentDirectoryEntries();
        } else {
            return null;
        }
    }

    int getNumCurrentDirectoryEntries() {

        if(currentNavigator != null) {
            ArrayList<File> entries = currentNavigator.getCurrentDirectoryEntries();
            if(entries != null) {
                return entries.size();
            }
        }

        return 0;
    }

//    String getCurrentPath() {
//        return navigator.getCurrentDi();
//    }

    int moveToParent() {

        // Make a call on the root navigator because currentNavigator alone does not
        // know about the parent
        if(navigator == null) {
            Log.d(TAG,"mTP: !navigator");
            return -1;
        }

        AbstractDirectoryNavigator parent = navigator.moveToParent();
        if(parent == null) {
            // Pointing to the root
            // Returning without setting null to currentNavigator
            return -1;
        }

        currentNavigator = parent;

        return 0;
    }

    int moveToChild(int pos) {
        if(currentNavigator != null) {
            AbstractDirectoryNavigator next = currentNavigator.moveToChild(pos);
            if(next != null) {
                currentNavigator = next;
            }
        } else {
            Log.d(TAG,"!cN");
            return -1;
        }
        return 0;
    }

//    int moveToChild(String childName) {
//
//        if(currentNavigator != null) {
//            AbstractDirectoryNavigator next = currentNavigator.moveToChild();
//            if(next != null) {
//                currentNavigator = next;
//            }
//        } else {
//            return -1;
//        }
//        return 0;
//    }

//    static int changeDirectory(String path) {
//
//        while(true) {
//            int ret = navigator.changeDirectory(path);
//            if(ret == 0) {
//                break;
//            } else {
//                navigator = createNavigator(ret,path);
//            }
//        }
//
//        return 0;
//    }
}
