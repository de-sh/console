package io.bytebeam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.*
import android.telephony.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.*
import java.lang.Process
import java.lang.Runtime
import java.lang.Thread
import java.util.concurrent.Executor

val StaticExecutor = object : Executor {
    override fun execute(cb: Runnable?) {
        cb?.run();
    }
}

class BytebeamService : Service() {
    val serviceThread = Handler(Looper.myLooper()!!)
    var uplinkConfig: BytebeamConfig? = null;
    var uplinkConfigChanged = false
    var uplinkProcess: Process? = null
    lateinit var uplinkLogger: LogRotate

    val binder = object : IBytebeamService.Stub() {
        override fun pushData(payload: BytebeamPayload) {
            this@BytebeamService.pushData(payload)
        }

        override fun stopService() {
            this@BytebeamService.stopSelf()
//            this@BytebeamService.onDestroy()
//            exitProcess(0)
        }
    }

    override fun onCreate() {
        createNotificationChannel();
        uplinkLogger = LogRotate(deviceJsonFile.parent!!, "out.log", 1024000, 8)
        serviceThread.post(this::processManager)
        serviceThread.post(this::powerStatusTask)
        serviceThread.post(this::networkStatusTask)
    }

    override fun onBind(intent: Intent): IBinder? {
        onStartCommand(intent, 0, 0)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(notificationId, buildNotification(intent?.getStringExtra(notificationMessageKey)!!))
        val newConfig = intent.getParcelableExtra<BytebeamConfig>(uplinkConfigKey)
        if (newConfig != uplinkConfig) {
            uplinkConfigChanged = true;
            uplinkConfig = newConfig
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceThread.removeCallbacksAndMessages(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
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
            Log.i(TAG, "reloading uplink config")
            stopUplink()
            setupUplink()
            uplinkConfigChanged = false
        }

        if (uplinkConfig != null && !uplinkIsRunning()) {
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
        Log.i(TAG, "stopping old uplink process: $this")
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
        Log.i(TAG, "setting up uplink config")

        File(uplinkFile.parent!!).mkdirs()
        overwriteFile(uplinkFile, assets.open("uplink"))
        uplinkFile.setExecutable(true)

        File(configTomlFile.parent!!).mkdirs()
        overwriteFile(configTomlFile, genConfigToml())

        File(deviceJsonFile.parent!!).mkdirs()
        overwriteFile(deviceJsonFile, uplinkConfig!!.credentials)
    }

    fun startUplink() {
        Log.i(TAG, "starting uplink")
        val baseCommand = arrayOf(
            uplinkFile.path, "-a", deviceJsonFile.path, "-c", configTomlFile.path, *uplinkConfig!!.extraUplinkArgs
        )
        val uplinkProcess = try {
            Runtime.getRuntime().exec(baseCommand)
        } catch (e1: Exception) {
            try {
                Runtime.getRuntime().exec(arrayOf("/system/bin/linker", *baseCommand))
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
            enable_remote_shell = ${uplinkConfig!!.enableRemoteShell}
            enable_stdin_collector = true
            
            [logging]
            tags = ["*"]
            min_level = 4
            
            [streams.power_status]
            batch_size = 8
            flush_period = 1
            persistence = { max_file_size = 102400, max_file_count = 10 }
            
            [streams.network_status]
            batch_size = 8
            flush_period = 1
            persistence = { max_file_size = 102400, max_file_count = 10 }
            
            [system_stats]
            enabled = true
            update_period = 2
            stream_size = 1
            
            [device_shadow]
            interval = 10
        """.trimIndent()
    }

    ////////////////////////////////////////////////////////////////////////

    fun pushData(payload: BytebeamPayload) {
        try {
            uplinkProcess?.let {
                it.outputStream.write(payload.toFlatJson().toByteArray());
                it.outputStream.write('\n'.code)
                it.outputStream.flush()
            }
        } catch (t: IOException) {
            Log.w(TAG, "uplink stdin closed, restarting...")
            stopUplink()
        }
    }

    var batteryInfoSequence = 1
    fun powerStatusTask() {
        val batteryInfo = getBatteryInfo()
        pushData(
            BytebeamPayload(
                stream = "power_status",
                sequence = batteryInfoSequence++,
                fields = mapOf(
                    "battery_level" to batteryInfo.first,
                    "charging" to batteryInfo.second
                )
            )
        )

        serviceThread.postDelayed(this::powerStatusTask, 5000)
    }

    fun getBatteryInfo(): Pair<Int, Boolean> {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status =
            batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: BatteryManager.BATTERY_STATUS_UNKNOWN

        val batteryLevel = if (level == -1 || scale == -1) -1 else (level / scale.toFloat() * 100).toInt()
        val batteryCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return Pair(batteryLevel, batteryCharging)
    }

    var networkInfoSequence = 1
    var cachedNetworkState = NetworkInfo(InternetType.Disconnected, 0, MobileConnectionType.Unknown, 0)
    fun networkStatusTask() {
        updateNetworkInfoOne()
        pushData(
            BytebeamPayload(
                stream = "network_status",
                sequence = networkInfoSequence++,
                fields = mapOf(
                    "ping_ms" to pingServer(),
                    "internet_connection_type" to cachedNetworkState.internetType.toString(),
                    "wifi_strength" to cachedNetworkState.wifiStrength,
                    "mobile_network_type" to cachedNetworkState.mobileNetworkType.toString(),
                    "mobile_network_level" to cachedNetworkState.mobileNetworkLevel
                )
            )
        )
        serviceThread.postDelayed(this::networkStatusTask, 1000)
    }

    fun updateNetworkInfoOne() {
        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cachedNetworkState.internetType = run {
            val network = connectivityManager.activeNetwork ?: return@run InternetType.Disconnected
            val capabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return@run InternetType.Disconnected

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                InternetType.Wifi
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                InternetType.Mobile
            } else {
                InternetType.Disconnected
            }
        }
        cachedNetworkState.wifiStrength = if (cachedNetworkState.internetType == InternetType.Wifi) {
            val wifiManager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo: WifiInfo = wifiManager.connectionInfo
            WifiManager.calculateSignalLevel(wifiInfo.rssi, 101)
        } else {
            0
        }

        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        for (ci in telephonyManager.allCellInfo) {
            if (ci is CellInfoLte) {
                cachedNetworkState.mobileNetworkType = MobileConnectionType.M4G
                cachedNetworkState.mobileNetworkLevel = ci.cellSignalStrength.dbm
            } else if (ci is CellInfoWcdma) {
                cachedNetworkState.mobileNetworkType = MobileConnectionType.M3G
                cachedNetworkState.mobileNetworkLevel = ci.cellSignalStrength.dbm
            } else if (ci is CellInfoGsm) {
                cachedNetworkState.mobileNetworkType = MobileConnectionType.M2G
                cachedNetworkState.mobileNetworkLevel = ci.cellSignalStrength.dbm
            } else if (ci is CellInfoCdma) {
                cachedNetworkState.mobileNetworkType = MobileConnectionType.M2G
                cachedNetworkState.mobileNetworkLevel = ci.cellSignalStrength.dbm
            }
        }
    }

    val updateNetworkInfoTwo = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            signalStrength?.level?.let {
                cachedNetworkState.mobileNetworkLevel = it
            }
        }

        override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
            Log.i(TAG, "data connection state change: $state, $networkType")
            if (state == TelephonyManager.DATA_CONNECTED) {
                if (networkType == TelephonyManager.NETWORK_TYPE_LTE) {
                    cachedNetworkState.mobileNetworkType = MobileConnectionType.M4G
                } else if (networkType == TelephonyManager.NETWORK_TYPE_CDMA) {
                    cachedNetworkState.mobileNetworkType = MobileConnectionType.M3G
                } else if (networkType == TelephonyManager.NETWORK_TYPE_GSM || networkType == TelephonyManager.NETWORK_TYPE_EDGE) {
                    cachedNetworkState.mobileNetworkType = MobileConnectionType.M2G
                }
            } else if (state == TelephonyManager.DATA_DISCONNECTED) {
                cachedNetworkState.mobileNetworkType = MobileConnectionType.Unknown
                cachedNetworkState.mobileNetworkLevel = 0
            }
        }
    }

    fun pingServer(): Long {
        return 0
//        return try {
//            val inetAddress = InetAddress.getByName(uplinkConfig!!.pingUrl)
//            val startTime = System.currentTimeMillis()
//            val isReachable = inetAddress.isReachable(1000)
//            val endTime = System.currentTimeMillis()
//            if (isReachable) {
//                endTime - startTime
//            } else {
//                -1L
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            -1L
//        }
    }
}

val TAG = "UplinkService"

private val notificationMessageKey = "notificationMessage"
private val uplinkConfigKey = "uplinkConfig"

private val channelId = "foregroundNotification"
private val notificationId = 1

fun startBytebeamService(
    context: Context,
    notificationMessage: String,
    bytebeamConfig: BytebeamConfig,
    onConnected: (IBytebeamService) -> Unit
) {
    val intent = Intent(context, BytebeamService::class.java)
    intent.putExtra(notificationMessageKey, notificationMessage)
    intent.putExtra(uplinkConfigKey, bytebeamConfig)
    context.startService(intent)
    context.bindService(
        intent,
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.i(TAG, "Service connected")
                onConnected(IBytebeamService.Stub.asInterface(service))
            }

            override fun onServiceDisconnected(name: ComponentName?) {}
        },
        Context.BIND_IMPORTANT
    )
}

enum class InternetType { Wifi, Mobile, Disconnected }

enum class MobileConnectionType { M2G, M3G, M4G, Unknown }

data class NetworkInfo(
    var internetType: InternetType,
    var wifiStrength: Int,
    var mobileNetworkType: MobileConnectionType,
    var mobileNetworkLevel: Int,
)