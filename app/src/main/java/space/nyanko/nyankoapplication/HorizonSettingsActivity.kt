package space.nyanko.nyankoapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.CheckBox

class HorizonSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horizon_settings)
        // Queue all media files in the directory when a single media file is clicked to play.

        var checkBox = findViewById<CheckBox>(R.id.autoQueueMediaFiles)
        checkBox?.isChecked = HorizonOptions.autoQueueMediaFiles

        val autoQueueCheckBox = findViewById<CheckBox>(R.id.autoQueueMediaFiles)
        autoQueueCheckBox?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                val checkBox = findViewById<CheckBox>(R.id.autoQueueMediaFiles)
                HorizonOptions.autoQueueMediaFiles = checkBox?.isChecked ?: false
            }
        })
    }
}
