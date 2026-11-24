package com.example.data.telephony

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

sealed interface TelephonyDispatchResult {
    data object Launched : TelephonyDispatchResult
    data class NoHandlerApp(val targetLabel: String) : TelephonyDispatchResult
    data class InvalidDestination(val reason: String) : TelephonyDispatchResult
}

/**
 * Clean abstraction for Android native SMS and cellular telephony integration.
 * Uses zero-permission standard Android Intents (ACTION_DIAL / ACTION_SENDTO) so the app
 * remains Play Policy compliant while integrating directly with the system dialer & SMS app.
 */
interface NativeTelephonyBridge {
    fun hasTelephonyHardware(): Boolean
    fun launchCellularDialer(phoneNumber: String): TelephonyDispatchResult
    fun launchSmsComposer(phoneNumber: String, messageBody: String): TelephonyDispatchResult
}

class AndroidNativeTelephonyBridge(
    private val context: Context
) : NativeTelephonyBridge {

    override fun hasTelephonyHardware(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    override fun launchCellularDialer(phoneNumber: String): TelephonyDispatchResult {
        val normalized = normalizeDialableNumber(phoneNumber)
            ?: return TelephonyDispatchResult.InvalidDestination("Group chats do not have a direct phone number")

        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$normalized")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            if (dialIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(dialIntent)
                TelephonyDispatchResult.Launched
            } else {
                TelephonyDispatchResult.NoHandlerApp(normalized)
            }
        }.getOrElse {
            TelephonyDispatchResult.NoHandlerApp(normalized)
        }
    }

    override fun launchSmsComposer(
        phoneNumber: String,
        messageBody: String
    ): TelephonyDispatchResult {
        val normalized = normalizeDialableNumber(phoneNumber)
            ?: return TelephonyDispatchResult.InvalidDestination("Group SMS requires individual phone numbers")

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$normalized")
            putExtra("sms_body", messageBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            if (smsIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(smsIntent)
                TelephonyDispatchResult.Launched
            } else {
                TelephonyDispatchResult.NoHandlerApp(normalized)
            }
        }.getOrElse {
            TelephonyDispatchResult.NoHandlerApp(normalized)
        }
    }

    private fun normalizeDialableNumber(raw: String): String? {
        if (raw.equals("Group", ignoreCase = true) || raw.isBlank()) return null
        val cleaned = raw.filter { it.isDigit() || it == '+' }
        return cleaned.takeIf { it.isNotBlank() }
    }
}
