package com.geeks.mdm.services

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri

/**
 * Xiaomi/Samsung va boshqa OEMlar uchun batareya va avto-ishga tushish sozlamalari.
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptimizationHelper"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun createIgnoreBatteryOptimizationsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        if (isIgnoringBatteryOptimizations(context)) return null
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
        }
    }

    fun createAppDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * OEM bo'yicha autostart / batareya sahifasiga yo'naltirish.
     * Topilmasa umumiy ilova sozlamalari ochiladi.
     */
    fun createOemBatteryIntent(context: Context): Intent {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val packageName = context.packageName

        val oemIntents = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> listOf(
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                },
                Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity"
                    )
                    putExtra("extra_pkgname", packageName)
                }
            )
            manufacturer.contains("samsung") -> listOf(
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.battery.ui.BatteryActivity"
                    )
                },
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                }
            )
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> listOf(
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                }
            )
            manufacturer.contains("vivo") -> listOf(
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                }
            )
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> listOf(
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                }
            )
            else -> emptyList()
        }

        for (intent in oemIntents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                return intent
            }
        }

        return createAppDetailsIntent(context)
    }

    fun launchIntent(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Activity topilmadi: ${intent.action}", e)
            false
        }
    }
}
