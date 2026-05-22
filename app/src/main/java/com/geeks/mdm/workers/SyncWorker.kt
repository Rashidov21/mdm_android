package com.geeks.mdm.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.geeks.mdm.core.GmdmApplication
import com.geeks.mdm.core.MdmLockCoordinator

/**
 * Serverdan lock/unlock holatini sinxronlash.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? GmdmApplication ?: return Result.failure()
        val api = app.apiClient.service

        if (api == null) {
            Log.d(TAG, "API yo'q — sync o'tkazib yuborildi")
            return Result.success()
        }

        return try {
            val response = api.getLockStatus(app.stateStore.deviceId)
            if (!response.isSuccessful || response.body() == null) {
                Log.w(TAG, "Lock status olinmadi: ${response.code()}")
                return Result.retry()
            }

            val body = response.body()!!
            app.stateStore.lastSyncEpochMs = System.currentTimeMillis()

            if (body.shouldLock) {
                val reason = body.reason?.takeIf { it.isNotBlank() }
                    ?: MdmLockCoordinator.LOCK_REASON_SERVER
                app.lockCoordinator.lockDevice(reason)
                Log.i(TAG, "Server buyrug'i: LOCK ($reason)")
            } else if (app.stateStore.isLocked &&
                app.stateStore.lockReason == MdmLockCoordinator.LOCK_REASON_SERVER
            ) {
                app.lockCoordinator.unlockDevice()
                Log.i(TAG, "Server buyrug'i: UNLOCK")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker xatolik", e)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_ONE_TIME_NAME = "gmdm_sync_onetime"
        private const val TAG = "SyncWorker"
    }
}
