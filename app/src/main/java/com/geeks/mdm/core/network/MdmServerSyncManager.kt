package com.geeks.mdm.core.network

import android.content.Context
import android.util.Log
import com.geeks.mdm.BuildConfig
import com.geeks.mdm.R
import com.geeks.mdm.core.GmdmApplication
import com.geeks.mdm.core.MdmLockCoordinator
import com.geeks.mdm.core.network.dto.HeartbeatRequest
import com.geeks.mdm.ui.LockScreenActivity
import com.geeks.mdm.utils.SecurityVerifier

/**
 * Dashboard bilan darhol sinxronlash: heartbeat + lock-status.
 */
object MdmServerSyncManager {

    private const val TAG = "MdmServerSyncManager"

    enum class LockAction {
        LOCKED,
        UNLOCKED,
        UNCHANGED
    }

    sealed class SyncResult {
        data object ApiDisabled : SyncResult()
        data class Success(
            val heartbeatOk: Boolean,
            val lockAction: LockAction,
            val serverShouldLock: Boolean?,
            val message: String
        ) : SyncResult()

        data class Failure(val message: String) : SyncResult()
    }

    suspend fun syncNow(context: Context): SyncResult {
        val app = context.applicationContext as? GmdmApplication
            ?: return SyncResult.Failure(context.getString(R.string.sync_context_missing))

        val api = app.apiClient.service
            ?: return SyncResult.ApiDisabled

        val heartbeatOk = sendHeartbeat(app, api)
        return applyLockStatus(context, app, api, heartbeatOk)
    }

    private suspend fun sendHeartbeat(
        app: GmdmApplication,
        api: MdmApiService
    ): Boolean {
        val scan = SecurityVerifier.scan(app)
        val request = HeartbeatRequest(
            deviceId = app.stateStore.deviceId,
            appVersion = BuildConfig.VERSION_NAME,
            isLocked = app.stateStore.isLocked,
            timestampMs = System.currentTimeMillis(),
            tamperFlags = scan.threats.takeIf { it.isNotEmpty() }
        )
        return try {
            val response = api.sendHeartbeat(request)
            if (response.isSuccessful && response.body()?.success == true) {
                app.stateStore.lastSyncEpochMs = System.currentTimeMillis()
                Log.i(TAG, "Heartbeat muvaffaqiyatli")
                true
            } else {
                Log.w(TAG, "Heartbeat muvaffaqiyatsiz: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Heartbeat xatolik", e)
            false
        }
    }

    private suspend fun applyLockStatus(
        context: Context,
        app: GmdmApplication,
        api: MdmApiService,
        heartbeatOk: Boolean
    ): SyncResult {
        return try {
            val response = api.getLockStatus(app.stateStore.deviceId)
            if (!response.isSuccessful || response.body() == null) {
                val msg = context.getString(R.string.sync_lock_status_http_error, response.code())
                Log.w(TAG, msg)
                return SyncResult.Failure(msg)
            }

            val body = response.body()!!
            app.stateStore.lastSyncEpochMs = System.currentTimeMillis()

            val lockAction = if (body.shouldLock) {
                val reason = body.reason?.takeIf { it.isNotBlank() }
                    ?: MdmLockCoordinator.LOCK_REASON_SERVER
                when (val result = app.lockCoordinator.lockDevice(reason)) {
                    is MdmLockCoordinator.LockResult.Success -> LockAction.LOCKED
                    is MdmLockCoordinator.LockResult.Partial -> LockAction.LOCKED
                    is MdmLockCoordinator.LockResult.Failed -> {
                        return SyncResult.Failure(
                            context.getString(R.string.sync_server_lock_not_applied, result.message)
                        )
                    }
                }
            } else if (app.stateStore.isLocked &&
                app.stateStore.lockReason == MdmLockCoordinator.LOCK_REASON_SERVER
            ) {
                app.lockCoordinator.unlockDevice()
                LockAction.UNLOCKED
            } else {
                LockAction.UNCHANGED
            }

            if (app.stateStore.isLocked) {
                LockScreenActivity.start(app)
            }

            val message = buildResultMessage(
                context,
                heartbeatOk,
                body.shouldLock,
                lockAction,
                body.reason
            )
            SyncResult.Success(
                heartbeatOk = heartbeatOk,
                lockAction = lockAction,
                serverShouldLock = body.shouldLock,
                message = message
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lock-status xatolik", e)
            SyncResult.Failure(e.message ?: context.getString(R.string.sync_unknown_error))
        }
    }

    private fun buildResultMessage(
        context: Context,
        heartbeatOk: Boolean,
        shouldLock: Boolean,
        lockAction: LockAction,
        reason: String?
    ): String {
        val heartbeatPart = context.getString(
            if (heartbeatOk) R.string.sync_msg_heartbeat_ok else R.string.sync_msg_heartbeat_error
        )
        val lockPart = when (lockAction) {
            LockAction.LOCKED -> context.getString(
                R.string.sync_msg_locked,
                reason ?: MdmLockCoordinator.LOCK_REASON_SERVER
            )
            LockAction.UNLOCKED -> context.getString(R.string.sync_msg_unlocked)
            LockAction.UNCHANGED -> if (shouldLock) {
                context.getString(R.string.sync_msg_unchanged_already_locked)
            } else {
                context.getString(R.string.sync_msg_unchanged_not_locked)
            }
        }
        return "$heartbeatPart. $lockPart"
    }
}
