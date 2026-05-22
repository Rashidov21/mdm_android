package com.geeks.mdm.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import com.geeks.mdm.R

/**
 * Layer 3: TYPE_APPLICATION_OVERLAY — ekranning eng yuqori qatlami.
 */
object OverlayLockManager {

    private const val TAG = "OverlayLockManager"

    @Volatile
    private var overlayView: android.view.View? = null

    @Volatile
    private var windowManager: WindowManager? = null

    fun show(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!LockUiPermissionHelper.canDrawOverlays(appContext)) {
            Log.w(TAG, "Overlay ruxsati yo'q")
            return false
        }
        if (overlayView != null) {
            return true
        }

        return try {
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val inflater = LayoutInflater.from(appContext)
            val view = inflater.inflate(R.layout.overlay_lock_screen, null)

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.OPAQUE
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            wm.addView(view, layoutParams)
            overlayView = view
            windowManager = wm
            Log.i(TAG, "Overlay ko'rsatildi")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Overlay qo'shish xatolik", e)
            false
        }
    }

    fun hide() {
        val view = overlayView ?: return
        val wm = windowManager ?: return
        try {
            wm.removeView(view)
            Log.i(TAG, "Overlay yashirildi")
        } catch (e: Exception) {
            Log.e(TAG, "Overlay olib tashlash xatolik", e)
        } finally {
            overlayView = null
            windowManager = null
        }
    }

    fun isShowing(): Boolean = overlayView != null
}
