package io.bytebeam.uplink.configurator

import android.content.Context
import java.io.File
import java.nio.file.Paths

abstract class Architecture {
    abstract val assetId: String
}
object X86 : Architecture() {
    override val assetId = "x86"
}
object X64 : Architecture() {
    override val assetId = "x86_64"
}
object ARM : Architecture() {
    override val assetId = "armeabi-v7a"
}
object ARM64 : Architecture() {
    override val assetId = "arm64-v8a"
}

class UnknownArchitecture(val name: String) : Architecture() {
    override val assetId: String
        get() = throw IllegalStateException("can't get assetId for UnknownArchitecture")
}

val ourArchitecture = run {
    Runtime.getRuntime().exec(arrayOf("uname", "-m")).let {
        it.waitFor()
        when (val name = it.inputStream.bufferedReader().readText().trim()) {
            "x86_64" -> X64
            "x86" -> X86
            else -> {
                if (name.contains("armv7")) {
                    ARM
                } else if (name.contains("arm")) {
                    ARM64
                } else {
                    UnknownArchitecture(name)
                }
            }
        }
    }
}

val Context.exePath: File
    get() = File(filesDir.absolutePath + "/exe")

val Context.uplinkConfigPath: File
    get() = File(filesDir.absolutePath + "/config.toml")

val Context.deviceConfigPath: File
    get() = File(filesDir.absolutePath + "/device.json")

val Context.persistenceDir: File
    get() = File(filesDir.absolutePath + "/persistence")

val Context.otaDir: File
    get() = File(filesDir.absolutePath + "/ota")
