package com.example.service.payment

/**
 * Payout Provider interface for disbursing funds to verified delivery drivers.
 */
interface PayoutProvider {
    val providerName: String
    val isMock: Boolean

    /**
     * Executes disbursement of driver earnings to Mobile Money.
     */
    suspend fun disburseEarnings(request: PayoutRequest): PayoutResult

    /**
     * Checks status of a payout transaction.
     */
    suspend fun checkPayoutStatus(payoutId: String, providerRef: String): PayoutResult
}
