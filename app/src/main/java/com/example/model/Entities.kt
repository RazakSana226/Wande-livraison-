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
    val isEmailVerified: Boolean = false,
    val firebaseUid: String? = null,
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
    val idDocumentType: IdentityDocumentType = IdentityDocumentType.CNI,
    val idDocumentFrontUrl: String? = "id_cni_front_preview.jpg",
    val idDocumentBackUrl: String? = "id_cni_back_preview.jpg",
    val selfieUrl: String? = "selfie_driver_preview.jpg",
    val livenessScore: Float = 0.98f,
    val birthDate: String = "12/05/1996",
    val city: String = "Ouagadougou",
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
    val rejectionReason: String? = null,
    val kycSubmittedAt: Long? = System.currentTimeMillis(),
    val kycReviewedAt: Long? = null,
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
    // Simplified Offer & Negotiation Architecture
    val customerInitialOffer: Int = 1000,
    val driverCounterOffer: Int? = null,
    val finalDeliveryPrice: Int = 1000,
    val commission: Int = 100, // 10% of finalDeliveryPrice
    val customerTotal: Int = 1100, // finalDeliveryPrice + commission
    val driverEarnings: Int = 1000, // finalDeliveryPrice
    val counterOfferDriverId: String? = null,
    val counterOfferDriverName: String? = null,
    val counterOfferDriverPhone: String? = null,
    val counterOfferDriverRating: Double = 4.9,
    val counterOfferDriverDeliveries: Int = 127,
    val offerStatus: String = "SEARCHING_DRIVER",
    // Backward compatibility aliases
    val basePriceXof: Int = 500,
    val distancePriceXof: Int = 750,
    val packageSurchargeXof: Int = 0,
    val totalPriceXof: Int = customerTotal,
    val platformFeeXof: Int = commission,
    val driverEarningsXof: Int = driverEarnings,
    // Security OTP (Generated at creation, shown to recipient, verified by driver at arrival)
    val otpCode: String = (1000..9999).random().toString(),
    val otpAttempts: Int = 0,
    val status: DeliveryStatus = DeliveryStatus.SEARCHING_DRIVER,
    // Live tracking progress coords
    val currentDriverLat: Double = pickupLat,
    val currentDriverLng: Double = pickupLng,
    // Payment
    val isPaid: Boolean = false,
    val paymentProvider: PaymentMethod = PaymentMethod.ORANGE_MONEY,
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
    val provider: PaymentMethod = PaymentMethod.ORANGE_MONEY,
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
    val provider: PaymentMethod = PaymentMethod.ORANGE_MONEY,
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

@Entity(tableName = "email_otps")
data class EmailOtpEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val email: String,
    val hashedOtp: String,
    val salt: String,
    val purpose: OtpPurpose = OtpPurpose.EMAIL_VERIFICATION,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 10 * 60 * 1000L, // 10 minutes max
    val attempts: Int = 0,
    val maxAttempts: Int = 5,
    val verifiedAt: Long? = null,
    val isInvalidated: Boolean = false
)

@Entity(tableName = "delivery_otps")
data class DeliveryOtpEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val deliveryId: String,
    val orderId: String,
    val clientId: String,
    val driverId: String? = null,
    val hashedOtp: String,
    val salt: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 3600 * 1000L, // 24h validity
    val attempts: Int = 0,
    val maxAttempts: Int = 5,
    val status: DeliveryOtpStatus = DeliveryOtpStatus.PENDING,
    val verifiedAt: Long? = null,
    val verifiedByDriverId: String? = null
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val userEmail: String? = null,
    val userRole: UserRole? = null,
    val action: AuditAction,
    val details: String,
    val ipAddress: String = "127.0.0.1",
    val deviceInfo: String = "WÀNDÉ Mobile Client",
    val severity: AuditSeverity = AuditSeverity.INFO,
    val timestamp: Long = System.currentTimeMillis()
)
