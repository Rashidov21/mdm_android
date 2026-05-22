package com.geeks.mdm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * SIM kart o'zgarishini aniqlash va shubhali holatda qulflash.
 */
class SimChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            ACTION_SIM_CARD_STATE_CHANGED,
            ACTION_SIM_APPLICATION_STATE_CHANGED,
            ACTION_SIM_STATE_CHANGED -> {
                MdmReceiverOrchestrator.onSimStateChanged(context)
            }
        }
    }

    companion object {
        private const val ACTION_SIM_CARD_STATE_CHANGED =
            "android.telephony.action.SIM_CARD_STATE_CHANGED"
        private const val ACTION_SIM_APPLICATION_STATE_CHANGED =
            "android.telephony.action.SIM_APPLICATION_STATE_CHANGED"

        @Suppress("DEPRECATION")
        private const val ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED"
    }
}
