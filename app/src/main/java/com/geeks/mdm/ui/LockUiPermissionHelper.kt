package com.geeks.mdm.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.net.toUri

/**
 * Overlay va Accessibility ruxsatlarini tekshirish va sozlamalarga yo'naltirish.
 */
object LockUiPermissionHelper {

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context.applicationContext)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedIds = setOf(
            "${context.packageName}/${MdmAccessibilityService::class.java.name}",
            "${context.packageName}/${MdmAccessibilityService::class.java.canonicalName}"
        )
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = manager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabled.any { it.id in expectedIds }
    }

    fun createOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun createAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
