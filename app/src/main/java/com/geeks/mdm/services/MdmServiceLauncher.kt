package com.geeks.mdm.services

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Foreground xizmatni xavfsiz ishga tushirish va holatini tekshirish.
 */
object MdmServiceLauncher {

    private const val TAG = "MdmServiceLauncher"

    fun start(context: Context) {
        val appContext = context.applicationContext
        if (MdmForegroundService.isRunning) {
            Log.d(TAG, "Xizmat allaqachon ishlayapti")
            return
        }
        val intent = Intent(appContext, MdmForegroundService::class.java)
        try {
            ContextCompat.startForegroundService(appContext, intent)
            Log.i(TAG, "Foreground xizmat ishga tushirildi")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Foreground xizmat ishga tushmadi", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Foreground xizmat uchun ruxsat yo'q", e)
        }
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        if (!MdmForegroundService.isRunning) return
        try {
            appContext.stopService(Intent(appContext, MdmForegroundService::class.java))
            Log.i(TAG, "Foreground xizmat to'xtatildi")
        } catch (e: Exception) {
            Log.e(TAG, "Xizmatni to'xtatishda xatolik", e)
        }
    }

    fun isServiceRunning(): Boolean = MdmForegroundService.isRunning
}
