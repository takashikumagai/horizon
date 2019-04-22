package space.nyanko.nyankoapplication

import android.util.Log
import java.io.Serializable
import java.util.HashMap

class MetadataCache : Serializable {
    // Maps a filename to a hashmap of tags and values
    var tags: HashMap<String, HashMap<Int, String>> = HashMap()

    fun clear() {
        Log.d(TAG, "clearing")
        tags.clear()
    }

    companion object {

        private val TAG = "MetadataCache"
    }
}