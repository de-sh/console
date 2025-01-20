package io.bytebeam.monitoring

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.random.Random

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

    Handler(Looper.myLooper()!!).postDelayed({ printRandomLogLine() }, Random.nextLong(500, 1000))
}
