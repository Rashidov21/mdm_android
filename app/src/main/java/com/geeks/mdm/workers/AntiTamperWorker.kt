package com.geeks.mdm.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.geeks.mdm.core.GmdmApplication
import com.geeks.mdm.core.MdmLockCoordinator
import com.geeks.mdm.services.MdmServiceLauncher
import com.geeks.mdm.utils.SecurityVerifier

/**
 * Root, ADB, Mock GPS, Frida/Xposed tekshiruvi — shubhada darhol qulflash.
 */
class AntiTamperWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? GmdmApplication ?: return Result.failure()

        return try {
            val scan = SecurityVerifier.scan(applicationContext)
            if (!scan.isCompromised) {
                return Result.success()
            }

            val threatSummary = scan.threats.joinToString(",")
            Log.e(TAG, "Anti-tamper: $threatSummary")
            app.stateStore.recordTamperEvent("security_$threatSummary")

            when (val result = app.lockCoordinator.lockDevice(MdmLockCoordinator.LOCK_REASON_TAMPER)) {
                is MdmLockCoordinator.LockResult.Success ->
                    Log.i(TAG, "Tamper sababli qulflash qo'llandi")
                is MdmLockCoordinator.LockResult.Partial ->
                    Log.w(TAG, "Tamper qisman qulflash: ${result.message}")
                is MdmLockCoordinator.LockResult.Failed ->
                    Log.e(TAG, "Tamper qulflash muvaffaqiyatsiz: ${result.message}")
            }

            MdmServiceLauncher.start(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "AntiTamperWorker xatolik", e)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_ONE_TIME_NAME = "gmdm_antitamper_onetime"
        private const val TAG = "AntiTamperWorker"
    }
}
