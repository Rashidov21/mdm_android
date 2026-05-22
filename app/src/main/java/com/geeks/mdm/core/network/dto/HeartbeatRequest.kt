package com.geeks.mdm.core.network.dto

import com.google.gson.annotations.SerializedName

data class HeartbeatRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("is_locked") val isLocked: Boolean,
    @SerializedName("timestamp_ms") val timestampMs: Long,
    @SerializedName("tamper_flags") val tamperFlags: List<String>? = null
)
