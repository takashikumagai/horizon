package space.nyanko.nyankoapplication;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class DirectoryNavigator {

    private String currentDirectory = "/storage/emulated/0";

    private ArrayList<File> currentDirectoryEntries = new ArrayList<>();

    DirectoryNavigator() {
        updateCurrentDirectoryEntries();
    }

    private void updateCurrentDirectoryEntries() {
        File f = new File(this.currentDirectory);
        ArrayList<File> entries = new ArrayList<File>(Arrays.asList(f.listFiles()));
        currentDirectoryEntries = entries;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(String currentDirectory) {
        this.currentDirectory = currentDirectory;

        updateCurrentDirectoryEntries();
    }

    public ArrayList<File> getCurrentDirectoryEntries() {
        return currentDirectoryEntries;
    }

    public void moveToParentDirectory() {
        File f = new File(currentDirectory);
        String p = f.getParent();
        currentDirectory = p;

        updateCurrentDirectoryEntries();
    }
}
