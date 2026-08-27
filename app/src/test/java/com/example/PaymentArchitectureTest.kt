package com.example

import com.example.model.PaymentMethod
import com.example.model.PaymentSimulationMode
import com.example.model.PaymentStatus
import com.example.model.PayoutStatus
import com.example.service.payment.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PaymentArchitectureTest {

    private lateinit var mockPaymentProvider: MockPaymentProvider
    private lateinit var cinetPayPaymentProvider: CinetPayPaymentProvider
    private lateinit var paymentGateway: PaymentGatewayService
    private lateinit var mockPayoutProvider: MockPayoutProvider

    @Before
    fun setUp() {
        mockPaymentProvider = MockPaymentProvider()
        cinetPayPaymentProvider = CinetPayPaymentProvider()
        mockPayoutProvider = MockPayoutProvider()
        paymentGateway = PaymentGatewayService(
            mockPaymentProvider = mockPaymentProvider,
            cinetPayPaymentProvider = cinetPayPaymentProvider,
            mockPayoutProvider = mockPayoutProvider
        )
    }

    @Test
    fun testMockPayment_SimulateSuccess() = runBlocking {
        val request = PaymentRequest(
            deliveryId = "DEL-101",
            amountXof = 1250,
            customerName = "Amadou Ouédraogo",
            customerPhone = "+226 70 12 34 56",
            description = "Course Ouaga 2000 -> Aéroport",
            provider = PaymentMethod.ORANGE_MONEY,
            simulationMode = PaymentSimulationMode.SIMULATE_SUCCESS
        )

        val result = mockPaymentProvider.initiatePayment(request)
        assertTrue("Result should be Success", result is PaymentInitiateResult.Success)
        val success = result as PaymentInitiateResult.Success
        assertEquals(PaymentStatus.PAYMENT_SUCCESS, success.status)
        assertEquals(1250, success.amountXof)
        assertTrue(success.providerRef.startsWith("CIN-MOCK-"))
    }

    @Test
    fun testMockPayment_SimulatePending() = runBlocking {
        val request = PaymentRequest(
            deliveryId = "DEL-102",
            amountXof = 2500,
            customerName = "Fatimata Kaboré",
            customerPhone = "+226 78 55 44 33",
            description = "Course Tampouy -> Centre-ville",
            provider = PaymentMethod.MOOV_MONEY,
            simulationMode = PaymentSimulationMode.SIMULATE_PENDING
        )

        val result = mockPaymentProvider.initiatePayment(request)
        assertTrue("Result should be PendingCheckout", result is PaymentInitiateResult.PendingCheckout)
        val pending = result as PaymentInitiateResult.PendingCheckout
        assertEquals(PaymentStatus.PAYMENT_PENDING, pending.status)
        assertTrue(pending.checkoutUrl.isNotEmpty())
    }

    @Test
    fun testMockPayment_SimulateFailed() = runBlocking {
        val request = PaymentRequest(
            deliveryId = "DEL-103",
            amountXof = 5000,
            customerName = "Ousmane Sawadogo",
            customerPhone = "+226 76 99 88 77",
            description = "Livraison Grand Colis",
            provider = PaymentMethod.WAVE,
            simulationMode = PaymentSimulationMode.SIMULATE_FAILED
        )

        val result = mockPaymentProvider.initiatePayment(request)
        assertTrue("Result should be Failed", result is PaymentInitiateResult.Failed)
        val failed = result as PaymentInitiateResult.Failed
        assertEquals(PaymentStatus.PAYMENT_FAILED, failed.status)
        assertEquals("ERR_INSUFFICIENT_FUNDS", failed.errorCode)
    }

    @Test
    fun testMockPayment_SimulateExpired() = runBlocking {
        val request = PaymentRequest(
            deliveryId = "DEL-104",
            amountXof = 1000,
            customerName = "Aïssata Diallo",
            customerPhone = "+226 71 00 11 22",
            description = "Livraison documents",
            provider = PaymentMethod.CINETPAY,
            simulationMode = PaymentSimulationMode.SIMULATE_EXPIRED
        )

        val result = mockPaymentProvider.initiatePayment(request)
        assertTrue("Result should be Expired", result is PaymentInitiateResult.Expired)
        val expired = result as PaymentInitiateResult.Expired
        assertEquals(PaymentStatus.PAYMENT_EXPIRED, expired.status)
    }

    @Test
    fun testPaymentGateway_HotSwapProvider() = runBlocking {
        // Starts in mock mode
        assertTrue(paymentGateway.isMockMode.value)
        assertEquals("Mock Sandbox Gateway (WÀNDÉ Test Mode)", paymentGateway.currentPaymentProvider.providerName)

        // Switch to CinetPay production provider
        paymentGateway.setMockMode(false)
        assertFalse(paymentGateway.isMockMode.value)
        assertEquals("CinetPay (Orange Money, Moov, Wave, Carte Bancaire)", paymentGateway.currentPaymentProvider.providerName)
        assertFalse(paymentGateway.currentPaymentProvider.isMock)

        // Switch back to Mock mode
        paymentGateway.setMockMode(true)
        assertTrue(paymentGateway.isMockMode.value)
        assertTrue(paymentGateway.currentPaymentProvider.isMock)
    }

    @Test
    fun testPayoutArchitecture_DriverDisbursement() = runBlocking {
        val payoutRequest = PayoutRequest(
            payoutId = "PAYOUT-001",
            driverId = "drv_1",
            driverName = "Ibrahim Traoré",
            driverPhone = "+226 70 99 88 77",
            mobileMoneyNumber = "+226 70 99 88 77",
            amountXof = 10000,
            provider = PaymentMethod.ORANGE_MONEY
        )

        val payoutResult = paymentGateway.disburseDriverPayout(payoutRequest)
        assertEquals(PayoutStatus.PAYOUT_COMPLETED, payoutResult.status)
        assertEquals(10000, payoutResult.amountXof)
        assertTrue(payoutResult.providerRef.startsWith("MM-OUT-"))
    }
}
