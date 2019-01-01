package space.nyanko.nyankoapplication;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class DirectoryNavigator extends AbstractDirectoryNavigator {

    private static final String TAG = "DirectoryNavigator";

    private File currentDirectory;// = "/storage/emulated/0";

    private ArrayList<File> currentDirectoryEntries = new ArrayList<>();

    private String baseDeviceDirectory;

    DirectoryNavigator(String baseDeviceDirectory) {

        this.baseDeviceDirectory = baseDeviceDirectory;

        currentDirectory = new File(baseDeviceDirectory);

        if(currentDirectory == null) {
            Log.d(TAG,"ctor: cD!");
            return;
        }

        updateCurrentDirectoryEntries();
    }

    public boolean isValid() {
        return (currentDirectory != null);
    }

    private void updateCurrentDirectoryEntries() {
        File f = currentDirectory;// new File(this.currentDirectory);
        if( f == null ) {
            currentDirectoryEntries.clear();
            Log.d(TAG,"f==null");
            return;
        }

        File[] fileList = f.listFiles();
        if( fileList == null ) {
            Log.d(TAG,"fileList==null");
            return;
        }

        ArrayList<File> entries = new ArrayList<File>(Arrays.asList(fileList));
        currentDirectoryEntries = entries;
    }

    @Override
    public String getCurrentDirectoryName() {
        return currentDirectory.getName();
    }

//    @Override
//    public int changeDirectory(String currentDirectory) {
//        this.currentDirectory = currentDirectory;
//
//        updateCurrentDirectoryEntries();
//
//        return 0;
//    }

    @Override
    public AbstractDirectoryNavigator moveToChild(int pos) {

        if(currentDirectory == null) {
            Log.d(TAG,"!cD");
            return null;
        }

        if(currentDirectoryEntries == null) {
            Log.d(TAG,"!cDE");
            return null;
        }

        if( pos < 0 || currentDirectoryEntries.size() <= pos ) {
            Log.d(TAG,"invalid pos");
            return null;
        }

        currentDirectory = currentDirectoryEntries.get(pos);

        //String childPath = Paths.get( currentDirectory.path(), child.)
        updateCurrentDirectoryEntries();

        return this;
    }

    @Override
    public ArrayList<File> getCurrentDirectoryEntries() {
        return currentDirectoryEntries;
    }

    @Override
    public AbstractDirectoryNavigator moveToParent() {

        if(currentDirectory == null) {
            Log.d(TAG,"mTP: !cD");
            return null; // Something is wrong; return to the storage seletor
        }

        if(currentDirectory.getPath().equals(baseDeviceDirectory)) {
            return null; // Return to the storage selector (no error)
        }

        String p = currentDirectory.getParent();
        if(p == null) {
            Log.d(TAG,"!getParent");
            return null; // Something is wrong; return to the storage selector
        }

        File to = new File(p);
        if(to == null) {
            Log.d(TAG,"!to");
            return null; // Something is wrong; return to the storage selector
        }

        currentDirectory = to;

        updateCurrentDirectoryEntries();

        return this;
    }
}
