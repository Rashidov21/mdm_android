package com.geeks.mdm.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.io.File

/**
 * Root, ADB, Mock Location, Frida/Xposed tekshiruvi.
 */
object SecurityVerifier {

    data class ScanResult(
        val isCompromised: Boolean,
        val threats: List<String>
    )

    private val SU_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su"
    )

    private val FRIDA_XPOSED_PACKAGES = listOf(
        "de.robv.android.xposed.installer",
        "org.meowcat.edxposed.manager",
        "org.lsposed.manager",
        "com.saurik.substrate",
        "re.frida.server",
        "com.frida.server",
        "com.topjohnwu.magisk"
    )

    fun scan(context: Context): ScanResult {
        val threats = mutableListOf<String>()

        if (isRootDetected()) threats.add("root")
        if (isAdbDebuggingEnabled(context)) threats.add("adb_debug")
        if (isMockLocationEnabled(context)) threats.add("mock_location")
        if (hasForbiddenPackages(context)) threats.add("hooking_framework")
        if (isFridaInMaps()) threats.add("frida_maps")

        return ScanResult(
            isCompromised = threats.isNotEmpty(),
            threats = threats
        )
    }

    private fun isRootDetected(): Boolean {
        if (Build.TAGS?.contains("test-keys") == true) return true
        return SU_PATHS.any { path -> File(path).exists() }
    }

    private fun isAdbDebuggingEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    private fun isMockLocationEnabled(context: Context): Boolean {
        return try {
            @Suppress("DEPRECATION")
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ALLOW_MOCK_LOCATION
            ) == "1"
        } catch (_: Exception) {
            false
        }
    }

    private fun hasForbiddenPackages(context: Context): Boolean {
        val pm = context.packageManager
        return FRIDA_XPOSED_PACKAGES.any { packageName ->
            try {
                pm.getPackageInfo(packageName, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun isFridaInMaps(): Boolean {
        return try {
            val maps = File("/proc/self/maps").readText()
            maps.contains("frida", ignoreCase = true) ||
                maps.contains("gadget", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}
