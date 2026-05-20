package com.geeks.mdm.core.network.dto

import com.google.gson.annotations.SerializedName

data class HeartbeatResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("server_time_ms") val serverTimeMs: Long?
)
