package com.geeks.mdm.core

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * Device Admin hodisalari: yoqish, o'chirish so'rovi va o'chirilganda reaksiya.
 */
class CustomDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device Admin yoqildi")
        val app = context.applicationContext as GmdmApplication
        app.onDeviceAdminEnabled()
        Toast.makeText(context, R.string.admin_enabled_toast, Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "Device Admin o'chirildi")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return context.getString(R.string.device_admin_description)
    }

    companion object {
        private const val TAG = "CustomDeviceAdminReceiver"
    }
}
