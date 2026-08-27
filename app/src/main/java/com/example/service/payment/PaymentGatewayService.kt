package com.example.service.payment

import com.example.model.PaymentMethod
import com.example.model.PaymentSimulationMode
import com.example.model.PaymentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-level Payment & Payout Gateway Manager.
 *
 * Provides a unified entry point for all financial interactions in WÀNDÉ.
 * This guarantees the payment and disbursement architecture is completely decoupled
 * and hot-swappable between [MockPaymentProvider] and [CinetPayPaymentProvider]
 * without touching core delivery lifecycle logic, OTP verification, or driver routing.
 */
class PaymentGatewayService(
    val mockPaymentProvider: MockPaymentProvider = MockPaymentProvider(),
    val cinetPayPaymentProvider: CinetPayPaymentProvider = CinetPayPaymentProvider(),
    val mockPayoutProvider: MockPayoutProvider = MockPayoutProvider(),
    val cinetPayPayoutProvider: CinetPayPayoutProvider = CinetPayPayoutProvider()
) {
    private val _isMockMode = MutableStateFlow(true)
    val isMockMode: StateFlow<Boolean> = _isMockMode.asStateFlow()

    private val _simulationMode = MutableStateFlow(PaymentSimulationMode.SIMULATE_SUCCESS)
    val simulationMode: StateFlow<PaymentSimulationMode> = _simulationMode.asStateFlow()

    val currentPaymentProvider: PaymentProvider
        get() = if (_isMockMode.value) mockPaymentProvider else cinetPayPaymentProvider

    val currentPayoutProvider: PayoutProvider
        get() = if (_isMockMode.value) mockPayoutProvider else cinetPayPayoutProvider

    fun setMockMode(enabled: Boolean) {
        _isMockMode.value = enabled
    }

    fun setSimulationMode(mode: PaymentSimulationMode) {
        _simulationMode.value = mode
        mockPaymentProvider.defaultSimulationMode = mode
    }

    /**
     * Executes delivery payment initiation.
     */
    suspend fun initiateDeliveryPayment(
        deliveryId: String,
        amountXof: Int,
        customerName: String,
        customerPhone: String,
        provider: PaymentMethod,
        description: String = "Livraison WÀNDÉ #$deliveryId",
        customSimulationMode: PaymentSimulationMode? = null
    ): PaymentInitiateResult {
        val request = PaymentRequest(
            deliveryId = deliveryId,
            amountXof = amountXof,
            currency = "XOF",
            customerName = customerName,
            customerPhone = customerPhone,
            description = description,
            provider = provider,
            simulationMode = customSimulationMode ?: _simulationMode.value
        )
        return currentPaymentProvider.initiatePayment(request)
    }

    /**
     * Verifies payment transaction status.
     */
    suspend fun verifyTransaction(transactionId: String, providerRef: String?): PaymentVerificationResult {
        return currentPaymentProvider.checkPaymentStatus(transactionId, providerRef)
    }

    /**
     * Executes driver earnings disbursement.
     */
    suspend fun disburseDriverPayout(request: PayoutRequest): PayoutResult {
        return currentPayoutProvider.disburseEarnings(request)
    }

    /**
     * Refunds client payment.
     */
    suspend fun refundTransaction(transactionId: String, amountXof: Int, reason: String): PaymentVerificationResult {
        return currentPaymentProvider.refundPayment(transactionId, amountXof, reason)
    }
}
