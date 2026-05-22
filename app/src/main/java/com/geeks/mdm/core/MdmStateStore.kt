package com.geeks.mdm.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * Shifrlangan local holat: qulf, sync vaqti, qurilma identifikatori.
 */
class MdmStateStore(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var isLocked: Boolean
        get() = prefs.getBoolean(KEY_IS_LOCKED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_IS_LOCKED, value).apply()
        }

    var lastSyncEpochMs: Long
        get() = prefs.getLong(KEY_LAST_SYNC_EPOCH_MS, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_SYNC_EPOCH_MS, value).apply()
        }

    var lockReason: String
        get() = prefs.getString(KEY_LOCK_REASON, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_LOCK_REASON, value).apply()
        }

    val deviceId: String
        get() {
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (!existing.isNullOrBlank()) {
                return existing
            }
            val generated = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
            return generated
        }

    var simFingerprint: String
        get() = prefs.getString(KEY_SIM_FINGERPRINT, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_SIM_FINGERPRINT, value).apply()
        }

    var lastBootEpochMs: Long
        get() = prefs.getLong(KEY_LAST_BOOT_EPOCH_MS, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_BOOT_EPOCH_MS, value).apply()
        }

    var adminDisabledAtEpochMs: Long
        get() = prefs.getLong(KEY_ADMIN_DISABLED_EPOCH_MS, 0L)
        set(value) {
            prefs.edit().putLong(KEY_ADMIN_DISABLED_EPOCH_MS, value).apply()
        }

    var lastTamperEvent: String
        get() = prefs.getString(KEY_LAST_TAMPER_EVENT, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_LAST_TAMPER_EVENT, value).apply()
        }

    fun recordTamperEvent(event: String) {
        lastTamperEvent = "${System.currentTimeMillis()}:$event"
    }

    fun clearLockState() {
        prefs.edit()
            .putBoolean(KEY_IS_LOCKED, false)
            .putString(KEY_LOCK_REASON, "")
            .apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "gmdm_encrypted_state"
        private const val KEY_IS_LOCKED = "is_locked"
        private const val KEY_LAST_SYNC_EPOCH_MS = "last_sync_epoch_ms"
        private const val KEY_LOCK_REASON = "lock_reason"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_SIM_FINGERPRINT = "sim_fingerprint"
        private const val KEY_LAST_BOOT_EPOCH_MS = "last_boot_epoch_ms"
        private const val KEY_ADMIN_DISABLED_EPOCH_MS = "admin_disabled_epoch_ms"
        private const val KEY_LAST_TAMPER_EVENT = "last_tamper_event"
    }
}
