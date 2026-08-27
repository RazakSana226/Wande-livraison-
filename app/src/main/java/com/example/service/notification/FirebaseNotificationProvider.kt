package com.example.service.notification

import android.util.Log
import com.example.model.DeliveryEntity
import com.example.model.DriverEntity
import com.example.model.PaymentEntity
import com.example.model.PayoutEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseNotificationProvider : NotificationProvider {

    private val tag = "FirebaseNotification"

    override suspend fun notifyAccountCreated(userId: String, email: String) {
        withContext(Dispatchers.IO) {
            Log.i(tag, "Account created notification: $email ($userId)")
        }
    }

    override suspend fun notifyEmailVerified(userId: String) {
        withContext(Dispatchers.IO) {
            Log.i(tag, "Email verified notification for user: $userId")
        }
    }

    override suspend fun notifyDeliveryCreated(delivery: DeliveryEntity) {
        withContext(Dispatchers.IO) {
            Log.i(tag, "New delivery request: #${delivery.trackingNumber} (${delivery.packageDescription})")
        }
    }

    override suspend fun notifyDriverAssigned(delivery: DeliveryEntity, driver: DriverEntity) {
        withContext(Dispatchers.IO) {
            Log.i(tag, "Driver assigned: ${driver.name} for course #${delivery.trackingNumber}")
        }
    }

    override suspend fun notifyPriceAcceptedByDriver(delivery: DeliveryEntity, driver: DriverEntity) {
        withContext(Dispatchers.IO) {
            // Client: "Un livreur a accepté votre prix." / "Votre livraison est confirmée."
            // Driver: "Vous avez accepté la livraison."
            Log.i(tag, "[Push Client] Un livreur a accepté votre prix (${delivery.finalDeliveryPrice} FCFA). Votre livraison #${delivery.trackingNumber} est confirmée.")
            Log.i(tag, "[Push Driver] Vous avez accepté la livraison #${delivery.trackingNumber}.")
        }
    }

    override suspend fun notifyCounterOfferReceived(delivery: DeliveryEntity, driverName: String, counterPrice: Int) {
        withContext(Dispatchers.IO) {
            // Client: "Un livreur vous propose {price} FCFA."
            Log.i(tag, "[Push Client] Un livreur ($driverName) vous propose $counterPrice FCFA pour la course #${delivery.trackingNumber}.")
        }
    }

    override suspend fun notifyCounterOfferAccepted(delivery: DeliveryEntity, driver: DriverEntity, finalPrice: Int) {
        withContext(Dispatchers.IO) {
            // Driver: "Le client a accepté votre proposition de {price} FCFA."
            // Client: "Votre livraison est confirmée."
            Log.i(tag, "[Push Driver] Le client a accepté votre proposition de $finalPrice FCFA pour la course #${delivery.trackingNumber}.")
            Log.i(tag, "[Push Client] Votre livraison #${delivery.trackingNumber} est confirmée à $finalPrice FCFA.")
        }
    }

    override suspend fun notifyCounterOfferRejected(delivery: DeliveryEntity, driverId: String) {
        withContext(Dispatchers.IO) {
            // Driver: "Le client a refusé votre proposition."
            // Client: "Votre proposition a été refusée."
            Log.i(tag, "[Push Driver] Le client a refusé votre proposition pour la course #${delivery.trackingNumber}.")
            Log.i(tag, "[Push Client] Cette proposition a été refusée. Vous pouvez attendre d'autres livreurs ou modifier votre offre.")
        }
    }

    override suspend fun notifyDriverEnRoute(delivery: DeliveryEntity) {
        withContext(Dispatchers.IO) {
            Log.i(tag, "Driver en route for course #${delivery.trackingNumber}")
        }
    }

    override suspend fun notifyDriverArrived(delivery: DeliveryEntity) {
        withContext(Dispatchers.IO) {
            Log.i(tag, "Driver arrived at destination for course #${delivery.trackingNumber}. Recipient OTP ready.")
        }
    }

    override suspend fun notifyDeliveryCompleted(delivery: DeliveryEntity) {
        withContext(Dispatchers.IO) {
            Log.i(tag, "Delivery completed successfully: #${delivery.trackingNumber} (${delivery.totalPriceXof} FCFA)")
        }
    }

    override suspend fun notifyPaymentConfirmed(delivery: DeliveryEntity, payment: PaymentEntity) {
        withContext(Dispatchers.IO) {
            Log.i(tag, "Payment confirmed: ${payment.amountXof} FCFA for course #${delivery.trackingNumber}")
        }
    }

    override suspend fun notifyNewDeliveryAvailable(delivery: DeliveryEntity) {
        withContext(Dispatchers.IO) {
            Log.i(tag, "Dispatch broadcast to online drivers for course #${delivery.trackingNumber}")
        }
    }

    override suspend fun notifyPayoutCompleted(payout: PayoutEntity) {
        withContext(Dispatchers.IO) {
            Log.i(tag, "Payout completed: ${payout.amountXof} FCFA to ${payout.phone}")
        }
    }
}
