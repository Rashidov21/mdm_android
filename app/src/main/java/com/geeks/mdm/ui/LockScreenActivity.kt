package com.geeks.mdm.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.geeks.mdm.BuildConfig
import com.geeks.mdm.R
import com.geeks.mdm.core.GmdmApplication
import com.geeks.mdm.core.DevicePolicyController
import com.geeks.mdm.databinding.ActivityLockScreenBinding

/**
 * Layer 2: To'liq ekran Kiosk Activity — startLockTask bilan Home/Recents blok.
 */
class LockScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockScreenBinding
    private lateinit var devicePolicyController: DevicePolicyController

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_FINISH_LOCK_SCREEN) {
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as GmdmApplication
        devicePolicyController = app.devicePolicyController

        binding.tvLockDeviceId.text = getString(
            R.string.lock_screen_device_id,
            app.stateStore.deviceId
        )

        applyFullscreen()
        devicePolicyController.enableLockTaskMode()
        registerFinishReceiver()
        setupDebugUnlockIfNeeded(app)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Bloklangan
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        isActive = true
        enterLockTaskIfPossible()
        OverlayLockManager.show(this)
    }

    override fun onPause() {
        isActive = false
        super.onPause()
    }

    override fun onDestroy() {
        unregisterFinishReceiver()
        if (isFinishing) {
            try {
                stopLockTask()
            } catch (_: Exception) {
            }
            devicePolicyController.disableLockTaskMode()
        }
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun enterLockTaskIfPossible() {
        if (!devicePolicyController.isDeviceOwner()) return
        try {
            startLockTask()
        } catch (e: Exception) {
            // Device Owner bo'lmasa oddiy fullscreen ishlaydi
        }
    }

    private fun applyFullscreen() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun setupDebugUnlockIfNeeded(app: GmdmApplication) {
        if (!BuildConfig.DEBUG) return
        var tapCount = 0
        binding.tvLockTitle.setOnClickListener {
            tapCount++
            if (tapCount >= 7) {
                app.lockCoordinator.unlockDevice()
                finishAndRemoveTask()
            }
        }
    }

    private fun registerFinishReceiver() {
        val filter = IntentFilter(ACTION_FINISH_LOCK_SCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(finishReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(finishReceiver, filter)
        }
    }

    private fun unregisterFinishReceiver() {
        try {
            unregisterReceiver(finishReceiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    companion object {
        const val ACTION_FINISH_LOCK_SCREEN = "com.geeks.mdm.action.FINISH_LOCK_SCREEN"

        @Volatile
        var isActive: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }

        fun finishIfRunning(context: Context) {
            if (!isActive) return
            context.sendBroadcast(
                Intent(ACTION_FINISH_LOCK_SCREEN).setPackage(context.packageName)
            )
        }
    }
}
