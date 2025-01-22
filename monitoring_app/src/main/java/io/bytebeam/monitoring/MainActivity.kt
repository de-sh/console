package io.bytebeam.monitoring

import android.Manifest.permission.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import io.bytebeam.*

class MainActivity : AppCompatActivity() {
    private var batteryOptIgnored = false
    lateinit var wakeLock: PowerManager.WakeLock
    var bytebeamServiceConnection: IBytebeamService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "UplinkService::WakeLock").apply {
                acquire()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestPermissions()
        } else {
            Toast.makeText(this, "App requires Android 7.0 (API 24) or higher", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(POST_NOTIFICATIONS)
        }

        permissions.add(ACCESS_FINE_LOCATION)
        permissions.add(ACCESS_COARSE_LOCATION)

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            PERMISSIONS_REQUEST_CODE
        )

        val packageName = packageName
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:$packageName")
            }
            startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST_CODE)
        } else {
            batteryOptIgnored = true
            checkAllPermissionsAndProceed()
        }
    }

    private fun checkAllPermissionsAndProceed() {
        val allPermissionsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    batteryOptIgnored
        } else {
            checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    batteryOptIgnored
        }

        if (allPermissionsGranted) {
            doSetup()
        } else {
            Toast.makeText(this, "Please grant all permissions required by this app", Toast.LENGTH_LONG).show()
        }
    }

    private fun doSetup() {
        setContentView(R.layout.activity_main)

        val testDeviceJson = assets.open("device.json").bufferedReader().use { it.readText() }
        val bytebeamConfig = BytebeamConfig(testDeviceJson, true, extraUplinkArgs = arrayOf("-v"))

        startBytebeamService(bytebeamConfig)
        findViewById<Button>(R.id.start_btn).setOnClickListener {
            startBytebeamService(bytebeamConfig)
        }
        findViewById<Button>(R.id.stop_btn).setOnClickListener {
            bytebeamServiceConnection?.stopService()
        }
        Handler(Looper.myLooper()!!).post({ printRandomLogLine() })
    }

    fun startBytebeamService(
        bytebeamConfig: BytebeamConfig,
    ) {
        val intent = Intent(this, BytebeamService::class.java)
        intent.putExtra(uplinkConfigKey, bytebeamConfig)
        startService(intent)
        bindService(
            intent,
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    Log.i(TAG, "Service connected")
                    bytebeamServiceConnection = IBytebeamService.Stub.asInterface(service)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    Log.e(TAG, "disconnected from bytebeam service")
                    Toast.makeText(this@MainActivity, "Disconnected from bytebeam service", Toast.LENGTH_SHORT).show()
                }
            },
            Context.BIND_IMPORTANT
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                checkAllPermissionsAndProceed()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BATTERY_OPTIMIZATION_REQUEST_CODE) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            batteryOptIgnored = pm.isIgnoringBatteryOptimizations(packageName)
            checkAllPermissionsAndProceed()
        }
    }

    companion object {
        private const val BATTERY_OPTIMIZATION_REQUEST_CODE = 124
    }
}

private val PERMISSIONS_REQUEST_CODE = 123

