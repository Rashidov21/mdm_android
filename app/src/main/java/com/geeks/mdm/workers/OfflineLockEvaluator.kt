package com.geeks.mdm.workers

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.geeks.mdm.BuildConfig
import com.geeks.mdm.core.GmdmApplication
import com.geeks.mdm.core.MdmLockCoordinator

/**
 * 24 soat server bilan aloqa bo'lmasa avtonom qulflash (API yoqilganda).
 */
object OfflineLockEvaluator {

    private const val TAG = "OfflineLockEvaluator"
    private const val OFFLINE_THRESHOLD_MS_RELEASE = 24L * 60L * 60L * 1000L
    private const val OFFLINE_THRESHOLD_MS_DEBUG = 5L * 60L * 1000L

    fun evaluateAndLockIfNeeded(context: Context): Boolean {
        val app = context.applicationContext as? GmdmApplication ?: return false
        if (!app.apiClient.isEnabled) {
            return false
        }

        val thresholdMs = if (BuildConfig.DEBUG) {
            OFFLINE_THRESHOLD_MS_DEBUG
        } else {
            OFFLINE_THRESHOLD_MS_RELEASE
        }

        val now = System.currentTimeMillis()
        val lastSync = app.stateStore.lastSyncEpochMs
        val referenceTime = if (lastSync > 0L) {
            lastSync
        } else {
            getFirstInstallTime(context)
        }

        if (referenceTime <= 0L) return false

        val offlineDuration = now - referenceTime
        if (offlineDuration < thresholdMs) {
            return false
        }

        if (app.stateStore.isLocked && app.stateStore.lockReason == MdmLockCoordinator.LOCK_REASON_OFFLINE) {
            return true
        }

        Log.w(TAG, "Offline chegaradan oshdi (${offlineDuration}ms) — qulflash")
        app.stateStore.recordTamperEvent("offline_${offlineDuration}ms")
        app.lockCoordinator.lockDevice(MdmLockCoordinator.LOCK_REASON_OFFLINE)
        return true
    }

    private fun getFirstInstallTime(context: Context): Long {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.firstInstallTime
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "firstInstallTime olinmadi", e)
            0L
        }
    }
}
