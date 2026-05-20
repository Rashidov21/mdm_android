package com.geeks.mdm.core

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import android.util.Log

/**
 * DevicePolicyManager orqali admin, owner va Layer 1 (lockNow) boshqaruvi.
 */
class DevicePolicyController(private val context: Context) {

    private val appContext = context.applicationContext
    private val dpm =
        appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(appContext, CustomDeviceAdminReceiver::class.java)

    fun isAdminActive(): Boolean = dpm.isAdminActive(adminComponent)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(appContext.packageName)

    fun lockNow(): Boolean {
        if (!isAdminActive()) {
            Log.w(TAG, "lockNow: Device Admin faol emas")
            return false
        }
        return try {
            dpm.lockNow()
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "lockNow xatolik", e)
            false
        }
    }

    /**
     * Device Owner bo'lganda o'chirishni bloklash va asosiy cheklovlarni qo'llash.
     */
    fun applyDeviceOwnerPolicies(): Boolean {
        if (!isDeviceOwner()) {
            Log.w(TAG, "applyDeviceOwnerPolicies: Device Owner emas")
            return false
        }
        return try {
            dpm.setUninstallBlocked(adminComponent, appContext.packageName, true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dpm.setStatusBarDisabled(adminComponent, true)
            }

            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)

            true
        } catch (e: SecurityException) {
            Log.e(TAG, "applyDeviceOwnerPolicies xatolik", e)
            false
        }
    }

    fun clearDeviceOwnerPolicies(): Boolean {
        if (!isDeviceOwner()) return false
        return try {
            dpm.setUninstallBlocked(adminComponent, appContext.packageName, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dpm.setStatusBarDisabled(adminComponent, false)
            }
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "clearDeviceOwnerPolicies xatolik", e)
            false
        }
    }

    fun getAdminComponent(): ComponentName = adminComponent

    companion object {
        private const val TAG = "DevicePolicyController"
    }
}
