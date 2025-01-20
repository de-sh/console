package io.bytebeam

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


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
    println(value)
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
