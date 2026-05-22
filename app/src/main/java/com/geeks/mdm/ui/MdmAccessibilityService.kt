package com.geeks.mdm.ui

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import com.geeks.mdm.core.GmdmApplication

/**
 * Layer 4: Sozlamalar va tizim ilovalariga kirishni darhol bloklash.
 */
class MdmAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        Log.i(TAG, "Accessibility xizmati ulandi")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val app = applicationContext as? GmdmApplication ?: return
        if (!app.stateStore.isLocked) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        if (!BlockedPackages.isBlocked(packageName)) return

        Log.w(TAG, "Bloklangan ilova aniqlandi: $packageName")
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_HOME)
        LockScreenActivity.start(this)
        OverlayLockManager.show(this)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility uzildi")
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MdmAccessibilityService"

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}

/**
 * Bloklanadigan sozlamalar va tizim ilovalari (OEM qo'shimchalari bilan).
 */
internal object BlockedPackages {
    private val PACKAGES = setOf(
        "com.android.settings",
        "com.android.systemui",
        "com.google.android.settings",
        "com.samsung.android.settings",
        "com.samsung.android.lool",
        "com.miui.securitycenter",
        "com.miui.permcenter",
        "com.coloros.safecenter",
        "com.oppo.safe",
        "com.vivo.permissionmanager",
        "com.huawei.systemmanager",
        "com.iqoo.secure",
        "com.oneplus.security"
    )

    fun isBlocked(packageName: String): Boolean {
        if (PACKAGES.contains(packageName)) return true
        return packageName.contains("settings", ignoreCase = true) &&
            (packageName.startsWith("com.android") ||
                packageName.startsWith("com.miui") ||
                packageName.startsWith("com.samsung"))
    }
}
