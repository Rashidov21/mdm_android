package com.geeks.mdm.core

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.geeks.mdm.R
import com.geeks.mdm.core.network.MdmServerSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.geeks.mdm.databinding.ActivityMainBinding
import com.geeks.mdm.receivers.MdmReceiverOrchestrator
import com.geeks.mdm.receivers.SimFingerprintProvider
import com.geeks.mdm.services.BatteryOptimizationHelper
import com.geeks.mdm.services.MdmServiceLauncher
import com.geeks.mdm.ui.LockScreenActivity
import com.geeks.mdm.ui.LockUiPermissionHelper
import java.text.DateFormat
import java.util.Date

/**
 * GMDM bosh ekrani: holat, ruxsatlar, Device Admin va 4 qatlamli test qulflash.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var app: GmdmApplication

    private val enableAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshStatus()
    }

    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshStatus()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                R.string.notification_permission_denied,
                Toast.LENGTH_LONG
            ).show()
        }
        MdmServiceLauncher.start(this)
        refreshStatus()
    }

    private val phoneStatePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                R.string.phone_permission_denied,
                Toast.LENGTH_LONG
            ).show()
        } else {
            MdmReceiverOrchestrator.onSimStateChanged(this)
        }
        refreshStatus()
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as GmdmApplication

        requestNotificationPermissionIfNeeded()
        requestPhoneStatePermissionIfNeeded()
        MdmServiceLauncher.start(this)

        binding.btnEnableAdmin.setOnClickListener { requestDeviceAdmin() }
        binding.btnBatterySettings.setOnClickListener { openBatterySettings() }
        binding.btnOverlayPermission.setOnClickListener { openOverlaySettings() }
        binding.btnAccessibilitySettings.setOnClickListener { openAccessibilitySettings() }
        binding.btnSyncNow.setOnClickListener { onSyncNowClicked() }
        binding.btnLockTest.setOnClickListener { onLockTestClicked() }
        binding.btnUnlockTest.setOnClickListener { onUnlockTestClicked() }
    }

    override fun onResume() {
        super.onResume()
        MdmServiceLauncher.start(this)
        if (app.stateStore.isLocked) {
            app.lockCoordinator.applyLockIfNeeded()
            LockScreenActivity.start(this)
        }
        refreshStatus()
    }

    private fun openOverlaySettings() {
        overlayPermissionLauncher.launch(LockUiPermissionHelper.createOverlayPermissionIntent(this))
    }

    private fun openAccessibilitySettings() {
        startActivity(LockUiPermissionHelper.createAccessibilitySettingsIntent())
    }

    private fun requestPhoneStatePermissionIfNeeded() {
        if (SimFingerprintProvider.hasPhoneStatePermission(this)) return
        phoneStatePermissionLauncher.launch(android.Manifest.permission.READ_PHONE_STATE)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openBatterySettings() {
        val batteryIntent = BatteryOptimizationHelper.createIgnoreBatteryOptimizationsIntent(this)
        if (batteryIntent != null) {
            batteryOptimizationLauncher.launch(batteryIntent)
            return
        }
        val oemIntent = BatteryOptimizationHelper.createOemBatteryIntent(this)
        if (!BatteryOptimizationHelper.launchIntent(this, oemIntent)) {
            BatteryOptimizationHelper.launchIntent(
                this,
                BatteryOptimizationHelper.createAppDetailsIntent(this)
            )
        }
    }

    private fun requestDeviceAdmin() {
        if (app.devicePolicyController.isAdminActive()) {
            Toast.makeText(this, R.string.admin_enabled_toast, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                app.devicePolicyController.getAdminComponent()
            )
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.device_admin_description)
            )
        }
        enableAdminLauncher.launch(intent)
    }

    private fun onSyncNowClicked() {
        if (!app.apiClient.isEnabled) {
            Toast.makeText(this, R.string.sync_api_disabled, Toast.LENGTH_LONG).show()
            return
        }

        setSyncButtonEnabled(false)
        Toast.makeText(this, R.string.sync_in_progress, Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                MdmServerSyncManager.syncNow(this@MainActivity)
            }
            setSyncButtonEnabled(true)
            refreshStatus()

            when (result) {
                is MdmServerSyncManager.SyncResult.ApiDisabled ->
                    Toast.makeText(
                        this@MainActivity,
                        R.string.sync_api_disabled,
                        Toast.LENGTH_LONG
                    ).show()

                is MdmServerSyncManager.SyncResult.Success ->
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.sync_success, result.message),
                        Toast.LENGTH_LONG
                    ).show()

                is MdmServerSyncManager.SyncResult.Failure ->
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.sync_failed, result.message),
                        Toast.LENGTH_LONG
                    ).show()
            }
        }
    }

    private fun setSyncButtonEnabled(enabled: Boolean) {
        binding.btnSyncNow.isEnabled = enabled && app.apiClient.isEnabled
    }

    private fun onLockTestClicked() {
        when (val result = app.lockCoordinator.lockDevice()) {
            is MdmLockCoordinator.LockResult.Success ->
                Toast.makeText(this, R.string.lock_layers_applied_toast, Toast.LENGTH_SHORT).show()
            is MdmLockCoordinator.LockResult.Partial ->
                Toast.makeText(
                    this,
                    getString(R.string.lock_partial_failed, result.message),
                    Toast.LENGTH_LONG
                ).show()
            is MdmLockCoordinator.LockResult.Failed ->
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
        }
        refreshStatus()
    }

    private fun onUnlockTestClicked() {
        app.lockCoordinator.unlockDevice()
        Toast.makeText(this, R.string.unlock_applied_toast, Toast.LENGTH_SHORT).show()
        refreshStatus()
    }

    private fun refreshStatus() {
        val dpc = app.devicePolicyController
        val store = app.stateStore
        val apiClient = app.apiClient

        val adminLabel = if (dpc.isAdminActive()) {
            getString(R.string.state_yes)
        } else {
            getString(R.string.state_no)
        }

        val ownerLabel = if (dpc.isDeviceOwner()) {
            getString(R.string.state_yes)
        } else {
            getString(R.string.state_no)
        }

        val lockedLabel = if (store.isLocked) {
            getString(R.string.state_yes)
        } else {
            getString(R.string.state_no)
        }

        val apiLabel = if (apiClient.isEnabled) {
            apiClient.baseUrl
        } else {
            getString(R.string.api_not_configured)
        }

        val serviceLabel = if (MdmServiceLauncher.isServiceRunning()) {
            getString(R.string.state_enabled)
        } else {
            getString(R.string.state_disabled)
        }

        val batteryLabel = if (BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            getString(R.string.battery_unrestricted)
        } else {
            getString(R.string.battery_restricted)
        }

        val overlayLabel = if (LockUiPermissionHelper.canDrawOverlays(this)) {
            getString(R.string.state_yes)
        } else {
            getString(R.string.state_no)
        }

        val accessibilityLabel = if (LockUiPermissionHelper.isAccessibilityServiceEnabled(this)) {
            getString(R.string.state_enabled)
        } else {
            getString(R.string.state_disabled)
        }

        binding.tvDeviceAdmin.text =
            getString(R.string.status_device_admin, adminLabel)
        binding.tvDeviceOwner.text =
            getString(R.string.status_device_owner, ownerLabel)
        binding.tvLocked.text =
            getString(R.string.status_locked, lockedLabel)
        binding.tvApi.text =
            getString(R.string.status_api, apiLabel)
        binding.tvDeviceId.text =
            getString(R.string.status_device_id, store.deviceId)
        binding.tvForegroundService.text =
            getString(R.string.status_foreground_service, serviceLabel)
        binding.tvBatteryOptimization.text =
            getString(R.string.status_battery_optimization, batteryLabel)
        binding.tvOverlayPermission.text =
            getString(R.string.status_overlay_permission, overlayLabel)
        binding.tvAccessibility.text =
            getString(R.string.status_accessibility, accessibilityLabel)

        val simLabel = if (SimFingerprintProvider.hasPhoneStatePermission(this)) {
            getString(R.string.sim_monitoring_active)
        } else {
            getString(R.string.sim_monitoring_no_permission)
        }

        val tamperLabel = store.lastTamperEvent.ifEmpty {
            getString(R.string.tamper_none)
        }

        binding.tvSimMonitoring.text =
            getString(R.string.status_sim_monitoring, simLabel)
        val syncLabel = if (store.lastSyncEpochMs > 0L) {
            DateFormat.getDateTimeInstance().format(Date(store.lastSyncEpochMs))
        } else {
            getString(R.string.sync_never)
        }

        binding.tvLastSync.text = getString(R.string.status_last_sync, syncLabel)
        binding.tvLastTamper.text =
            getString(R.string.status_last_tamper, tamperLabel)

        binding.btnEnableAdmin.isEnabled = !dpc.isAdminActive()
        setSyncButtonEnabled(true)
        binding.btnLockTest.isEnabled = dpc.isAdminActive()
        binding.btnOverlayPermission.isEnabled = !LockUiPermissionHelper.canDrawOverlays(this)
        binding.tvHint.visibility = if (dpc.isDeviceOwner()) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }
}
