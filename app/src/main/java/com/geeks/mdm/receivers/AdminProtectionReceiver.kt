package com.geeks.mdm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Ekran qulfi ochilganda va ilova yangilanganda himoyani tiklash.
 */
class AdminProtectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_USER_PRESENT -> MdmReceiverOrchestrator.onUserPresent(context)
            Intent.ACTION_MY_PACKAGE_REPLACED -> MdmReceiverOrchestrator.onPackageReplaced(context)
        }
    }
}
