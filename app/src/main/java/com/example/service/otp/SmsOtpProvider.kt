package com.example.service.otp

import com.example.model.OtpPurpose

/**
 * SMS OTP Provider Stub (Future Extension)
 * Implements OtpProvider to allow zero-effort activation when SMS credits / Twilio / Infobip are provisioned.
 */
class SmsOtpProvider(
    private val delegate: SecureEmailOtpProvider
) : OtpProvider {

    override suspend fun generateEmailOtp(
        userId: String,
        email: String,
        purpose: OtpPurpose,
        recipientName: String
    ): Result<EmailOtpResult> = delegate.generateEmailOtp(userId, email, purpose, recipientName)

    override suspend fun verifyEmailOtp(
        userId: String,
        email: String,
        inputCode: String,
        purpose: OtpPurpose
    ): Result<OtpVerificationResult> = delegate.verifyEmailOtp(userId, email, inputCode, purpose)

    override suspend fun generateDeliveryOtp(
        deliveryId: String,
        orderId: String,
        clientId: String,
        driverId: String?
    ): Result<DeliveryOtpResult> = delegate.generateDeliveryOtp(deliveryId, orderId, clientId, driverId)

    override suspend fun verifyDeliveryOtp(
        deliveryId: String,
        driverId: String,
        inputCode: String
    ): Result<OtpVerificationResult> = delegate.verifyDeliveryOtp(deliveryId, driverId, inputCode)
}
