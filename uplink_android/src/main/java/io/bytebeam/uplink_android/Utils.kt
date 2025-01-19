package io.bytebeam.uplink_android


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