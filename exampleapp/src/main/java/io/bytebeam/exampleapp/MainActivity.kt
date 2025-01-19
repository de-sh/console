package io.bytebeam.exampleapp

import android.util.Log
import UplinkConfig
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
import io.bytebeam.uplink_android.startUplinkService
import io.bytebeam.uplink_android.stopUplinkService
import kotlin.random.Random

// language=json
val testDeviceJson = """
    {
      "project_id": "sagar",
      "broker": "cloud.bytebeam.io",
      "port": 8883,
      "device_id": "1",
      "authentication": {
        "ca_certificate": "-----BEGIN CERTIFICATE-----\nMIIFrDCCA5SgAwIBAgICB+MwDQYJKoZIhvcNAQELBQAwdzEOMAwGA1UEBhMFSW5k\naWExETAPBgNVBAgTCEthcm5hdGFrMRIwEAYDVQQHEwlCYW5nYWxvcmUxFzAVBgNV\nBAkTDlN1YmJpYWggR2FyZGVuMQ8wDQYDVQQREwY1NjAwMTExFDASBgNVBAoTC0J5\ndGViZWFtLmlvMB4XDTIxMDkwMjExMDYyM1oXDTMxMDkwMjExMDYyM1owdzEOMAwG\nA1UEBhMFSW5kaWExETAPBgNVBAgTCEthcm5hdGFrMRIwEAYDVQQHEwlCYW5nYWxv\ncmUxFzAVBgNVBAkTDlN1YmJpYWggR2FyZGVuMQ8wDQYDVQQREwY1NjAwMTExFDAS\nBgNVBAoTC0J5dGViZWFtLmlvMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKC\nAgEAr/bnOa/8AUGZmd/s+7rejuROgeLqqU9X15KKfKOBqcoMyXsSO65UEwpzadpw\nMl7GDCdHqFTymqdnAnbhgaT1PoIFhOG64y7UiNgiWmbh0XJj8G6oLrW9rQ1gug1Q\n/D7x2fUnza71aixiwEL+KsIFYIdDuzmoRD3rSer/bKOcGGs0WfB54KqIVVZ1DwsU\nk1wx5ExsKo7gAdXMAbdHRI2Szmn5MsZwGL6V0LfsKLE8ms2qlZe50oo2woLNN6XP\nRfRL4bwwkdsCqXWkkt4eUSNDq9hJsuINHdhO3GUieLsKLJGWJ0lq6si74t75rIKb\nvvsFEQ9mnAVS+iuUUsSjHPJIMnn/J64Nmgl/R/8FP5TUgUrHvHXKQkJ9h/a7+3tS\nlV2KMsFksXaFrGEByGIJ7yR4qu9hx5MXf8pf8EGEwOW/H3CdWcC2MvJ11PVpceUJ\neDVwE7B4gPM9Kx02RNwvUMH2FmYqkXX2DrrHQGQuq+6VRoN3rEdmGPqnONJEPeOw\nZzcGDVXKWZtd7UCbcZKdn0RYmVtI/OB5OW8IRoXFYgGB3IWP796dsXIwbJSqRb9m\nylICGOceQy3VR+8+BHkQLj5/ZKTe+AA3Ktk9UADvxRiWKGcejSA/LvyT8qzz0dqn\nGtcHYJuhJ/XpkHtB0PykB5WtxFjx3G/osbZfrNflcQZ9h1MCAwEAAaNCMEAwDgYD\nVR0PAQH/BAQDAgKEMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYEFKl/MTbLrZ0g\nurneOmAfBHO+LHz+MA0GCSqGSIb3DQEBCwUAA4ICAQAlus/uKic5sgo1d2hBJ0Ak\ns1XJsA2jz+OEdshQHmCCmzFir3IRSuVRmDBaBGlJDHCELqYxKn6dl/sKGwoqoAQ5\nOeR2sey3Nmdyw2k2JTDx58HnApZKAVir7BDxbIbbHmfhJk4ljeUBbertNXWbRHVr\ncs4XBNwXvX+noZjQzmXXK89YBsV2DCrGRAUeZ4hQEqV7XC0VKmlzEmfkr1nibDr5\nqwbI+7QWIAnkHggYi27lL2UTHpbsy9AnlrRMe73upiuLO7TvkwYC4TyDaoQ2ZRpG\nHY+mxXLdftoMv/ZvmyjOPYeTRQbfPqoRqcM6XOPXwSw9B6YddwmnkI7ohNOvAVfD\nwGptUc5OodgFQc3waRljX1q2lawZCTh58IUf32CRtOEL2RIz4VpUrNF/0E2vts1f\npO7V1vY2Qin998Nwqkxdsll0GLtEEE9hUyvk1F8U+fgjJ3Rjn4BxnCN4oCrdJOMa\nJCaysaHV7EEIMqrYP4jH6RzQzOXLd0m9NaL8A/Y9z2a96fwpZZU/fEEOH71t3Eo3\nV/CKlysiALMtsHfZDwHNpa6g0NQNGN5IRl/w1TS1izzjzgWhR6r8wX8OPLRzhNRz\n2HDbTXGYsem0ihC0B8uzujOhTHcBwsfxZUMpGjg8iycJlfpPDWBdw8qrGu8LeNux\na0cIevjvYAtVysoXInV0kg==\n-----END CERTIFICATE-----\n",
        "device_certificate": "-----BEGIN CERTIFICATE-----\nMIIEZTCCAk2gAwIBAgICB+MwDQYJKoZIhvcNAQELBQAwdzEOMAwGA1UEBhMFSW5k\naWExETAPBgNVBAgTCEthcm5hdGFrMRIwEAYDVQQHEwlCYW5nYWxvcmUxFzAVBgNV\nBAkTDlN1YmJpYWggR2FyZGVuMQ8wDQYDVQQREwY1NjAwMTExFDASBgNVBAoTC0J5\ndGViZWFtLmlvMB4XDTI0MTIxMzA2MDkwOVoXDTM0MTIxMzA2MDkwOVowHDEOMAwG\nA1UEChMFc2FnYXIxCjAIBgNVBAMTATEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw\nggEKAoIBAQDxM9W9OpdQIxsx9eTDw0OxEZQR0WNYqoiewy+XxONfUn4gTDgqgWdZ\nir3ZIPS3GYigV4OhWovP+dMpuTLjtDjrGixmnbx9PzNCyEGKG6D0Uca45ZVeR4yG\nMbSWdUOFxpxyC8llI8cCe0WU6kUF53wIhvDWsbJ8lTGXqeUiUdkVOivt+2Jx03Kp\nrkzQh7vlkzLmgy6qkFM5xKDNcViTz9Io3RYkfzi7JVm31DX263dibsBo5FNqynav\nXnb79a370CHX0GSbZlzYH5P61jF/0QbmRjB/pKFNa18CjtvcQ0gpyvrbPTZkH8/4\n6a/5cM4jN9lDBwnKBEnKvkv7MpkuJqNDAgMBAAGjVjBUMA4GA1UdDwEB/wQEAwIF\noDATBgNVHSUEDDAKBggrBgEFBQcDAjAfBgNVHSMEGDAWgBSpfzE2y62dILq53jpg\nHwRzvix8/jAMBgNVHREEBTADggExMA0GCSqGSIb3DQEBCwUAA4ICAQCKSkOxe+W8\nb1bsOQ5bZtnyvyIYEPJQ1D1V7/E56/55+cH4snHUe7qatsr+MWA5aS0Orcxbt6kI\n+YXbrnjmx4z33rAtFIShJEOXrs+4+B/6KxjSr/AiWR63e98uKhAp/bNrwD39MwTH\nrZumo4/ZyTDp+YjeGeAIDd2l0bETiOo/x34mT0QJ0btkP7AG/TCLu8OkCWl1DXjo\n0UYdCuEVUGfZS1qIkiVr7deYJah+Z9Atvh+MTYUAsPSjW32p+AWrBBieK645cmxU\nZDsOIZ4SajRQlUe8B3yEz8K+Bkh2gieMKwcHNQ8duF/UPwZIJ9JDVZ9XNbx4r/R2\nUQMK1h7rrC7FN+u7PLwbh1VsZr8fmqTJGvW8ivFrnnTVdQZgIrNanHW88ZZzNzM3\n2fK97/3vNWa+m77K9YnjgSBsdOSLz0uDEilARXHSvCdjrAgYHUPFe83x4eRGeGec\ng+l8ZmigbPntezq3wt+O24Lg2WaW2GX2qkwEZN4n0rrdWCNxk6tvLNauIanZKsGY\nKUawyLZx3WXhlbBKLM25jcKAY58/biDncwuyfQs+27EF19jJWPEcryJ+yK1C+vdR\na0x8ZeEzaQHzHWZ6bf5umfxnzFnyPwMPV9PgF2FREYyEq9L3h3MGVg4VWbu8bvfS\njuGA2clbLYU8Y457u0r05wjz/m/mWijCcQ==\n-----END CERTIFICATE-----\n",
        "device_private_key": "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA8TPVvTqXUCMbMfXkw8NDsRGUEdFjWKqInsMvl8TjX1J+IEw4\nKoFnWYq92SD0txmIoFeDoVqLz/nTKbky47Q46xosZp28fT8zQshBihug9FHGuOWV\nXkeMhjG0lnVDhcaccgvJZSPHAntFlOpFBed8CIbw1rGyfJUxl6nlIlHZFTor7fti\ncdNyqa5M0Ie75ZMy5oMuqpBTOcSgzXFYk8/SKN0WJH84uyVZt9Q19ut3Ym7AaORT\nasp2r152+/Wt+9Ah19Bkm2Zc2B+T+tYxf9EG5kYwf6ShTWtfAo7b3ENIKcr62z02\nZB/P+Omv+XDOIzfZQwcJygRJyr5L+zKZLiajQwIDAQABAoIBAQDIWKnc9+HfaomY\nQgq2oGaFmXhIJIhEicHZPX82rQ+/iaHB6g1tIPdbZOa0h688tzTJGmf3imI/ad9/\n9JjUkkoE3kSrJ7H8xpYpjnUB5lkXFyXdRrV/Dicm//pkTxq673HbOPcbz0/qO8Jc\nIwsaEGDyJEO/0PHUkYgT1cbBbwLFoWMI6Mk3xWUQ5O9djHEPIF57kFPlPRdz8jkC\nTuArvxiGNky6OgUPhyAx83js0VBanXEKZ+rzxZRduPDYZBsIHjv5W0YHMbeStup6\nP+a7964wy3Re5W6Ee3If9GOv3Ti/WA86gz7M80c05clph78K82aYroUzdlmonW9s\nwl1wYtVBAoGBAP/KyVjrg8tgFPakx1tQqVfagXDd4uNrE0PhohuJhSG+L1EA4V6S\nFq0bsj4zKf7ba8LdeRHqVCW1hkr19734UHwqmU4m/aHxLiTlX0psRarjKyzsmJZ/\nQ31OuQ8jBz+6ETCsUsve2mMjKJt5sT3jSOKSDLpUr1GqcWWjklXlprzJAoGBAPFm\nA2UAFPCtdSSSvjh1ri3o96R0IQvgx/z7bFsRtO7OkjeCDdtRw3zX70b6DYS8cg16\ncmXPA8Zl452npl/rc71EhCjb8k0jLuv3EskSwwLiC/Wpdhx6jNQWCTZKSFBPl7X3\n00DkCBmrnNueFcpwRDVLUUPhVBRxFFWsvN04W8GrAoGBAJuBCrs2Ip9nQBdZwaCv\n/uNcAUk+e4rKM6IW79hR2E/VMSrLoDdAFO0UY14Q+LzpZC/JOKs9i/6IxqWXtw8U\ngMmblCvA5HypBOaFU1MJU6k6BauAApurrrnlO/gJ0YRad8zhVkx+pMGUREGQz8HS\nBSNIqtg0V4kMV3f04ye5P46RAoGAYrf2MWqsJZS58B/2nH47odjA1UcMcKAXCPUE\n5eVC2douX3bXFdbFHBvuZVdDCgJKngpyGAJlp8/pGoMB/f360e7gIRl5aGQ3/xWG\nYCZFC7vHakpH2/Od/emZENOl6Pnzr93OTZ4zVdQhjIqEnMn2itjnPdw7FzT7POIs\nRkeS9osCgYBoCgLGDVex6eFJmsrs5D4Remd6A8u/PqRiiRO3T4xYJYKqPyhqAdAn\nUJ6V1N8pW2c/dFznHXHY+GP77kgVIxsbbhVocIXvc4XTcfMJOxDTQjxIdis8luH7\nRTewBNnK3TRkZa1hYZMusmJlZQoYRVAYgbDUZsg1q0V7FC7NevzN/w==\n-----END RSA PRIVATE KEY-----\n"
      }
    }
""".trimIndent()

val uplinkConfig = UplinkConfig(testDeviceJson, true, extraUplinkArgs = arrayOf("-v"))

class MainActivity : AppCompatActivity() {
    lateinit var wakeLock: PowerManager.WakeLock

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
        startUplinkService(this, "monitoring service is running", uplinkConfig)

        findViewById<Button>(R.id.start_btn).setOnClickListener {
            startUplinkService(this, "monitoring service is running", uplinkConfig)
        }
        findViewById<Button>(R.id.stop_btn).setOnClickListener {
            stopUplinkService(this)
        }
        Handler(Looper.myLooper()!!).post(this::printRandomLogLine)
    }

    fun printRandomLogLine() {
        val logLevels = listOf(
            Log.VERBOSE,
            Log.DEBUG,
            Log.INFO,
            Log.WARN,
            Log.ERROR,
            Log.ASSERT
        )

        val tags = listOf(
            "MainActivity",
            "NetworkManager",
            "DatabaseHelper",
            "LoginViewModel",
            "AppCompatDelegate",
            "RecyclerAdapter",
            "WebSocketService",
            "FirebaseAuth",
            "JobScheduler",
            "BroadcastReceiver"
        )

        val messages = listOf(
            "Initialization successful after 3 retries",
            "Fetching data from server: endpoint=https://api.example.com/users",
            "User logged in: id=12345, name=JohnDoe",
            "Cache cleared for key: user_profile",
            "Error while parsing response: expected JSONArray but got JSONObject",
            "NullPointerException at com.example.app.MainActivity:42",
            "Connection timed out after 15 seconds",
            "Retrying request in 3 seconds: attempt=2",
            "Item clicked: position=4, id=5678",
            "Unknown host exception: api.example.com",
            "Database updated successfully: table=user_profiles, rows=42",
            "Service started: Intent { cmp=com.example.app/.MyService }",
            "Permission denied: android.permission.ACCESS_FINE_LOCATION",
            "Received unexpected response: HTTP 500 Internal Server Error",
            "Background task completed: jobId=7890, duration=325ms",
            "File not found: /storage/emulated/0/Download/file.txt",
            "Attempting to reconnect to server: interval=5s",
            "User session expired: token=abc123",
            "Low memory warning: available=15MB, threshold=50MB",
            "Authentication token refreshed successfully",
            "User profile updated: field=nickname, oldValue=John, newValue=Johnny",
            "Failed to bind service: component=com.example.app/.MyService",
            "Invalid JSON format received: expected field 'data' not found",
            "Bluetooth device connected: name=Headphones, address=00:11:22:33:44:55",
            "Screen orientation changed: LANDSCAPE -> PORTRAIT",
            "Task cancelled by user: taskId=1234",
            "APK file successfully downloaded: path=/downloads/app-release.apk",
            "Input validation failed: field=email, reason=Invalid format",
            "Settings restored to default: category=notifications",
            "Resource not found: R.drawable.ic_launcher",
            "User granted location access: time=2025-01-18T14:45:00Z",
            "Network request aborted: reason=No internet connection",
            "Sync completed with errors: 2 items failed, see logs for details",
            "Push notification received: id=notif123, title='New Message'",
            "App in foreground: time=2025-01-18T14:47:12Z",
            "Retry limit exceeded: jobId=456, retries=5",
            "Activity destroyed: com.example.app.MainActivity",
            "View hierarchy inflated: layout=R.layout.activity_main",
            "Crash detected: java.lang.IllegalStateException: Invalid state",
            "Audio focus gained: streamType=MUSIC",
            "Database migration completed: version=12 -> 13",
            "UI thread blocked for 250ms: potential ANR detected",
            "Widget updated: id=widget123, size=4x2",
            "Camera permission requested: rationaleDisplayed=true",
            "Fragment transaction committed: fragment=DetailsFragment",
            "Disk space critically low: available=2MB, required=100MB",
            "Proguard mapping file missing: obfuscation disabled",
            "Data encrypted successfully: method=AES-256, duration=12ms",
            "Unexpected EOF while reading stream: source=socket",
            "LifecycleObserver added: observer=com.example.app.MyObserver",
            "Analytics event logged: eventName=UserSignup, params={source=GoogleAds}",
            "Clipboard content accessed: length=32 characters",
            "Deep link handled: url=https://example.com/deeplink",
            "App shortcut created: name=QuickAction, id=shortcut1",
            "SDK version not supported: required=31, current=29",
            "Animation started: id=fadeInAnimation, duration=300ms",
            "Keyboard dismissed: inputField=email",
            "Preference value changed: key=theme, oldValue=Light, newValue=Dark",
            "Error decoding bitmap: OutOfMemoryError",
            "Foreground service started: notificationId=1001",
            "Hardware acceleration enabled: status=success",
            "Security exception: write access denied for /data/user/0/com.example.app",
            "Shared preferences cleared: file=settings_prefs",
            "Retrying with exponential backoff: delay=4s",
            "Scheduled task executed: taskId=5678, result=success",
            "Time zone changed: GMT+2 -> GMT+1",
            "Uncaught exception handled: java.io.IOException: Disk full",
            "WebView loaded URL: https://example.com",
            "Zip file decompressed: entries=42, duration=1.2s",
            "Dynamic feature module installed: name=chatModule, size=15MB",
            "Notification channel created: id=updates, importance=HIGH",
            "Request queued for execution: priority=HIGH, queueSize=3"
        )

        val level = logLevels.random()
        val tag = tags.random()
        val message = messages.random()

        when (level) {
            Log.VERBOSE -> Log.v(tag, message)
            Log.DEBUG -> Log.d(tag, message)
            Log.INFO -> Log.i(tag, message)
            Log.WARN -> Log.w(tag, message)
            Log.ERROR -> Log.e(tag, message)
            Log.ASSERT -> Log.wtf(tag, message)
        }

        Handler(Looper.myLooper()!!).postDelayed(this::printRandomLogLine, Random.nextLong(500, 1000))
    }
}
