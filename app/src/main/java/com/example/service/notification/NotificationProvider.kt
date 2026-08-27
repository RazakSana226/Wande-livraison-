package com.example.service.notification

import com.example.model.DeliveryEntity
import com.example.model.DriverEntity
import com.example.model.PaymentEntity
import com.example.model.PayoutEntity

/**
 * Universal Pluggable Notification Provider
 * Manages push notifications and event dispatching for Client and Driver lifecycle events.
 */
interface NotificationProvider {
    suspend fun notifyAccountCreated(userId: String, email: String)
    suspend fun notifyEmailVerified(userId: String)
    suspend fun notifyDeliveryCreated(delivery: DeliveryEntity)
    suspend fun notifyDriverAssigned(delivery: DeliveryEntity, driver: DriverEntity)
    suspend fun notifyPriceAcceptedByDriver(delivery: DeliveryEntity, driver: DriverEntity)
    suspend fun notifyCounterOfferReceived(delivery: DeliveryEntity, driverName: String, counterPrice: Int)
    suspend fun notifyCounterOfferAccepted(delivery: DeliveryEntity, driver: DriverEntity, finalPrice: Int)
    suspend fun notifyCounterOfferRejected(delivery: DeliveryEntity, driverId: String)
    suspend fun notifyDriverEnRoute(delivery: DeliveryEntity)
    suspend fun notifyDriverArrived(delivery: DeliveryEntity)
    suspend fun notifyDeliveryCompleted(delivery: DeliveryEntity)
    suspend fun notifyPaymentConfirmed(delivery: DeliveryEntity, payment: PaymentEntity)
    suspend fun notifyNewDeliveryAvailable(delivery: DeliveryEntity)
    suspend fun notifyPayoutCompleted(payout: PayoutEntity)
}
