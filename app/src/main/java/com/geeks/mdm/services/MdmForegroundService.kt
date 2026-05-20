package com.geeks.mdm.services

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Doimiy foreground xizmat — batareya optimizatsiyasi va OEM killerlarga qarshi himoya.
 * WorkManager heartbeat Modul C da qo'shiladi; bu yerda notification yangilanadi.
 */
class MdmForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Log.i(TAG, "onCreate")
        promoteToForeground()
        startNotificationRefreshLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForeground()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "onTaskRemoved — xizmat qayta ishga tushiriladi")
        MdmServiceLauncher.start(applicationContext)
    }

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        Log.w(TAG, "onDestroy — xizmat qayta tiklanmoqda")
        super.onDestroy()
        MdmServiceLauncher.start(applicationContext)
    }

    private fun promoteToForeground() {
        val notification = MdmNotificationFactory.build(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                MdmNotificationFactory.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(MdmNotificationFactory.NOTIFICATION_ID, notification)
        }
    }

  /**
     * Ba'zi OEMlarda notification yo'qolishi mumkin — muntazam yangilash.
     */
    private fun startNotificationRefreshLoop() {
        serviceScope.launch {
            while (isActive) {
                delay(NOTIFICATION_REFRESH_INTERVAL_MS)
                try {
                    val notification = MdmNotificationFactory.build(this@MdmForegroundService)
                    val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                    manager.notify(MdmNotificationFactory.NOTIFICATION_ID, notification)
                } catch (e: Exception) {
                    Log.e(TAG, "Notification yangilash xatolik", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MdmForegroundService"
        private const val NOTIFICATION_REFRESH_INTERVAL_MS = 5 * 60 * 1000L

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
