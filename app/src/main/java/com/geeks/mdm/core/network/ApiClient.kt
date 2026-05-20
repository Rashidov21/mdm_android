package com.geeks.mdm.core.network

import com.geeks.mdm.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit klienti. API_BASE_URL bo'sh bo'lsa service null qaytaradi.
 */
class ApiClient {

    val isEnabled: Boolean = BuildConfig.API_ENABLED

    val baseUrl: String = BuildConfig.API_BASE_URL

    val service: MdmApiService? by lazy {
        if (!isEnabled || baseUrl.isBlank()) {
            null
        } else {
            createRetrofit(normalizeBaseUrl(baseUrl)).create(MdmApiService::class.java)
        }
    }

    private fun createRetrofit(url: String): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun normalizeBaseUrl(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }
}
