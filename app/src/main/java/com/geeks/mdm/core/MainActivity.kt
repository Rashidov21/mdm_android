package com.geeks.mdm.core

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.geeks.mdm.R
import com.geeks.mdm.databinding.ActivityMainBinding

/**
 * GMDM bosh ekrani: holat ko'rsatish, Device Admin yoqish, test qulflash.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var app: GmdmApplication

    private val enableAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as GmdmApplication

        binding.btnEnableAdmin.setOnClickListener { requestDeviceAdmin() }
        binding.btnLockTest.setOnClickListener { onLockTestClicked() }
        binding.btnUnlockTest.setOnClickListener { onUnlockTestClicked() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
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

    private fun onLockTestClicked() {
        when (val result = app.lockCoordinator.lockDevice()) {
            is MdmLockCoordinator.LockResult.Success ->
                Toast.makeText(this, R.string.lock_applied_toast, Toast.LENGTH_SHORT).show()
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

        binding.btnEnableAdmin.isEnabled = !dpc.isAdminActive()
        binding.btnLockTest.isEnabled = dpc.isAdminActive()
        binding.tvHint.visibility = if (dpc.isDeviceOwner()) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }
}
