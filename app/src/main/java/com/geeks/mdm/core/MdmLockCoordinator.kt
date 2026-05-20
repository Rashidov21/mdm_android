package com.geeks.mdm.core

import android.util.Log

/**
 * Qulflash buyruqlarini boshqaradi.
 * Modul A: faqat Layer 1 (lockNow) va holatni saqlash.
 * Layer 2–4 keyingi modullarda shu klassga qo'shiladi.
 */
class MdmLockCoordinator(
    private val stateStore: MdmStateStore,
    private val devicePolicyController: DevicePolicyController
) {

    sealed class LockResult {
        data object Success : LockResult()
        data class Failed(val message: String) : LockResult()
    }

    fun applyLockIfNeeded(reason: String = LOCK_REASON_LOCAL): LockResult {
        if (!stateStore.isLocked) {
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
        // Layer 2–4 keyingi modullarda to'liq olib tashlanadi
        Log.i(TAG, "unlockDevice: holat saqlandi, Layer 2–4 hali ulanmagan")
        return LockResult.Success
    }

    private fun applyLockLayers(reason: String): LockResult {
        val layer1Ok = devicePolicyController.lockNow()
        if (!layer1Ok) {
            return LockResult.Failed("Layer 1 (lockNow) muvaffaqiyatsiz. Device Admin yoqing.")
        }
        Log.i(TAG, "Layer 1 qo'llandi. Sabab: $reason")
        return LockResult.Success
    }

    companion object {
        private const val TAG = "MdmLockCoordinator"
        const val LOCK_REASON_MANUAL_TEST = "manual_test"
        const val LOCK_REASON_LOCAL = "persisted_lock"
        const val LOCK_REASON_OFFLINE = "offline_24h"
        const val LOCK_REASON_SERVER = "server_command"
        const val LOCK_REASON_TAMPER = "anti_tamper"
    }
}
