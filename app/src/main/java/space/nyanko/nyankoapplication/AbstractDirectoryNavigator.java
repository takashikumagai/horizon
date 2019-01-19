package space.nyanko.nyankoapplication;

import java.io.File;
import java.util.ArrayList;

public abstract class AbstractDirectoryNavigator {

    /**
     * @brief Returns a list containing 2 types of objects
     * 1. Media file(s) Horizon is capable of playing
     * 2. Directory/directories
     *
     * Directories might or might not have media files in them.
     *
     * @return
     */
    abstract ArrayList<File> getCurrentDirectoryEntries();

    abstract String getCurrentDirectoryName();

    //abstract String getCurrentDirectoryPath();

    abstract AbstractDirectoryNavigator moveToParent();

    abstract AbstractDirectoryNavigator moveToChild(int pos);

    abstract boolean isAtLeastOneMediaFilePresent();

    //abstract int changeDirectory(String childDirPath);
}
