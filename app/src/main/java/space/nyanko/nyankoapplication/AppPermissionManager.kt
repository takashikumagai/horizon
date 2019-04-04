package space.nyanko.nyankoapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * @brief Functions for checking/requesting app permissions
 *
 *
 */
class AppPermissionManager {

    public fun requestAppPermissions(mainActivity: MainActivity) {
        Log.d(TAG, "rAPs")

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "SDK_INT < LOLLIPOP")
            return
        }

        if (hasReadPermissions(mainActivity) && hasWritePermissions(mainActivity)) {
            Log.d(TAG, "Read/write permissions already granted")
            return
        }


        val myRequestCode = 12

        Log.d(TAG, "Requesting read/write permissions")
        ActivityCompat.requestPermissions(mainActivity,
                arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                myRequestCode)
    }

    private fun hasReadPermissions(mainActivity: MainActivity): Boolean {
        return ContextCompat.checkSelfPermission(mainActivity.getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasWritePermissions(mainActivity: MainActivity): Boolean {
        return ContextCompat.checkSelfPermission(mainActivity.getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    companion object {

        private val TAG = "AppPermissionManager"
    }
}
