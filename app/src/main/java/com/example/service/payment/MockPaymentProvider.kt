package com.example.service.payment

import com.example.model.PaymentSimulationMode
import com.example.model.PaymentStatus
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Mock Payment Provider implementation for the WÀNDÉ MVP.
 *
 * Simulates real-world Mobile Money & card gateway responses without
 * requiring credentials or external network connectivity.
 *
 * Supports deterministic simulation of:
 * - [PaymentStatus.PAYMENT_PENDING]
 * - [PaymentStatus.PAYMENT_SUCCESS]
 * - [PaymentStatus.PAYMENT_FAILED]
 * - [PaymentStatus.PAYMENT_EXPIRED]
 */
class MockPaymentProvider(
    var defaultSimulationMode: PaymentSimulationMode = PaymentSimulationMode.SIMULATE_SUCCESS
) : PaymentProvider {

    override val providerName: String = "Mock Sandbox Gateway (WÀNDÉ Test Mode)"
    override val isMock: Boolean = true

    // In-memory registry of mock transactions for query/status tracking during test session
    private val transactionStore = ConcurrentHashMap<String, PaymentVerificationResult>()

    override suspend fun initiatePayment(request: PaymentRequest): PaymentInitiateResult {
        // Simulate network roundtrip latency
        delay(600)

        val txId = "TX-MOCK-${System.currentTimeMillis()}-${(1000..9999).random()}"
        val providerRef = "CIN-MOCK-${UUID.randomUUID().toString().take(8).uppercase()}"

        val effectiveMode = if (request.simulationMode != PaymentSimulationMode.SIMULATE_SUCCESS) {
            request.simulationMode
        } else {
            defaultSimulationMode
        }

        return when (effectiveMode) {
            PaymentSimulationMode.SIMULATE_SUCCESS -> {
                val result = PaymentVerificationResult(
                    transactionId = txId,
                    providerRef = providerRef,
                    amountXof = request.amountXof,
                    status = PaymentStatus.PAYMENT_SUCCESS,
                    message = "Paiement ${request.provider.label} de ${request.amountXof} FCFA validé avec succès (Mode Test)."
                )
                transactionStore[txId] = result
                PaymentInitiateResult.Success(
                    transactionId = txId,
                    providerRef = providerRef,
                    amountXof = request.amountXof,
                    status = PaymentStatus.PAYMENT_SUCCESS,
                    message = result.message
                )
            }

            PaymentSimulationMode.SIMULATE_PENDING -> {
                val result = PaymentVerificationResult(
                    transactionId = txId,
                    providerRef = providerRef,
                    amountXof = request.amountXof,
                    status = PaymentStatus.PAYMENT_PENDING,
                    message = "Validation USSD Mobile Money en attente sur le ${request.customerPhone}."
                )
                transactionStore[txId] = result
                PaymentInitiateResult.PendingCheckout(
                    transactionId = txId,
                    providerRef = providerRef,
                    checkoutUrl = "https://sandbox-checkout.wande.local/pay/$txId",
                    paymentToken = "tok_mock_${UUID.randomUUID()}",
                    status = PaymentStatus.PAYMENT_PENDING,
                    message = result.message
                )
            }

            PaymentSimulationMode.SIMULATE_FAILED -> {
                val result = PaymentVerificationResult(
                    transactionId = txId,
                    providerRef = providerRef,
                    amountXof = request.amountXof,
                    status = PaymentStatus.PAYMENT_FAILED,
                    message = "Paiement refusé : Solde insuffisant sur le compte ${request.provider.label}."
                )
                transactionStore[txId] = result
                PaymentInitiateResult.Failed(
                    transactionId = txId,
                    errorCode = "ERR_INSUFFICIENT_FUNDS",
                    errorMessage = result.message,
                    status = PaymentStatus.PAYMENT_FAILED
                )
            }

            PaymentSimulationMode.SIMULATE_EXPIRED -> {
                val result = PaymentVerificationResult(
                    transactionId = txId,
                    providerRef = providerRef,
                    amountXof = request.amountXof,
                    status = PaymentStatus.PAYMENT_EXPIRED,
                    message = "La session de paiement a expiré sans confirmation du client."
                )
                transactionStore[txId] = result
                PaymentInitiateResult.Expired(
                    transactionId = txId,
                    errorMessage = result.message,
                    status = PaymentStatus.PAYMENT_EXPIRED
                )
            }
        }
    }

    override suspend fun checkPaymentStatus(transactionId: String, providerRef: String?): PaymentVerificationResult {
        delay(400)
        return transactionStore[transactionId] ?: PaymentVerificationResult(
            transactionId = transactionId,
            providerRef = providerRef ?: "UNKNOWN",
            amountXof = 0,
            status = PaymentStatus.PAYMENT_FAILED,
            message = "Transaction introuvable dans le bac à sable mock."
        )
    }

    override suspend fun refundPayment(transactionId: String, amountXof: Int, reason: String): PaymentVerificationResult {
        delay(400)
        val original = transactionStore[transactionId]
        val refundedResult = PaymentVerificationResult(
            transactionId = transactionId,
            providerRef = original?.providerRef ?: "REF-MOCK",
            amountXof = amountXof,
            status = PaymentStatus.REFUNDED,
            message = "Remboursement de $amountXof FCFA effectué avec succès : $reason"
        )
        transactionStore[transactionId] = refundedResult
        return refundedResult
    }

    /**
     * Test helper to manually simulate a webhook transition for testing async workflows.
     */
    fun simulateWebhookStatusUpdate(transactionId: String, newStatus: PaymentStatus) {
        val current = transactionStore[transactionId] ?: return
        transactionStore[transactionId] = current.copy(
            status = newStatus,
            message = "Mise à jour statut simulée : ${newStatus.label}"
        )
    }
}
