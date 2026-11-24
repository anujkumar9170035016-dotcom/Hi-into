package com.example.data.onboarding

import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.random.Random

sealed interface OtpRequestResult {
    data class Sent(
        val challengeId: String,
        val formattedPhone: String,
        val previewCodeForDemo: String
    ) : OtpRequestResult

    data class InvalidNumber(val message: String) : OtpRequestResult
}

sealed interface OtpVerifyResult {
    data class Verified(val formattedPhone: String) : OtpVerifyResult
    data class Failed(val message: String) : OtpVerifyResult
}

/**
 * Clean phone OTP authentication boundary.
 * Replace [LocalOtpAuthService] with Firebase Phone Auth or custom SMS OTP backend
 * without changing onboarding ViewModels or UI.
 */
interface PhoneAuthService {
    suspend fun requestOtp(countryDialCode: String, rawPhoneNumber: String): OtpRequestResult
    suspend fun verifyOtp(challengeId: String, enteredCode: String): OtpVerifyResult
}

class LocalOtpAuthService : PhoneAuthService {

    private var activeChallengeId: String? = null
    private var activeCode: String? = null
    private var activePhone: String? = null

    override suspend fun requestOtp(
        countryDialCode: String,
        rawPhoneNumber: String
    ): OtpRequestResult {
        val digits = rawPhoneNumber.filter { it.isDigit() }
        if (digits.length < 7 || digits.length > 15) {
            return OtpRequestResult.InvalidNumber("Enter a valid mobile number (7–15 digits)")
        }
        delay(450)
        val challengeId = UUID.randomUUID().toString()
        val code = (100000 + Random.nextInt(900000)).toString()
        val formatted = "${countryDialCode.trim()} $digits"

        activeChallengeId = challengeId
        activeCode = code
        activePhone = formatted

        return OtpRequestResult.Sent(
            challengeId = challengeId,
            formattedPhone = formatted,
            previewCodeForDemo = code
        )
    }

    override suspend fun verifyOtp(challengeId: String, enteredCode: String): OtpVerifyResult {
        val cleaned = enteredCode.filter { it.isDigit() }
        if (cleaned.length != 6) {
            return OtpVerifyResult.Failed("Enter the complete 6-digit verification code")
        }
        delay(350)
        return if (challengeId == activeChallengeId && cleaned == activeCode) {
            OtpVerifyResult.Verified(activePhone.orEmpty())
        } else {
            OtpVerifyResult.Failed("Incorrect 6-digit code. Check the code and try again.")
        }
    }
}
