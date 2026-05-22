package com.geeks.mdm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Qurilma yoqilganda fon xizmatlari va himoyani qayta tiklash.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            ACTION_QUICKBOOT_POWERON -> {
                MdmReceiverOrchestrator.onBootCompleted(context, action)
            }
        }
    }

    companion object {
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
