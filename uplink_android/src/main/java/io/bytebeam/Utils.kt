package io.bytebeam

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


fun getPid(process: Process): Int? {
    return try {
        val pidField = process.javaClass.getDeclaredField("pid")
        pidField.isAccessible = true
        pidField.getInt(process)
    } catch (e: Exception) {
        null
    }
}

fun<T> dbg(value: T): T {
    println("dbg: $value")
    return value
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

fun getLocalDateTimeAsString(): String {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return dateFormat.format(calendar.time)
}

fun benchmarkNetwork(url: String): Pair<Long, Long> {
    val totalRequests = 10
    var successfulPings = 0
    var totalPingTime = 0L

    for (i in 1..totalRequests) {
        val result = measureTimeMillis { isReachable(url, 80, 1000) }
        val took = result.first
        val reachable = result.second
        if (reachable) {
            successfulPings += 1
        }
        totalPingTime += took
    }

    val averagePing = if (successfulPings > 0) totalPingTime / successfulPings else -1
    val packetLoss = 100 - (successfulPings * 100 / totalRequests)

    return Pair(averagePing, packetLoss.toLong())
}

private fun isReachable(addr: String, openPort: Int, timeOutMillis: Int): Boolean {
    try {
        Socket().use { soc ->
            soc.connect(InetSocketAddress(addr, openPort), timeOutMillis)
        }
        return true
    } catch (ex: IOException) {
        return false
    }
}

inline fun<T> measureTimeMillis(block: () -> T): Pair<Long, T> {
    val start = System.currentTimeMillis()
    val result = block()
    val took = System.currentTimeMillis() - start
    return Pair(took, result)
}

