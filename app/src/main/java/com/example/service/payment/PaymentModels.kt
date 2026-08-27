package com.example.service.payment

import com.example.model.PaymentMethod
import com.example.model.PaymentSimulationMode
import com.example.model.PaymentStatus
import java.util.UUID

/**
 * Clean domain request representation for initiating a customer delivery payment.
 * Decouples the UI and repository from any specific payment gateway SDK.
 */
data class PaymentRequest(
    val deliveryId: String,
    val amountXof: Int,
    val currency: String = "XOF",
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String? = null,
    val description: String,
    val provider: PaymentMethod = PaymentMethod.ORANGE_MONEY,
    val simulationMode: PaymentSimulationMode = PaymentSimulationMode.SIMULATE_SUCCESS,
    val returnUrl: String = "wande://payment/return",
    val notifyUrl: String = "https://api.wande.app/api/v1/payments/webhook",
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Result returned upon payment initiation.
 */
sealed class PaymentInitiateResult {
    data class Success(
        val transactionId: String,
        val providerRef: String,
        val amountXof: Int,
        val status: PaymentStatus = PaymentStatus.PAYMENT_SUCCESS,
        val message: String = "Paiement validé avec succès."
    ) : PaymentInitiateResult()

    data class PendingCheckout(
        val transactionId: String,
        val providerRef: String,
        val checkoutUrl: String,
        val paymentToken: String,
        val status: PaymentStatus = PaymentStatus.PAYMENT_PENDING,
        val message: String = "En attente de validation par le client."
    ) : PaymentInitiateResult()

    data class Failed(
        val transactionId: String = UUID.randomUUID().toString(),
        val errorCode: String = "ERR_PAYMENT_FAILED",
        val errorMessage: String,
        val status: PaymentStatus = PaymentStatus.PAYMENT_FAILED
    ) : PaymentInitiateResult()

    data class Expired(
        val transactionId: String = UUID.randomUUID().toString(),
        val errorMessage: String = "Délai de paiement dépassé.",
        val status: PaymentStatus = PaymentStatus.PAYMENT_EXPIRED
    ) : PaymentInitiateResult()
}

/**
 * Result returned upon querying/verifying payment transaction status.
 */
data class PaymentVerificationResult(
    val transactionId: String,
    val providerRef: String,
    val amountXof: Int,
    val status: PaymentStatus,
    val message: String,
    val operatorId: String? = null,
    val rawResponse: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
