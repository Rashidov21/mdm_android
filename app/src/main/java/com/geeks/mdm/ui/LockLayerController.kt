package com.geeks.mdm.ui

import android.content.Context
import android.util.Log
import com.geeks.mdm.core.DevicePolicyController

/**
 * Layer 2, 3, 4 ni bir vaqtda yoqish/o'chirish.
 */
object LockLayerController {

    private const val TAG = "LockLayerController"

    data class LayerActivationResult(
        val layer2Started: Boolean,
        val layer3Shown: Boolean,
        val layer4Active: Boolean
    )

    fun activateUiLayers(
        context: Context,
        devicePolicyController: DevicePolicyController
    ): LayerActivationResult {
        devicePolicyController.enableLockTaskMode()

        LockScreenActivity.start(context)
        val layer2 = true

        val layer3 = OverlayLockManager.show(context)
        if (!layer3) {
            Log.w(TAG, "Layer 3: overlay ruxsati kerak")
        }

        val layer4 = LockUiPermissionHelper.isAccessibilityServiceEnabled(context) &&
            MdmAccessibilityService.isRunning
        if (!layer4) {
            Log.w(TAG, "Layer 4: accessibility yoqilmagan")
        }

        Log.i(
            TAG,
            "UI qatlamlar: L2=$layer2 L3=$layer3 L4=$layer4"
        )

        return LayerActivationResult(
            layer2Started = layer2,
            layer3Shown = layer3,
            layer4Active = layer4
        )
    }

    fun deactivateUiLayers(context: Context, devicePolicyController: DevicePolicyController) {
        OverlayLockManager.hide()
        LockScreenActivity.finishIfRunning(context)
        devicePolicyController.disableLockTaskMode()
        Log.i(TAG, "UI qatlamlar o'chirildi")
    }
}
