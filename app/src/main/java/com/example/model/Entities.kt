package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val role: UserRole = UserRole.CLIENT,
    val name: String,
    val phone: String,
    val email: String? = null,
    val photoUrl: String? = null,
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "drivers")
data class DriverEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val phone: String,
    val photoUrl: String? = null,
    val vehicleType: VehicleType = VehicleType.MOTO,
    val vehicleModel: String = "Yamaha 125",
    val vehicleNumber: String = "11-AB-2044-BF",
    val verificationStatus: DriverVerificationStatus = DriverVerificationStatus.PENDING_VERIFICATION,
    val idDocumentUrl: String? = "id_card_sample.jpg",
    val vehiclePhotoUrl: String? = "vehicle_sample.jpg",
    val mobileMoneyNumber: String = "",
    val habitualZone: String = "Ouaga Centre / Ouaga 2000",
    val isOnline: Boolean = false,
    val currentLat: Double = 12.3714,
    val currentLng: Double = -1.5197,
    val rating: Double = 4.9,
    val ratingCount: Int = 24,
    val totalDeliveries: Int = 42,
    val balanceXof: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val trackingNumber: String = "WD-" + (100000..999999).random(),
    val clientId: String,
    val clientName: String,
    val clientPhone: String,
    val driverId: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val driverVehicle: String? = null,
    val driverRating: Double = 5.0,
    // Pickup
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val pickupInstructions: String = "",
    // Destination
    val destinationAddress: String,
    val destinationLat: Double,
    val destinationLng: Double,
    // Recipient
    val recipientName: String,
    val recipientPhone: String,
    // Package
    val packageDescription: String,
    val packageSize: PackageSize = PackageSize.PETIT,
    val specialNotes: String = "",
    // Distance & Pricing (All in integer FCFA/XOF)
    val distanceKm: Double = 3.5,
    val estimatedMinutes: Int = 15,
    val basePriceXof: Int = 500,
    val distancePriceXof: Int = 750,
    val packageSurchargeXof: Int = 0,
    val totalPriceXof: Int = 1250,
    val platformFeeXof: Int = 125, // 10%
    val driverEarningsXof: Int = 1125,
    // Security OTP (Generated at creation, shown to recipient, verified by driver at arrival)
    val otpCode: String = (1000..9999).random().toString(),
    val otpAttempts: Int = 0,
    val status: DeliveryStatus = DeliveryStatus.REQUESTED,
    // Live tracking progress coords
    val currentDriverLat: Double = pickupLat,
    val currentDriverLng: Double = pickupLng,
    // Payment
    val isPaid: Boolean = false,
    val paymentProvider: PaymentProvider = PaymentProvider.ORANGE_MONEY,
    val paymentTransactionId: String? = null,
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null,
    val pickedUpAt: Long? = null,
    val deliveredAt: Long? = null,
    val cancelledAt: Long? = null,
    val cancelReason: String? = null
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val deliveryId: String,
    val clientId: String,
    val amountXof: Int,
    val currency: String = "XOF",
    val provider: PaymentProvider = PaymentProvider.ORANGE_MONEY,
    val providerTransactionId: String = "PAY-" + System.currentTimeMillis(),
    val status: PaymentStatus = PaymentStatus.SUCCESS,
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val deliveryId: String? = null,
    val driverId: String? = null,
    val type: TransactionType,
    val amountXof: Int,
    val description: String,
    val status: String = "COMPLETED",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payouts")
data class PayoutEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val driverId: String,
    val driverName: String,
    val amountXof: Int,
    val phone: String,
    val provider: PaymentProvider = PaymentProvider.ORANGE_MONEY,
    val status: PayoutStatus = PayoutStatus.PAYOUT_PENDING,
    val transactionRef: String = "OUT-" + (10000..99999).random(),
    val createdAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val deliveryId: String,
    val fromUserId: String,
    val toUserId: String,
    val fromName: String,
    val rating: Int, // 1 to 5
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "disputes")
data class DisputeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val deliveryId: String,
    val reportedByUserId: String,
    val reporterName: String,
    val reporterRole: UserRole,
    val reason: String,
    val details: String,
    val status: String = "OPEN", // OPEN, UNDER_REVIEW, RESOLVED, REJECTED
    val resolutionNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "platform_settings")
data class PlatformSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val basePriceXof: Int = 500,
    val pricePerKmXof: Int = 250,
    val minimumPriceXof: Int = 1000,
    val commissionPercent: Int = 10,
    val searchRadiusKm: Double = 8.0,
    val currency: String = "FCFA",
    val mockPaymentEnabled: Boolean = true,
    val mockMapsEnabled: Boolean = false,
    val mockNotificationsEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
