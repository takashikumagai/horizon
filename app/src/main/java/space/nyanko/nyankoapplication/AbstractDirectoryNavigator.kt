package space.nyanko.nyankoapplication

import java.io.File
import java.io.Serializable
import java.util.ArrayList

abstract class AbstractDirectoryNavigator : Serializable {

    /**
     * @brief Returns a list containing 2 types of objects
     * 1. Media file(s) Horizon is capable of playing
     * 2. Directory/directories
     *
     * Directories might or might not have media files in them.
     *
     * @return
     */
    internal abstract val currentDirectoryEntries: ArrayList<File>

    internal abstract val currentDirectoryName: String

    internal abstract val isAtLeastOneMediaFilePresent: Boolean

    //abstract String getCurrentDirectoryPath();

    internal abstract fun moveToParent(): AbstractDirectoryNavigator

    internal abstract fun moveToChild(pos: Int): AbstractDirectoryNavigator

    //abstract int changeDirectory(String childDirPath);
}
