package com.geeks.mdm.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.geeks.mdm.BuildConfig
import com.geeks.mdm.core.GmdmApplication
import com.geeks.mdm.core.network.dto.HeartbeatRequest
import com.geeks.mdm.services.MdmServiceLauncher
import com.geeks.mdm.utils.SecurityVerifier

/**
 * Har 15 daqiqada: xizmat tirikmi, offline qulf, API heartbeat.
 */
class HeartbeatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? GmdmApplication
            ?: return Result.failure()

        return try {
            ensureForegroundServiceAlive()
            app.reapplyProtection()
            OfflineLockEvaluator.evaluateAndLockIfNeeded(applicationContext)
            sendHeartbeatIfPossible(app)
            MdmWorkerScheduler.enqueueSyncOneTime(applicationContext)
            MdmWorkerScheduler.enqueueAntiTamperOneTime(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "HeartbeatWorker xatolik", e)
            Result.retry()
        }
    }

    private fun ensureForegroundServiceAlive() {
        if (!MdmServiceLauncher.isServiceRunning()) {
            Log.w(TAG, "Foreground xizmat o'lik — qayta tiklanmoqda")
            MdmServiceLauncher.start(applicationContext)
        }
    }

    private suspend fun sendHeartbeatIfPossible(app: GmdmApplication) {
        val api = app.apiClient.service ?: return
        val scan = SecurityVerifier.scan(applicationContext)
        val request = HeartbeatRequest(
            deviceId = app.stateStore.deviceId,
            appVersion = BuildConfig.VERSION_NAME,
            isLocked = app.stateStore.isLocked,
            timestampMs = System.currentTimeMillis(),
            tamperFlags = scan.threats.takeIf { it.isNotEmpty() }
        )
        val response = api.sendHeartbeat(request)
        if (response.isSuccessful) {
            app.stateStore.lastSyncEpochMs = System.currentTimeMillis()
            Log.i(TAG, "Heartbeat muvaffaqiyatli")
        } else {
            Log.w(TAG, "Heartbeat muvaffaqiyatsiz: ${response.code()}")
        }
    }

    companion object {
        const val UNIQUE_PERIODIC_NAME = "gmdm_heartbeat_periodic"
        private const val TAG = "HeartbeatWorker"
    }
}
