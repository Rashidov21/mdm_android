package com.geeks.mdm.core.network

import com.geeks.mdm.core.network.dto.HeartbeatRequest
import com.geeks.mdm.core.network.dto.HeartbeatResponse
import com.geeks.mdm.core.network.dto.LockStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Kelajakdagi backend API shartnomasi.
 * API_BASE_URL bo'sh bo'lsa ApiClient bu interfeysni yaratmaydi.
 */
interface MdmApiService {

    @POST("api/v1/devices/heartbeat")
    suspend fun sendHeartbeat(
        @Body request: HeartbeatRequest
    ): Response<HeartbeatResponse>

    @GET("api/v1/devices/{deviceId}/lock-status")
    suspend fun getLockStatus(
        @Path("deviceId") deviceId: String
    ): Response<LockStatusResponse>
}
