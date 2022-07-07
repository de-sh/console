package io.bytebeam.UplinkDemo

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import io.bytebeam.uplink.Uplink

const val TAG = "==APP=="

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.statusView).text = "configuratorAvaialable: ${Uplink.configuratorAvailable(this)}\nserviceRunning: ${Uplink.serviceRunning(this)}";

        findViewById<Button>(R.id.start_btn).setOnClickListener {
            Intent(this, UplinkActivity::class.java).also {
                startActivity(it)
            }
        }
    }
}