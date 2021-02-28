package space.nyanko.nyankoapplication

import android.util.Log

import java.io.File
import java.util.ArrayList

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
            Log.d(TAG, "iALOMFP: " + currentDirectoryEntries.size)
            for (entry in currentDirectoryEntries) {
                if (HorizonUtils.isMediaFile(entry.getName())) {
                    return true
                }
            }

            return false
        }

    init {

        currentDirectory = File(baseDeviceDirectory)
        Log.d(TAG, "init cD: $currentDirectory")

        if (currentDirectory == null) {
            Log.d(TAG, "ctor: cD!")
        }

        updateCurrentDirectoryEntries()
    }

    private fun updateCurrentDirectoryEntries() {
        Log.d(TAG, "uCDE: ${currentDirectory?.path}")
        val f = currentDirectory// new File(this.currentDirectory);
        if (f == null) {
            currentDirectoryEntries.clear()
            Log.d(TAG, "f==null")
            return
        }

        // Tried to do this with f.walkTopDown().maxDepth(1) but the results
        // were not ordered in directory first plus they include the argument
        // directory so decided not to bethoer

        val entries = f.listFiles()
        if(entries == null) {
            // The app failed to read files in the directory.
            // This happens on some devices when android:requestLegacyExternalStorage
            // is not set to true in AndroidManifest.xml
            Log.d(TAG, "uCDE !entries")
            return
        }

        val displayOnlyMediaFiles = true
        var dirsAndFilesToShow: MutableList<File> =
                if (displayOnlyMediaFiles) pickDirsAndMediaFiles(entries)
                else entries.toCollection(ArrayList())// ArrayList<File>(Arrays.asList(entries))

        val fileList = sortDirsFirst(dirsAndFilesToShow)
        if (fileList == null) {
            Log.d(TAG, "fileList==null")
            return
        }

        var names = ""
        for (file in fileList) {
            names += file.getName() + ", "
        }
        Log.d(TAG, "names (" + fileList.size + "): " + names)

        currentDirectoryEntries = ArrayList<File>(fileList)
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

        if (pos < 0 || currentDirectoryEntries.size <= pos) {
            Log.d(TAG, "invalid pos")
            return null
        }

        currentDirectory = currentDirectoryEntries.get(pos)

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

    fun pickDirsAndMediaFiles(dirsAndFiles: Array<File>): MutableList<File> {
        val filtered = ArrayList<File>()
        // Filter file list. Note that after this 'entries' contains directories
        // and media files.


        // Gave up on using lambda because of this error:
        // Error: Call requires API level 24 (current min is 19): java.util.ArrayList#removeIf [NewApi]
        //entries.removeIf(e -> !e.isDirectory() && !);
        for (fileOrDir in dirsAndFiles) {
            if (fileOrDir.isDirectory() || HorizonUtils.isMediaFile(fileOrDir.getPath())) {
                filtered.add(fileOrDir)
            }
        }

        return filtered
    }

    // Sort the files and directories inside the current directory
    // Note that each element of entries can be a file or a directory.
    fun sortDirsFirst(entries: MutableList<File>): List<File> {
        entries.sortWith(object: Comparator<File>{
            override fun compare(f1: File, f2: File): Int {
                if(f1.isDirectory()) {
                    if(f2.isDirectory()) {
                        // Both f1 and f2 are directories
                        return if (f1.toString() < f2.toString()) -1 else 1
                    } else {
                        return -1
                    }
                } else {
                    if(f2.isDirectory()) {
                        return 1
                    } else {
                        // Both f1 and f2 are files
                        return if (f1.toString() < f2.toString()) -1 else 1
                    }
                }
            }
        })

        return entries
    }

    override fun cloneNavigator(): AbstractDirectoryNavigator? {
        var clone = DirectoryNavigator(currentDirectory?.path ?: "")
        return clone
    }

    override fun getCurrentNavigator(): AbstractDirectoryNavigator? {
        return null
    }

    companion object {

        private const val TAG = "DirectoryNavigator"
    }
}
