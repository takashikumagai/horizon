package space.nyanko.nyankoapplication

import android.util.Log

import java.io.File
import java.io.Serializable
import java.util.ArrayList

class FileSystemNavigator : Serializable {

    internal var navigator: AbstractDirectoryNavigator? = null
        private set

    internal var currentNavigator: AbstractDirectoryNavigator? = null
        private set

    internal val currentDirectoryName: String
        get() = if (currentNavigator != null) {
            currentNavigator!!.currentDirectoryName
        } else {
            ""
        }

    internal val currentDirectoryEntries: ArrayList<File>?
        get() = if (currentNavigator != null) {
            currentNavigator!!.currentDirectoryEntries
        } else {
            null
        }

    internal val numCurrentDirectoryEntries: Int
        get() {

            if (currentNavigator != null) {
                val entries = currentNavigator!!.currentDirectoryEntries
                if (entries != null) {
                    return entries!!.size
                }
            }

            return 0
        }

    internal fun initialize(): Int {

        navigator = StorageSelector()
        currentNavigator = navigator
        return 0
    }

    internal fun initialize(path: String): Int {

        return -1

        //        try() {
        //
        //        } catch(Exception e) {
        //
        //        }
        //        if( dirs == null ) {
        //            return -1;
        //        }
    }

    //    String getCurrentPath() {
    //        return navigator.getCurrentDi();
    //    }

    internal fun moveToParent(): Int {

        // Make a call on the root navigator because currentNavigator alone does not
        // know about the parent
        if (navigator == null) {
            Log.d(TAG, "mTP: !navigator")
            return -1
        }

        val parent = navigator!!.moveToParent()
                ?: // Pointing to the root
                // Returning without setting null to currentNavigator
                return -1

        currentNavigator = parent

        return 0
    }

    internal fun moveToChild(pos: Int): Int {
        if (currentNavigator != null) {
            val next = currentNavigator!!.moveToChild(pos)
            if (next != null) {
                currentNavigator = next
            }
        } else {
            Log.d(TAG, "!cN")
            return -1
        }
        return 0
    }

    companion object {

        private val TAG = "FileSystemNavigator"

        internal fun createNavigator(type: Int, path: String): AbstractDirectoryNavigator? {
            if (type == 1)
                return StorageSelector()
            else if (type == 2)
                return DirectoryNavigator(path)
            else {
                Log.d(TAG, "invalid type")
                return null
            }
        }
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
