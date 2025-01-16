package io.bytebeam.uplink_android

import LogRotate
import UplinkConfig
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader


class UplinkService : Service() {
    val serviceThread = Handler(Looper.myLooper()!!)
    var uplinkConfig: UplinkConfig? = null;
    var uplinkConfigChanged = false
    var uplinkProcess: Process? = null
    lateinit var uplinkLogger: LogRotate

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        createNotificationChannel();
        uplinkLogger = LogRotate(deviceJsonFile.parent!!, "out.log", 1024000, 8)
        serviceThread.post(this::processManager)
        serviceThread.post(this::systemInfoTask)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(notificationId, buildNotification(intent?.getStringExtra(notificationMessageKey)!!))
        val newConfig = intent.getParcelableExtra<UplinkConfig>(uplinkConfigKey)
        if (newConfig != uplinkConfig) {
            uplinkConfigChanged = true;
            uplinkConfig = newConfig
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceThread.removeCallbacksAndMessages(null)
        stopUplink()
    }

    ////////////////////////////////////////////////////////////////////////

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Monitoring Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "This channel is used by the monitoring service"

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification? {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(message)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    ////////////////////////////////////////////////////////////////////////

    fun processManager() {
        if (uplinkConfigChanged) {
            Log.i(TAG, "uplink config changed, stopping old uplink and reconfiguring setup")
            stopUplink()
            setupUplink()
            uplinkConfigChanged = false
        }

        if (uplinkConfig != null && !uplinkIsRunning()) {
            Log.i(TAG, "Uplink isn't running, restarting it")
            startUplink()
        }

        serviceThread.postDelayed(this::processManager, 1000)
    }

    fun uplinkIsRunning(): Boolean {
        if (uplinkProcess == null) {
            return false
        }

        try {
            uplinkProcess!!.exitValue()
            return false
        } catch (e: Exception) {
            return true
        }
    }

    fun stopUplink() {
        if (uplinkProcess == null) return
        uplinkProcess!!.destroy()
        for (idx in 1..6) {
            Thread.sleep(500)
            val uplinkRunning = uplinkIsRunning()
            if (!uplinkRunning) {
                break;
            } else if (idx == 6) {
                val uplinkPid = getPid(uplinkProcess!!)
                if (uplinkPid != null) {
                    Log.i(TAG, "uplink still running, doing kill -9")
                    android.os.Process.killProcess(uplinkPid)
                } else {
                    Log.e(TAG, "Couldn't get uplink pid, process might be orphaned")
                }
            }
        }
        uplinkProcess = null
    }

    /**
     * Created this:
     * application_data_dir
     *  - bytebeam
     *     - uplink_module
     *        - uplink
     *        - config.toml
     *     - uplink_data
     *        - device.json
     *        - out.log.*
     */
    fun setupUplink() {
        File(uplinkFile.parent!!).mkdirs()
        overwriteFile(uplinkFile, assets.open("uplink"))
        uplinkFile.setExecutable(true)

        File(configTomlFile.parent!!).mkdirs()
        overwriteFile(configTomlFile, genConfigToml())

        File(deviceJsonFile.parent!!).mkdirs()
        overwriteFile(deviceJsonFile, uplinkConfig!!.credentials)
    }

    fun startUplink() {
        Log.i(TAG, "starting uplink process")
        val uplinkProcess = try {
            Runtime.getRuntime().exec(
                arrayOf(
                    "/system/bin/linker", uplinkFile.path, "-a", deviceJsonFile.path, "-c", configTomlFile.path, "-v"
                )
            )
        } catch (e1: Exception) {
            try {
                Runtime.getRuntime().exec(
                    arrayOf(
                        "/system/bin/linker", uplinkFile.path, "-a", deviceJsonFile.path, "-c", configTomlFile.path, "-v"
                    )
                )
            } catch (e2: Exception) {
                e2.initCause(e1)
                Log.e(TAG, "couldn't start uplink", e2)
                return
            }
        }
        val stdout = BufferedReader(InputStreamReader(uplinkProcess.inputStream))
        val stderr = BufferedReader(InputStreamReader(uplinkProcess.errorStream))
        Thread {
            try {
                stdout.forEachLine {
                    uplinkLogger.pushLogLine(it)
                }
            } catch (t: Throwable) {
                return@Thread
            }
        }.start()
        Thread {
            try {
                stderr.forEachLine {
                    Log.w(TAG, it)
                    uplinkLogger.pushLogLine(it)
                }
            } catch (t: Throwable) {
                return@Thread
            }
        }.start()

        this.uplinkProcess = uplinkProcess
    }

    ////////////////////////////////////////////////////////////////////////

    val uplinkFile: File
        get() = File(filesDir, "bytebeam/uplink_module/uplink")

    val configTomlFile: File
        get() = File(filesDir, "bytebeam/uplink_module/config.toml")

    val deviceJsonFile: File
        get() = File(filesDir, "bytebeam/uplink_data/device.json")

    fun genConfigToml(): String {
        val persistencePath = File(deviceJsonFile.parent, "persistence")

        // language=toml
        return """
            persistence_path = "$persistencePath"
            enable_remote_shell = true
            stdin_collector = true
            
            # [logging]
            # tags = ["UplinkService"]
            # min_level = 4
            
            [system_stats]
            enabled = true
            update_period = 2
            stream_size = 1
            
            [device_shadow]
            interval = 10
        """.trimIndent()
    }

    ////////////////////////////////////////////////////////////////////////

    fun systemInfoTask() {
        Log.w(TAG, "Battery level: ${getBatteryLevel()}")
//        Log.w(TAG, "Network level: ${getMobileSignalStrength()}")
        serviceThread.postDelayed(this::systemInfoTask, 5000)
    }

    fun getBatteryLevel(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level == -1 || scale == -1) -1 else (level / scale.toFloat() * 100).toInt()
    }

    // Signal strength in dBm, where higher values are better
    fun getMobileSignalStrength(): Int {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var signalStrengthO = -1 // Default value

        val listener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                super.onSignalStrengthsChanged(signalStrength)
                signalStrength?.let {
                    signalStrengthO = it.gsmSignalStrength
                }
            }
        }

        telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)

        return signalStrengthO
    }
}

val TAG = "UplinkService"

private val notificationMessageKey = "notificationMessage"
private val uplinkConfigKey = "uplinkConfig"

private val channelId = "foregroundNotification"
private val notificationId = 1

fun startUplinkService(context: Context, notificationMessage: String, uplinkConfig: UplinkConfig) {
    val intent = Intent(context, UplinkService::class.java)
    intent.putExtra(notificationMessageKey, notificationMessage)
    intent.putExtra(uplinkConfigKey, uplinkConfig)
    context.startService(intent)
}

fun stopUplinkService(context: Context) {
    context.stopService(Intent(context, UplinkService::class.java))
}

fun overwriteFile(file: File, content: String) {
    overwriteFile(file, content.byteInputStream(Charsets.UTF_8))
}

fun overwriteFile(file: File, content: InputStream) {
    file.delete()
    file.createNewFile()
    val fos = FileOutputStream(file)
    val buffer = ByteArray(1024)
    while (true) {
        val read = content.read(buffer)
        if (read > 0) {
            fos.write(buffer, 0, read)
        } else {
            break
        }
    }
    fos.close()
    content.close()
}
