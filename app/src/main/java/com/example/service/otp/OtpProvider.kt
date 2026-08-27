package com.example.service.otp

import com.example.model.OtpPurpose

/**
 * Universal Pluggable OTP Provider Interface
 * Allows interchangeable usage of Email OTP, SMS OTP, and WhatsApp OTP
 */
interface OtpProvider {

    /**
     * Generates a secure numeric 6-digit OTP for Email, stored salted & hashed.
     * Enforces rate-limiting (max 3 in 15min, cooldown).
     */
    suspend fun generateEmailOtp(
        userId: String,
        email: String,
        purpose: OtpPurpose,
        recipientName: String = "Client WÀNDÉ"
    ): Result<EmailOtpResult>

    /**
     * Verifies the 6-digit email OTP against stored cryptographic hash.
     * Decrements attempts and enforces expiration and invalidation.
     */
    suspend fun verifyEmailOtp(
        userId: String,
        email: String,
        inputCode: String,
        purpose: OtpPurpose
    ): Result<OtpVerificationResult>

    /**
     * Generates a 4-6 digit recipient Delivery OTP (Code de remise)
     * Stored with salted hash in local / backend DB.
     */
    suspend fun generateDeliveryOtp(
        deliveryId: String,
        orderId: String,
        clientId: String,
        driverId: String? = null
    ): Result<DeliveryOtpResult>

    /**
     * Verifies the Delivery OTP submitted by the driver at dropoff.
     */
    suspend fun verifyDeliveryOtp(
        deliveryId: String,
        driverId: String,
        inputCode: String
    ): Result<OtpVerificationResult>
}
