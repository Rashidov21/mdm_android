package com.geeks.mdm.receivers

import android.content.Context
import android.util.Log
import com.geeks.mdm.core.GmdmApplication
import com.geeks.mdm.core.MdmLockCoordinator
import com.geeks.mdm.services.MdmServiceLauncher
import com.geeks.mdm.workers.MdmWorkerScheduler

/**
 * Barcha receiver hodisalarini markazlashtirilgan boshqaruv.
 */
object MdmReceiverOrchestrator {

    private const val TAG = "MdmReceiverOrchestrator"

    fun onBootCompleted(context: Context, source: String) {
        Log.i(TAG, "Boot yakunlandi: $source")
        val app = context.applicationContext as? GmdmApplication ?: return
        app.stateStore.lastBootEpochMs = System.currentTimeMillis()
        app.reapplyProtection()
        MdmWorkerScheduler.scheduleAll(context)
        initializeSimBaselineIfNeeded(app)
    }

    fun onSimStateChanged(context: Context) {
        val app = context.applicationContext as? GmdmApplication ?: return
        if (!SimFingerprintProvider.hasPhoneStatePermission(context)) {
            Log.w(TAG, "SIM tekshiruvi: READ_PHONE_STATE ruxsati yo'q")
            return
        }

        val newFingerprint = SimFingerprintProvider.getFingerprint(context)
        val storedFingerprint = app.stateStore.simFingerprint

        if (storedFingerprint.isEmpty()) {
            app.stateStore.simFingerprint = newFingerprint
            Log.i(TAG, "SIM baseline saqlandi")
            return
        }

        if (storedFingerprint == newFingerprint) {
            return
        }

        Log.w(TAG, "SIM o'zgarishi aniqlandi")
        app.stateStore.simFingerprint = newFingerprint
        app.stateStore.recordTamperEvent("sim_change")

        when (val result = app.lockCoordinator.lockDevice(MdmLockCoordinator.LOCK_REASON_SIM_CHANGE)) {
            is MdmLockCoordinator.LockResult.Success ->
                Log.i(TAG, "SIM o'zgarishi sababli qulflash qo'llandi")
            is MdmLockCoordinator.LockResult.Partial ->
                Log.w(TAG, "SIM qisman qulflash: ${result.message}")
            is MdmLockCoordinator.LockResult.Failed ->
                Log.e(TAG, "SIM qulflash muvaffaqiyatsiz: ${result.message}")
        }

        MdmServiceLauncher.start(context)
    }

    fun onUserPresent(context: Context) {
        Log.d(TAG, "USER_PRESENT — himoya qayta qo'llanmoqda")
        val app = context.applicationContext as? GmdmApplication ?: return
        app.reapplyProtection()
        initializeSimBaselineIfNeeded(app)
    }

    fun onPackageReplaced(context: Context) {
        Log.i(TAG, "Paket yangilandi — xizmatlar qayta ishga tushirilmoqda")
        val app = context.applicationContext as? GmdmApplication ?: return
        app.reapplyProtection()
    }

    private fun initializeSimBaselineIfNeeded(app: GmdmApplication) {
        if (!SimFingerprintProvider.hasPhoneStatePermission(app)) return
        if (app.stateStore.simFingerprint.isNotEmpty()) return
        app.stateStore.simFingerprint = SimFingerprintProvider.getFingerprint(app)
    }
}
