package io.bytebeam.exampleapp

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.bytebeam.IBytebeamService
import io.bytebeam.BytebeamConfig
import io.bytebeam.startBytebeamService

class MainActivity : AppCompatActivity() {
    lateinit var wakeLock: PowerManager.WakeLock
    var bytebeamServiceConnection: IBytebeamService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "UplinkService::NetworkInfoWakeLock").apply {
                acquire()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            val requestPermissionLauncher = this.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    checkBatteryOptimizationPermission()
                }
            }
            requestPermissionLauncher.launch(POST_NOTIFICATIONS)
        } else {
            checkBatteryOptimizationPermission()
        }
    }

    private fun checkBatteryOptimizationPermission() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
        doSetup()
    }

    fun doSetup() {
        val testDeviceJson = assets.open("device.json").bufferedReader().use { it.readText() }
        val bytebeamConfig = BytebeamConfig(testDeviceJson, true, extraUplinkArgs = arrayOf("-v"))

        startBytebeamService(this, "monitoring service is running", bytebeamConfig, { bytebeamServiceConnection = it })

        findViewById<Button>(R.id.start_btn).setOnClickListener {
            startBytebeamService(this, "monitoring service is running", bytebeamConfig, { bytebeamServiceConnection = it })
        }
        findViewById<Button>(R.id.stop_btn).setOnClickListener {
            bytebeamServiceConnection?.stopService()
        }
        Handler(Looper.myLooper()!!).post({ printRandomLogLine() })
    }
}
