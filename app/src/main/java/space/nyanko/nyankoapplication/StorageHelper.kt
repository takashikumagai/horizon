package space.nyanko.nyankoapplication

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.StringTokenizer

import android.os.Environment
import android.util.Log

/**
 * \brief Enumerates storage locations
 *
 * Source: https://stackoverflow.com/questions/22219312/android-open-external-storage-directorysdcard-for-storing-file
 */
object StorageHelper {

    private val TAG = "StorageHelper"

    private val STORAGES_ROOT: String

    private val AVOIDED_DEVICES = arrayOf("rootfs", "tmpfs", "dvpts", "proc", "sysfs", "none")

    private val AVOIDED_DIRECTORIES = arrayOf("obb", "asec")

    private val DISALLOWED_FILESYSTEMS = arrayOf("tmpfs", "rootfs", "romfs", "devpts", "sysfs", "proc", "cgroup", "debugfs")

    init {
        val primaryStoragePath = Environment.getExternalStorageDirectory()
                .getAbsolutePath()
        val index = primaryStoragePath.indexOf(File.separatorChar, 1)
        if (index != -1) {
            STORAGES_ROOT = primaryStoragePath.substring(0, index + 1)
        } else {
            STORAGES_ROOT = File.separator
        }
    }

    /**
     * Returns a list of mounted [StorageVolume]s Returned list always
     * includes a [StorageVolume] for
     * [Environment.getExternalStorageDirectory]
     *
     * @param includeUsb
     * if true, will include USB storages
     * @return list of mounted [StorageVolume]s
     */
    fun getStorages(includeUsb: Boolean): List<StorageVolume> {
        val deviceVolumeMap = HashMap<String, ArrayList<StorageVolume>>()

        // this approach considers that all storages are mounted in the same non-root directory
        if (!STORAGES_ROOT.equals(File.separator)) {
            var reader: BufferedReader? = null
            try {
                reader = BufferedReader(FileReader("/proc/mounts"))
                if(reader == null) {
                    Log.d(TAG,"gSs !reader")
                    return ArrayList<StorageVolume>()
                }

                var line: String?
                while (true) {//((line = reader!!.readLine()) != null) {
                    line = reader.readLine()

                    if(line == null) {
                        break
                    }

                    // Log.d(TAG, line);
                    val tokens = StringTokenizer(line, " ")
                    //String[] tokens = line.split(" ")

                    val device = tokens.nextToken()
                    // skipped devices that are not sdcard for sure
                    if (arrayContains(AVOIDED_DEVICES, device)) {
                        continue
                    }

                    // should be mounted in the same directory to which
                    // the primary external storage was mounted
                    val path = tokens.nextToken()
                    if (!path.startsWith(STORAGES_ROOT)) {
                        continue
                    }

                    // skip directories that indicate tha volume is not a storage volume
                    if (pathContainsDir(path, AVOIDED_DIRECTORIES)) {
                        continue
                    }

                    val fileSystem = tokens.nextToken()
                    // don't add ones with non-supported filesystems
                    if (arrayContains(DISALLOWED_FILESYSTEMS, fileSystem)) {
                        continue
                    }

                    val file = File(path)
                    // skip volumes that are not accessible
                    if (!file.canRead() || !file.canExecute()) {
                        continue
                    }

                    var volumes = deviceVolumeMap.get(device)
                    if (volumes == null) {
                        volumes = ArrayList<StorageVolume>(3)
                        deviceVolumeMap.put(device, volumes)
                    }

                    var volume = StorageVolume(device, file, fileSystem)
                    val flags = StringTokenizer(tokens.nextToken(), ",")
                    while (flags.hasMoreTokens()) {
                        val token = flags.nextToken()
                        if (token.equals("rw")) {
                            volume.isReadOnly = false
                            break
                        } else if (token.equals("ro")) {
                            volume.isReadOnly = true
                            break
                        }
                    }
                    volumes.add(volume)
                }

            } catch (ex: IOException) {
                ex.printStackTrace()
            } finally {
                if (reader != null) {
                    try {
                        reader.close()
                    } catch (ex: IOException) {
                        // ignored
                    }

                }
            }
        }

        // remove volumes that are the same devices
        var primaryStorageIncluded = false
        val externalStorage = Environment.getExternalStorageDirectory()
        val volumeList = ArrayList<StorageVolume>()
        for (entry in deviceVolumeMap) {//.entrySet()) {
            val volumes = entry.value
            if (volumes.size === 1) {
                // go ahead and add
                val v = volumes.get(0)
                val isPrimaryStorage = v.file!!.equals(externalStorage)
                primaryStorageIncluded = primaryStorageIncluded or isPrimaryStorage
                setTypeAndAdd(volumeList, v, includeUsb, isPrimaryStorage)
                continue
            }
            val volumesLength = volumes.size
            for (i in 0 until volumesLength) {
                val v = volumes.get(i)
                if (v.file!!.equals(externalStorage)) {
                    primaryStorageIncluded = true
                    // add as external storage and continue
                    setTypeAndAdd(volumeList, v, includeUsb, true)
                    break
                }
                // if that was the last one and it's not the default external
                // storage then add it as is
                if (i == volumesLength - 1) {
                    setTypeAndAdd(volumeList, v, includeUsb, false)
                }
            }
        }
        // add primary storage if it was not found
        if (!primaryStorageIncluded) {
            val defaultExternalStorage = StorageVolume("", externalStorage, "UNKNOWN")
            defaultExternalStorage.isEmulated = Environment.isExternalStorageEmulated()
            defaultExternalStorage.type = if (defaultExternalStorage.isEmulated)
                StorageVolume.Type.INTERNAL
            else
                StorageVolume.Type.EXTERNAL
            defaultExternalStorage.isRemovable = Environment.isExternalStorageRemovable()
            defaultExternalStorage.isReadOnly = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)
            volumeList.add(0, defaultExternalStorage)
        }
        return volumeList
    }

    /**
     * Sets [StorageVolume.Type], removable and emulated flags and adds to
     * volumeList
     *
     * @param volumeList
     * List to add volume to
     * @param v
     * volume to add to list
     * @param includeUsb
     * if false, volume with type [StorageVolume.Type.USB] will
     * not be added
     * @param asFirstItem
     * if true, adds the volume at the beginning of the volumeList
     */
    private fun setTypeAndAdd(volumeList: ArrayList<StorageVolume>,
                              v: StorageVolume,
                              includeUsb: Boolean,
                              asFirstItem: Boolean) {
        val type = resolveType(v)
        if (includeUsb || type != StorageVolume.Type.USB) {
            v.type = type
            if (v.file!!.equals(Environment.getExternalStorageDirectory())) {
                v.isRemovable = Environment.isExternalStorageRemovable()
            } else {
                v.isRemovable = type != StorageVolume.Type.INTERNAL
            }
            v.isEmulated = type == StorageVolume.Type.INTERNAL
            if (asFirstItem) {
                volumeList.add(0, v)
            } else {
                volumeList.add(v)
            }
        }
    }

    /**
     * Resolved [StorageVolume] type
     *
     * @param v
     * [StorageVolume] to resolve type for
     * @return [StorageVolume] type
     */
    private fun resolveType(v: StorageVolume): StorageVolume.Type {
        return if (v.file!!.equals(Environment.getExternalStorageDirectory()) && Environment.isExternalStorageEmulated()) {
            StorageVolume.Type.INTERNAL
        } else if (containsIgnoreCase(v.file.getAbsolutePath(), "usb")) {
            StorageVolume.Type.USB
        } else {
            StorageVolume.Type.EXTERNAL
        }
    }

    /**
     * Checks whether the array contains object
     *
     * @param array
     * Array to check
     * @param object
     * Object to find
     * @return true, if the given array contains the object
     */
    private fun <T> arrayContains(array: Array<T>, `object`: T): Boolean {
        for (item in array) {
            if ((item != null) && item.equals(`object`)) {
                return true
            }
        }
        return false
    }

    /**
     * Checks whether the path contains one of the directories
     *
     * For example, if path is /one/two, it returns true input is "one" or
     * "two". Will return false if the input is one of "one/two", "/one" or
     * "/two"
     *
     * @param path
     * path to check for a directory
     * @param dirs
     * directories to find
     * @return true, if the path contains one of the directories
     */
    private fun pathContainsDir(path: String, dirs: Array<String>): Boolean {
        val tokens = StringTokenizer(path, File.separator)
        while (tokens.hasMoreElements()) {
            val next = tokens.nextToken()
            for (dir in dirs) {
                if (next.equals(dir)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks ifString contains a search String irrespective of case, handling.
     * Case-insensitivity is defined as by
     * [String.equalsIgnoreCase].
     *
     * @param str
     * the String to check, may be null
     * @param searchStr
     * the String to find, may be null
     * @return true if the String contains the search String irrespective of
     * case or false if not or `null` string input
     */
    fun containsIgnoreCase(str: String?, searchStr: String?): Boolean {
        if (str == null || searchStr == null) {
            return false
        }
        val len = searchStr.length
        val max = str.length - len
        for (i in 0..max) {
            if (str.regionMatches(i, searchStr, 0, len, true)) {
                return true
            }
        }
        return false
    }

    /**
     * Represents storage volume information
     */
    class StorageVolume internal constructor(
            /**
             * Device name
             */
            val device: String,
            /**
             * Points to mount point of this device
             */
            val file: File?,
            /**
             * File system of this device
             */
            val fileSystem: String) {

        /**
         * if true, the storage is mounted as read-only
         */
        /**
         * Returns true if this storage is mounted as read-only
         *
         * @return true if this storage is mounted as read-only
         */
        var isReadOnly: Boolean = false

        /**
         * If true, the storage is removable
         */
        /**
         * Returns true if this storage is removable
         *
         * @return true if this storage is removable
         */
        var isRemovable: Boolean = false

        /**
         * If true, the storage is emulated
         */
        /**
         * Returns true if this storage is emulated
         *
         * @return true if this storage is emulated
         */
        var isEmulated: Boolean = false

        /**
         * Type of this storage
         */
        /**
         * Returns type of this storage
         *
         * @return Type of this storage
         */
        var type: Type? = null

        /**
         * Represents [StorageVolume] type
         */
        enum class Type {
            /**
             * Device built-in internal storage. Probably points to
             * [Environment.getExternalStorageDirectory]
             */
            INTERNAL,

            /**
             * External storage. Probably removable, if no other
             * [StorageVolume] of type [.INTERNAL] is returned by
             * [StorageHelper.getStorages], this might be
             * pointing to [Environment.getExternalStorageDirectory]
             */
            EXTERNAL,

            /**
             * Removable usb storage
             */
            USB
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + if (file == null) 0 else file.hashCode()
            return result
        }

        /**
         * Returns true if the other object is StorageHelper and it's
         * [.file] matches this one's
         *
         * @see Object.equals
         */
        override fun equals(obj: Any?): Boolean {
            if (obj === this) {
                return true
            }
            if (obj == null) {
                return false
            }
            //if (getClass() !== obj!!.getClass()) {
            if (this.javaClass !== obj.javaClass) {
                return false
            }
            val other = obj as StorageVolume?
            return if (file == null) {
                other!!.file == null
            } else file.equals(other!!.file)
        }

        override fun toString(): String {
            return (file!!.getAbsolutePath() + (if (isReadOnly) " ro " else " rw ") + type + (if (isRemovable) " R " else "")
                    + (if (isEmulated) " E " else "") + fileSystem)
        }
    }
}
