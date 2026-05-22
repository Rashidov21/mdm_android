package com.geeks.mdm.receivers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import java.security.MessageDigest

/**
 * SIM kartalar identifikatori — telefon raqami saqlanmaydi, faqat hash.
 */
object SimFingerprintProvider {

    private const val NO_PERMISSION_MARKER = "no_phone_permission"
    private const val NO_SIM_MARKER = "no_active_sim"

    fun getFingerprint(context: Context): String {
        if (!hasPhoneStatePermission(context)) {
            return NO_PERMISSION_MARKER
        }

        val appContext = context.applicationContext
        val telephony = appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subscriptionManager =
                appContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeSubs: List<SubscriptionInfo> = try {
                subscriptionManager.activeSubscriptionInfoList ?: emptyList()
            } catch (_: SecurityException) {
                emptyList()
            }

            if (activeSubs.isNotEmpty()) {
                val raw = activeSubs
                    .sortedBy { it.subscriptionId }
                    .joinToString("|") { sub ->
                        buildString {
                            append(sub.subscriptionId)
                            append(":")
                            append(sub.simSlotIndex)
                            append(":")
                            append(sub.carrierName?.toString().orEmpty())
                            append(":")
                            append(sub.countryIso.orEmpty())
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                append(":")
                                append(sub.cardId)
                            }
                        }
                    }
                return sha256(raw)
            }
        }

        val simState = telephony.simState
        if (simState == TelephonyManager.SIM_STATE_READY) {
            return sha256("legacy:${telephony.simOperator}:${telephony.simOperatorName}")
        }

        return sha256(NO_SIM_MARKER)
    }

    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
