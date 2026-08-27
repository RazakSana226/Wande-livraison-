package com.example.service.otp

import com.example.model.OtpPurpose

data class EmailOtpResult(
    val id: String,
    val userId: String,
    val email: String,
    val purpose: OtpPurpose,
    val expiresAt: Long,
    val remainingCooldownSeconds: Long = 0,
    val attemptsRemaining: Int = 5,
    val maskedEmail: String = maskEmail(email),
    val rawCodeForDebug: String? = null // only populated in debug/sandbox test mode
)

data class DeliveryOtpResult(
    val id: String,
    val deliveryId: String,
    val rawOtpCode: String, // 4-digit code shown to recipient / client
    val expiresAt: Long,
    val attemptsRemaining: Int = 5
)

sealed class OtpVerificationResult {
    object Success : OtpVerificationResult()
    data class InvalidCode(val attemptsRemaining: Int) : OtpVerificationResult()
    object Expired : OtpVerificationResult()
    object MaxAttemptsExceeded : OtpVerificationResult()
    data class RateLimitExceeded(val cooldownSeconds: Long) : OtpVerificationResult()
    data class Error(val message: String) : OtpVerificationResult()
}

fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return email
    val user = parts[0]
    val domain = parts[1]
    val maskedUser = if (user.length <= 2) {
        user.first() + "***"
    } else {
        user.take(2) + "***" + user.takeLast(1)
    }
    return "$maskedUser@$domain"
}
