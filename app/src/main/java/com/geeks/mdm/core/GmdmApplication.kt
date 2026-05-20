package com.geeks.mdm.core

import android.app.Application
import android.util.Log
import com.geeks.mdm.BuildConfig
import com.geeks.mdm.core.network.ApiClient

/**
 * Ilova ishga tushganda holat, API va Device Owner siyosatlarini boshqaradi.
 */
class GmdmApplication : Application() {

    lateinit var stateStore: MdmStateStore
        private set

    lateinit var devicePolicyController: DevicePolicyController
        private set

    lateinit var lockCoordinator: MdmLockCoordinator
        private set

    lateinit var apiClient: ApiClient
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        stateStore = MdmStateStore(this)
        devicePolicyController = DevicePolicyController(this)
        lockCoordinator = MdmLockCoordinator(stateStore, devicePolicyController)
        apiClient = ApiClient()

        ensureDeviceId()
        applyStartupPolicies()
        restoreLockIfNeeded()

        Log.i(
            TAG,
            "GMDM ishga tushdi. API enabled=${BuildConfig.API_ENABLED}, package=$packageName"
        )
    }

    fun onDeviceAdminEnabled() {
        if (devicePolicyController.isDeviceOwner()) {
            devicePolicyController.applyDeviceOwnerPolicies()
        }
        restoreLockIfNeeded()
    }

    private fun ensureDeviceId() {
        val id = stateStore.deviceId
        Log.d(TAG, "Qurilma ID: $id")
    }

    private fun applyStartupPolicies() {
        if (devicePolicyController.isDeviceOwner()) {
            devicePolicyController.applyDeviceOwnerPolicies()
        }
    }

    private fun restoreLockIfNeeded() {
        if (!stateStore.isLocked) return
        when (val result = lockCoordinator.applyLockIfNeeded()) {
            is MdmLockCoordinator.LockResult.Success ->
                Log.i(TAG, "Saqlangan qulf holati qayta qo'llandi")
            is MdmLockCoordinator.LockResult.Failed ->
                Log.w(TAG, "Qulf qayta qo'llanmadi: ${result.message}")
        }
    }

    companion object {
        private const val TAG = "GmdmApplication"

        lateinit var instance: GmdmApplication
            private set
    }
}
