package com.geeks.mdm.core.network.dto

import com.google.gson.annotations.SerializedName

data class LockStatusResponse(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("should_lock") val shouldLock: Boolean,
    @SerializedName("reason") val reason: String?
)
