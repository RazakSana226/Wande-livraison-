package com.example.service.payment

import com.example.model.PayoutStatus
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Mock Payout Provider for driver disbursements in MVP / Sandbox mode.
 */
class MockPayoutProvider : PayoutProvider {
    override val providerName: String = "Mock Mobile Money Disbursement (Sandbox)"
    override val isMock: Boolean = true

    override suspend fun disburseEarnings(request: PayoutRequest): PayoutResult {
        delay(700) // simulate gateway response latency

        val providerRef = "MM-OUT-${(100000..999999).random()}"
        return PayoutResult(
            payoutId = request.payoutId,
            providerRef = providerRef,
            status = PayoutStatus.PAYOUT_COMPLETED,
            amountXof = request.amountXof,
            feeXof = 0,
            message = "Virement Mobile Money de ${request.amountXof} FCFA envoyé avec succès vers ${request.mobileMoneyNumber} (${request.provider.label})."
        )
    }

    override suspend fun checkPayoutStatus(payoutId: String, providerRef: String): PayoutResult {
        delay(300)
        return PayoutResult(
            payoutId = payoutId,
            providerRef = providerRef,
            status = PayoutStatus.PAYOUT_COMPLETED,
            amountXof = 0,
            feeXof = 0,
            message = "Transaction de virement confirmée par l'opérateur (Mock)."
        )
    }
}
