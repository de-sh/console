package io.bytebeam.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.bytebeam.BytebeamConfig
import io.bytebeam.BytebeamService
import io.bytebeam.uplinkConfigKey

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val testDeviceJson = context.assets.open("device.json").bufferedReader().use { it.readText() }
            val bytebeamConfig = BytebeamConfig(testDeviceJson, true, extraUplinkArgs = arrayOf("-v"))
            val intent = Intent(context, BytebeamService::class.java)
            intent.putExtra(uplinkConfigKey, bytebeamConfig)
            context.startService(intent)
        }
    }
}