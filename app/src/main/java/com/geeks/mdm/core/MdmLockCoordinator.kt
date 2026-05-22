package com.geeks.mdm.core

import android.content.Context
import android.util.Log
import com.geeks.mdm.ui.LockLayerController

/**
 * 4 qatlamli qulflash: Layer 1 (DPM) + Layer 2–4 (UI).
 */
class MdmLockCoordinator(
    private val context: Context,
    private val stateStore: MdmStateStore,
    private val devicePolicyController: DevicePolicyController
) {

    sealed class LockResult {
        data object Success : LockResult()
        data class Failed(val message: String) : LockResult()
        data class Partial(val message: String) : LockResult()
    }

    fun applyLockIfNeeded(reason: String = LOCK_REASON_LOCAL): LockResult {
        if (!stateStore.isLocked) {
            deactivateUiLayers()
            return LockResult.Success
        }
        return applyLockLayers(reason)
    }

    fun lockDevice(reason: String = LOCK_REASON_MANUAL_TEST): LockResult {
        stateStore.isLocked = true
        stateStore.lockReason = reason
        return applyLockLayers(reason)
    }

    fun unlockDevice(): LockResult {
        stateStore.clearLockState()
        deactivateUiLayers()
        Log.i(TAG, "unlockDevice: barcha qatlamlar o'chirildi")
        return LockResult.Success
    }

    private fun applyLockLayers(reason: String): LockResult {
        val appContext = context.applicationContext
        val failures = mutableListOf<String>()

        val layer1Ok = devicePolicyController.lockNow()
        if (!layer1Ok) {
            failures.add("Layer 1 (lockNow)")
            Log.w(TAG, "Layer 1 muvaffaqiyatsiz — UI qatlamlar davom etadi")
        } else {
            Log.i(TAG, "Layer 1 qo'llandi. Sabab: $reason")
        }

        val uiResult = LockLayerController.activateUiLayers(appContext, devicePolicyController)
        if (!uiResult.layer2Started) failures.add("Layer 2 (Kiosk)")
        if (!uiResult.layer3Shown) failures.add("Layer 3 (Overlay)")
        if (!uiResult.layer4Active) failures.add("Layer 4 (Accessibility)")

        return when {
            failures.isEmpty() -> LockResult.Success
            layer1Ok || uiResult.layer2Started -> LockResult.Partial(
                failures.joinToString(", ")
            )
            else -> LockResult.Failed(
                failures.joinToString(", ") + " — Device Admin va UI ruxsatlarini tekshiring."
            )
        }
    }

    private fun deactivateUiLayers() {
        LockLayerController.deactivateUiLayers(
            context.applicationContext,
            devicePolicyController
        )
    }

    companion object {
        private const val TAG = "MdmLockCoordinator"
        const val LOCK_REASON_MANUAL_TEST = "manual_test"
        const val LOCK_REASON_LOCAL = "persisted_lock"
        const val LOCK_REASON_OFFLINE = "offline_24h"
        const val LOCK_REASON_SERVER = "server_command"
        const val LOCK_REASON_TAMPER = "anti_tamper"
        const val LOCK_REASON_SIM_CHANGE = "sim_change"
        const val LOCK_REASON_ADMIN_DISABLED = "admin_disabled"
    }
}
