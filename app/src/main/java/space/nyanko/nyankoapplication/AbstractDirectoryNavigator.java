package space.nyanko.nyankoapplication;

import java.io.File;
import java.util.ArrayList;

public abstract class AbstractDirectoryNavigator {

    abstract ArrayList<File> getCurrentDirectoryEntries();

    abstract String getCurrentDirectoryName();

    //abstract String getCurrentDirectoryPath();

    abstract AbstractDirectoryNavigator moveToParent();

    abstract AbstractDirectoryNavigator moveToChild(int pos);

    abstract boolean isAtLeastOneMediaFilePresent();

    //abstract int changeDirectory(String childDirPath);
}
