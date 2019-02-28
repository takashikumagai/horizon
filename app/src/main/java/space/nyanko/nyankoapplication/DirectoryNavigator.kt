package space.nyanko.nyankoapplication

import android.util.Log

import java.io.File
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections

class DirectoryNavigator internal constructor(private val baseDeviceDirectory: String) : AbstractDirectoryNavigator() {

    private var currentDirectory: File? = null

    override var currentDirectoryEntries: ArrayList<File> = ArrayList()

    //val isValid: Boolean
    //    get() = currentDirectory != null
    fun isValid(): Boolean {
        return if (currentDirectory != null) true else false
    }

    override val currentDirectoryName: String
        get() = currentDirectory!!.getName()

    override val isAtLeastOneMediaFilePresent: Boolean
        get() {
            Log.d(TAG, "iALOMFP: " + currentDirectoryEntries!!.size)
            for (entry in currentDirectoryEntries!!) {
                if (HorizonUtils.isMediaFile(entry.getName())) {
                    return true
                }
            }

            return false
        }

    init {

        currentDirectory = File(baseDeviceDirectory)

        if (currentDirectory == null) {
            Log.d(TAG, "ctor: cD!")
        }

        updateCurrentDirectoryEntries()
    }

    private fun updateCurrentDirectoryEntries() {
        val f = currentDirectory// new File(this.currentDirectory);
        if (f == null) {
            currentDirectoryEntries!!.clear()
            Log.d(TAG, "f==null")
            return
        }

        val fileList = f!!.listFiles()
        if (fileList == null) {
            Log.d(TAG, "fileList==null")
            return
        }

        var names = ""
        for (file in fileList!!) {
            names += file.getName() + ", "
        }
        Log.d(TAG, "names (" + fileList!!.size + "): " + names)

        var entries = fileList

        val filtered = ArrayList<File>()
        val displayOnlyMediaFiles = true
        if (displayOnlyMediaFiles) {
            // Filter file list. Note that after this 'entries' contains directories
            // and media files.


            // Gave up on using lambda because of this error:
            // Error: Call requires API level 24 (current min is 19): java.util.ArrayList#removeIf [NewApi]
            //entries.removeIf(e -> !e.isDirectory() && !);
            for (fileOrDir in entries) {
                if (fileOrDir.isDirectory() || HorizonUtils.isMediaFile(fileOrDir.getPath())) {
                    filtered.add(fileOrDir)
                }
            }
        }

        // Sort the files and directories inside the current directory
        // Note that each element of entries can be a file or a directory.
        Collections.sort(filtered)

        currentDirectoryEntries = filtered
    }

    //    @Override
    //    public int changeDirectory(String currentDirectory) {
    //        this.currentDirectory = currentDirectory;
    //
    //        updateCurrentDirectoryEntries();
    //
    //        return 0;
    //    }

    override fun moveToChild(pos: Int): AbstractDirectoryNavigator? {

        if (currentDirectory == null) {
            Log.d(TAG, "!cD")
            return null
        }

        if (currentDirectoryEntries == null) {
            Log.d(TAG, "!cDE")
            return null
        }

        if (pos < 0 || currentDirectoryEntries!!.size <= pos) {
            Log.d(TAG, "invalid pos")
            return null
        }

        currentDirectory = currentDirectoryEntries!!.get(pos)

        //String childPath = Paths.get( currentDirectory.path(), child.)
        updateCurrentDirectoryEntries()

        return this
    }


    override fun moveToParent(): AbstractDirectoryNavigator? {

        if (currentDirectory == null) {
            Log.w(TAG, "mTP: !cD")
            return null // Something is wrong; return to the storage seletor
        }

        if (currentDirectory!!.getPath().equals(baseDeviceDirectory)) {
            // Currently at the root and can't go up the tree any more
            // -> Return null to indicate that control has be handed back
            // to the storage selector (no error)
            return null
        }

        val p = currentDirectory!!.getParent()
        if (p == null) {
            Log.w(TAG, "!getParent")
            return null // Something is wrong; return to the storage selector
        }

        val to = File(p)
        if (to == null) {
            Log.w(TAG, "!to")
            return null // Something is wrong; return to the storage selector
        }

        currentDirectory = to

        updateCurrentDirectoryEntries()

        return this
    }

    companion object {

        private val TAG = "DirectoryNavigator"
    }
}
