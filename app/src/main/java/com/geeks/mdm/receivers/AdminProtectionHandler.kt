package com.geeks.mdm.receivers

import android.content.Context
import android.util.Log
import com.geeks.mdm.core.GmdmApplication
import com.geeks.mdm.core.MdmLockCoordinator
import com.geeks.mdm.services.MdmServiceLauncher

/**
 * Device Admin o'chirilishi va boshqa admin himoya hodisalari.
 */
object AdminProtectionHandler {

    private const val TAG = "AdminProtectionHandler"

    fun onAdminDisabled(context: Context) {
        val app = context.applicationContext as? GmdmApplication ?: return
        app.stateStore.adminDisabledAtEpochMs = System.currentTimeMillis()
        app.stateStore.recordTamperEvent("admin_disabled")

        Log.e(TAG, "Device Admin o'chirildi — tamper hodisasi qayd etildi")

        when (val result = app.lockCoordinator.lockDevice(MdmLockCoordinator.LOCK_REASON_ADMIN_DISABLED)) {
            is MdmLockCoordinator.LockResult.Success ->
                Log.i(TAG, "Admin o'chirilganda qulflash qo'llandi")
            is MdmLockCoordinator.LockResult.Partial ->
                Log.w(TAG, "Admin qisman qulflash: ${result.message}")
            is MdmLockCoordinator.LockResult.Failed ->
                Log.w(TAG, "Admin o'chirilganda qulflash ishlamadi: ${result.message}")
        }

        MdmServiceLauncher.start(context)
    }

    fun onDisableRequested(context: Context) {
        val app = context.applicationContext as? GmdmApplication ?: return
        app.stateStore.recordTamperEvent("admin_disable_requested")
        Log.w(TAG, "Device Admin o'chirish so'raldi")
        MdmServiceLauncher.start(context)
    }
}
