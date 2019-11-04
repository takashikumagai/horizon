package space.nyanko.nyankoapplication

import android.util.Log

import java.io.File
import java.util.ArrayList

class StorageSelector internal constructor() : AbstractDirectoryNavigator() {

    //@get:Override
    internal override val currentDirectoryEntries = ArrayList<File>()

    //private ArrayList<String> names = new ArrayList<String>();

    private var childDirectoryNavigator: AbstractDirectoryNavigator? = null

    override val currentDirectoryName: String
        get() = "[Device Storage Directories]"

    override val isAtLeastOneMediaFilePresent: Boolean
        get() = false

    init {
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

        val vols = StorageHelper.getStorages(true)
        Log.d(TAG, Integer.toString(vols.size) + " storage volumes found.")
        for (vol in vols) {
            Log.d(TAG, "Device name: '" + vol.device + "'")
            Log.d(TAG, "  Path: " + vol.file!!.getPath())
            //Log.d(TAG, "  Type: " + vol.getType())

            currentDirectoryEntries.add(vol.file)
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

    override fun moveToChild(pos: Int): AbstractDirectoryNavigator? {

        if (pos < 0 || currentDirectoryEntries.size <= pos) {
            Log.d(TAG, "invalid pos")
            return null
        }

        val path = currentDirectoryEntries.get(pos).getPath()
        if (path == null) {
            Log.d(TAG, "!path")
            return null
        }

        val navigator = DirectoryNavigator(path)
        if (navigator == null) {
            Log.d(TAG, "!n")
            return null
        }

        if (!navigator.isValid()) {
            Log.d(TAG, "!n.iV")
            return null
        }

        childDirectoryNavigator = navigator

        return childDirectoryNavigator
    }

    override fun moveToParent(): AbstractDirectoryNavigator? {

        if (childDirectoryNavigator == null) {
            Log.d(TAG, "!cDN")
            // device storage is not open yet;
            // tell the caller that the curosr is pointing to the root
            // of the navigator system
            return null
        } else {

            val ret = childDirectoryNavigator!!.moveToParent()
            if (ret != null) {
                return ret
            } else {
                // Storage selector will be the current navigator at FileSystemNavigator
                childDirectoryNavigator = null
                return this
            }
        }
    }

    override fun cloneNavigator(): AbstractDirectoryNavigator? {
        var clone = StorageSelector()

        clone.childDirectoryNavigator = childDirectoryNavigator?.cloneNavigator()

        return clone
    }

    override fun getCurrentNavigator(): AbstractDirectoryNavigator? {
        if(childDirectoryNavigator != null) {
            return childDirectoryNavigator
        } else {
            return this
        }
    }

    companion object {

        private val TAG = "StorageSelector"
    }
}
