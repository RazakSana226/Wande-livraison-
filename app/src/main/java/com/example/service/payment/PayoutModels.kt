package com.example.service.payment

import com.example.model.PaymentMethod
import com.example.model.PayoutStatus

/**
 * Payout request model for disbursing earnings to verified drivers via Mobile Money (Orange Money, Moov, Wave).
 *
 * Architecture Rule:
 * User/driver funds are never held directly inside unmonitored Firestore documents.
 * Transactions are recorded in the double-entry ledger, and real fund custody/disbursement
 * is executed by licensed Payment Service Providers (PSP) / Mobile Money operators.
 */
data class PayoutRequest(
    val payoutId: String,
    val driverId: String,
    val driverName: String,
    val driverPhone: String,
    val mobileMoneyNumber: String,
    val amountXof: Int,
    val currency: String = "XOF",
    val provider: PaymentMethod = PaymentMethod.ORANGE_MONEY,
    val deliveryId: String? = null
)

/**
 * Result of driver payout disbursement.
 */
data class PayoutResult(
    val payoutId: String,
    val providerRef: String,
    val status: PayoutStatus,
    val amountXof: Int,
    val feeXof: Int = 0,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
