package space.nyanko.nyankoapplication

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import java.io.File

class MediaInfoPopupWindow(activity: MainActivity) {

    var window: PopupWindow

    var layout: View? = null

    init {
        Log.d(TAG,"init")

        val inflater
                = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        layout = inflater.inflate(
                R.layout.media_info_window, // Resource ID
                activity.findViewById(R.id.app_layout), // root
                false
                )

        if(layout == null) { // sanity check ;should never be true
            Log.w(TAG,"!layout")
        }

        window = PopupWindow(activity)
        window.contentView = layout
        window.setFocusable(true)

        if(window.isShowing) {
            Log.w(TAG,"init popup showing")
        } else {
            Log.w(TAG,"init popup not showing")
        }

        window.dismiss()
    }

    fun open(activity: MainActivity, mediaFile: File) {
        Log.d(TAG,"open " + mediaFile.path)

        if(window == null) {
            Log.w(TAG,"open !mIP")
            return
        }

        val tags = intArrayOf(
                MediaMetadataRetriever.METADATA_KEY_TITLE,
                MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER,
                MediaMetadataRetriever.METADATA_KEY_ALBUM,
                MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,
                MediaMetadataRetriever.METADATA_KEY_COMPOSER,
                MediaMetadataRetriever.METADATA_KEY_COMPILATION,
                MediaMetadataRetriever.METADATA_KEY_BITRATE,
                MediaMetadataRetriever.METADATA_KEY_DURATION)
        val metaTags
                = HorizonUtils.getMediaFileMetaTags(mediaFile, tags)

        layout?.findViewById<TextView>(R.id.mi_title_val)?.text = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_TITLE)
        layout?.findViewById<TextView>(R.id.mi_track_val)?.text = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
        layout?.findViewById<TextView>(R.id.mi_album_val)?.text = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        layout?.findViewById<TextView>(R.id.mi_artist_val)?.text = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
        layout?.findViewById<TextView>(R.id.mi_composer_val)?.text = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
        //layout?.findViewById<TextView>(R.id.mi_compilation_val)?.setText(metaTags?.get(MediaMetadataRetriever.METADATA_KEY_COMPILATION))

        val bitrate = metaTags?.get(MediaMetadataRetriever.METADATA_KEY_BITRATE)
        val kbps = if(bitrate != null) (bitrate.toInt() / 1000).toString() + " kbps" else "-"

        layout?.findViewById<TextView>(R.id.mi_bitrate_val)?.text = kbps

        Log.d(TAG,"show")
        window.showAtLocation(
                //activity.findViewById(R.id.recycler_view),
                activity.findViewById(R.id.app_layout),
                Gravity.CENTER,
                0,
                0)
    }

    fun dismiss() {

        if(window == null) {
            Log.w(TAG,"dismiss !mIP")
            return
        }

        window.dismiss()
    }

    companion object {

        private const val TAG = "MediaInfoPopupWindow"
    }
}