package com.example.service.payment

/**
 * Standard Payment Provider interface contract for WÀNDÉ.
 *
 * This abstraction decouples delivery orchestration from any payment gateway.
 * For the MVP, [MockPaymentProvider] is used. In production, [CinetPayPaymentProvider]
 * communicates with a secure server-side proxy without exposing credentials in the client.
 */
interface PaymentProvider {
    val providerName: String
    val isMock: Boolean

    /**
     * Initiates a payment for a delivery request.
     */
    suspend fun initiatePayment(request: PaymentRequest): PaymentInitiateResult

    /**
     * Verifies status of a previously created transaction.
     */
    suspend fun checkPaymentStatus(transactionId: String, providerRef: String?): PaymentVerificationResult

    /**
     * Issues a refund for a cancelled or disputed delivery.
     */
    suspend fun refundPayment(transactionId: String, amountXof: Int, reason: String): PaymentVerificationResult
}
