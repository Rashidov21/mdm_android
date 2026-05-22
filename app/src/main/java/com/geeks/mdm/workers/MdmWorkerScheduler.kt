package com.geeks.mdm.workers

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * WorkManager rejalashtirish — heartbeat, sync, anti-tamper.
 */
object MdmWorkerScheduler {

    private const val TAG = "MdmWorkerScheduler"
    private const val HEARTBEAT_INTERVAL_MINUTES = 15L

    fun scheduleAll(context: Context) {
        val appContext = context.applicationContext
        schedulePeriodicHeartbeat(appContext)
        enqueueAntiTamperOneTime(appContext)
        Log.i(TAG, "Barcha workerlar rejalashtirildi")
    }

    fun schedulePeriodicHeartbeat(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(
            HEARTBEAT_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .addTag(HeartbeatWorker.UNIQUE_PERIODIC_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HeartbeatWorker.UNIQUE_PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun enqueueSyncOneTime(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag(SyncWorker.UNIQUE_ONE_TIME_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncWorker.UNIQUE_ONE_TIME_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun enqueueAntiTamperOneTime(context: Context) {
        val request = OneTimeWorkRequestBuilder<AntiTamperWorker>()
            .addTag(AntiTamperWorker.UNIQUE_ONE_TIME_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            AntiTamperWorker.UNIQUE_ONE_TIME_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
