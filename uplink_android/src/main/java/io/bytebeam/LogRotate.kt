package io.bytebeam;

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException

class LogRotate(
    private val directory: String,
    private val fileNameBase: String,
    private val maxFileSize: Long,
    private val maxFileCount: Int,
) {

    private val logFile = File(directory, fileNameBase)

    init {
        val dir = File(directory)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    @Synchronized
    fun pushLogLine(line: String) {
        try {
            val writer = FileWriter(logFile, true)
            writer.appendLine(line)
            writer.close()

            if (logFile.length() > maxFileSize) {
                rotateLogs()
            }
        } catch (e: IOException) {
            println(e)
            Log.e(TAG, "failed to write uplink logs", e)
        }
    }

    private fun rotateLogs() {
        for (i in maxFileCount downTo 1) {
            val currentFile = File(directory, "$fileNameBase.$i")
            val nextFile = File(directory, "$fileNameBase.${i + 1}")

            if (currentFile.exists()) {
                if (i == maxFileCount) {
                    currentFile.delete()
                } else {
                    currentFile.renameTo(nextFile)
                }
            }
        }

        logFile.renameTo(File(directory, "$fileNameBase.1"))

        logFile.createNewFile()
    }
}

