package com.example.service.email

import com.example.model.OtpPurpose

/**
 * Pluggable Email Transactional Provider Interface
 * Supports Brevo, SendGrid, Mailgun, AWS SES, or Cloud Function Proxy
 */
interface EmailProvider {

    /**
     * Sends account verification link or token to the recipient
     */
    suspend fun sendVerificationEmail(
        email: String,
        recipientName: String,
        verificationToken: String
    ): Result<Boolean>

    /**
     * Sends a 6-digit numeric OTP code for authentication/verification
     */
    suspend fun sendEmailOtp(
        email: String,
        recipientName: String,
        otpCode: String,
        purpose: OtpPurpose
    ): Result<Boolean>

    /**
     * Sends password reset instructions
     */
    suspend fun sendPasswordResetEmail(
        email: String,
        resetToken: String
    ): Result<Boolean>

    /**
     * Sends delivery receipt and confirmation to client
     */
    suspend fun sendDeliveryConfirmation(
        email: String,
        clientName: String,
        trackingNumber: String,
        amountXof: Int
    ): Result<Boolean>

    /**
     * Sends security alert for suspicious activity or password change
     */
    suspend fun sendSecurityAlert(
        email: String,
        alertType: String,
        details: String
    ): Result<Boolean>
}
