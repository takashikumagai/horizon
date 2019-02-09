package space.nyanko.nyankoapplication;

import android.os.Environment;
//import android.os.storage.StorageVolume;
//import android.os.storage.StorageManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StorageSelector extends AbstractDirectoryNavigator {

    private static final String TAG = "StorageSelector";

    ArrayList<File> deviceStorageDirectories = new ArrayList<File>();
    ArrayList<String> names = new ArrayList<String>();

    AbstractDirectoryNavigator childDirectoryNavigator;

    StorageSelector() {
        // This should be unneessary as StorageHelper's getStorages() contains the primary storage.
//        File f = new File("/storage/emulated/0");
//        if(f == null) {
//            Log.d(TAG,"!/s/e/0");
//        } else {
//            deviceStorageDirectories.add(f);
//            names.add("Internal Storage");
//        }

        // This also returns the primary internal storage and as such also unnecessary.
        //File dir = Environment.getExternalStorageDirectory();
        //if(dir == null) {
        //    Log.d(TAG,"!getESD");
        //} else {
        //    deviceStorageDirectories.add(dir);
        //}

        List<StorageHelper.StorageVolume> vols = StorageHelper.getStorages(true);
        Log.d(TAG,  Integer.toString(vols.size()) + " storage volumes found.");
        for (StorageHelper.StorageVolume vol: vols) {
            Log.d(TAG, "Device name: '" + vol.device + "'");
            Log.d(TAG, "  Path: " + vol.file.getPath());
            Log.d(TAG, "  Type: " + vol.getType());

            deviceStorageDirectories.add(vol.file);
        }

        // StorageManager API is newer but require API level 24 so we'll go
        // with the home-made one above.
        //StorageManager mgr = new StorageManager;
        //List<StorageVolume> vols = mgr.getStorageVolumes();
        //Log.d(TAG,  Integer.toString(vols.size()) + " storage volumes found.");
        //for(StorageVolume vol: vols) {
        //    String desc = vol.getDescription();
        //    Log.d(TAG, desc);
        //}
    }

    @Override
    ArrayList<File> getCurrentDirectoryEntries() {
        return deviceStorageDirectories;
    }

    @Override
    public String getCurrentDirectoryName() {
        return "[Device Storage Directories]";
    }

    @Override
    public AbstractDirectoryNavigator moveToChild(int pos) {

        if( pos < 0 || deviceStorageDirectories.size() <= pos ) {
            Log.d(TAG,"invalid pos");
            return null;
        }

        String path = deviceStorageDirectories.get(pos).getPath();
        if(path == null) {
            Log.d(TAG,"!path");
            return null;
        }

        DirectoryNavigator navigator = new DirectoryNavigator(path);
        if(navigator == null) {
            Log.d(TAG, "!n");
            return null;
        }

        if(!navigator.isValid()) {
            Log.d(TAG, "!n.iV");
            return null;
        }

        childDirectoryNavigator = navigator;

        return childDirectoryNavigator;
    }

    @Override
    public AbstractDirectoryNavigator moveToParent() {

        if(childDirectoryNavigator == null) {
            Log.d(TAG, "!cDN");
            // device storage is not open yet;
            // tell the caller that the curosr is pointing to the root
            // of the navigator system
            return null;
        } else {

            AbstractDirectoryNavigator ret = childDirectoryNavigator.moveToParent();
            if(ret != null) {
                return ret;
            } else {
                // Storage selector will be the current navigator at FileSystemNavigator
                childDirectoryNavigator = null;
                return this;
            }
        }
    }

    @Override
    public boolean isAtLeastOneMediaFilePresent() {
        return false;
    }
}
