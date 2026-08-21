package com.example.data.repository

import com.example.data.local.WandeDao
import com.example.model.*
import com.example.service.GeminiMapsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class WandeRepository(
    private val dao: WandeDao,
    private val appScope: CoroutineScope
) {
    private val deliveryLock = Mutex()

    // Flows
    val allDeliveries: Flow<List<DeliveryEntity>> = dao.getAllDeliveries()
    val allDrivers: Flow<List<DriverEntity>> = dao.getAllDrivers()
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()
    val allPayments: Flow<List<PaymentEntity>> = dao.getAllPayments()
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allPayouts: Flow<List<PayoutEntity>> = dao.getAllPayouts()
    val allDisputes: Flow<List<DisputeEntity>> = dao.getAllDisputes()
    val allReviews: Flow<List<ReviewEntity>> = dao.getAllReviews()
    val platformSettings: Flow<PlatformSettingsEntity?> = dao.getSettings()

    fun getDeliveriesForClient(clientId: String): Flow<List<DeliveryEntity>> =
        dao.getDeliveriesForClient(clientId)

    fun getDeliveriesForDriver(driverId: String): Flow<List<DeliveryEntity>> =
        dao.getDeliveriesForDriver(driverId)

    fun getOpenDeliveries(): Flow<List<DeliveryEntity>> =
        dao.getOpenDeliveries()

    fun getDeliveryFlow(deliveryId: String): Flow<DeliveryEntity?> =
        dao.getDeliveryFlowById(deliveryId)

    suspend fun getDriverById(driverId: String): DriverEntity? =
        dao.getDriverById(driverId)

    suspend fun getDriverByUserId(userId: String): DriverEntity? =
        dao.getDriverByUserId(userId)

    suspend fun getUserById(userId: String): UserEntity? =
        dao.getUserById(userId)

    /**
     * Server-side fare calculation rule
     * Base fee + (distance * price/km) + package size surcharge, respecting minimum price
     */
    suspend fun calculateDeliveryPrice(
        distanceKm: Double,
        packageSize: PackageSize
    ): PricingBreakdown = withContext(Dispatchers.IO) {
        val settings = dao.getSettingsSnapshot() ?: PlatformSettingsEntity()
        val baseFee = settings.basePriceXof
        val distanceFee = (distanceKm * settings.pricePerKmXof).toInt()
        val surcharge = packageSize.surchargeXof
        val calculatedTotal = baseFee + distanceFee + surcharge
        val finalTotal = maxOf(calculatedTotal, settings.minimumPriceXof)

        // Commission (10% standard)
        val platformFee = (finalTotal * settings.commissionPercent / 100)
        val driverEarnings = finalTotal - platformFee

        PricingBreakdown(
            basePriceXof = baseFee,
            distancePriceXof = distanceFee,
            packageSurchargeXof = surcharge,
            totalPriceXof = finalTotal,
            platformFeeXof = platformFee,
            driverEarningsXof = driverEarnings,
            distanceKm = distanceKm,
            estimatedMinutes = GeminiMapsService.estimateMinutes(distanceKm)
        )
    }

    /**
     * Create & initiate a new delivery request
     */
    suspend fun createDelivery(
        clientId: String,
        clientName: String,
        clientPhone: String,
        pickupPoint: LatLngPoint,
        pickupInstructions: String,
        destPoint: LatLngPoint,
        recipientName: String,
        recipientPhone: String,
        packageDesc: String,
        packageSize: PackageSize,
        specialNotes: String,
        paymentProvider: PaymentProvider
    ): Result<DeliveryEntity> = withContext(Dispatchers.IO) {
        val distanceKm = GeminiMapsService.calculateDistanceKm(
            pickupPoint.latitude, pickupPoint.longitude,
            destPoint.latitude, destPoint.longitude
        )
        val pricing = calculateDeliveryPrice(distanceKm, packageSize)
        val secureOtp = (1000..9999).random().toString()

        val delivery = DeliveryEntity(
            clientId = clientId,
            clientName = clientName,
            clientPhone = clientPhone,
            pickupAddress = pickupPoint.address,
            pickupLat = pickupPoint.latitude,
            pickupLng = pickupPoint.longitude,
            pickupInstructions = pickupInstructions,
            destinationAddress = destPoint.address,
            destinationLat = destPoint.latitude,
            destinationLng = destPoint.longitude,
            recipientName = recipientName,
            recipientPhone = recipientPhone,
            packageDescription = packageDesc,
            packageSize = packageSize,
            specialNotes = specialNotes,
            distanceKm = distanceKm,
            estimatedMinutes = pricing.estimatedMinutes,
            basePriceXof = pricing.basePriceXof,
            distancePriceXof = pricing.distancePriceXof,
            packageSurchargeXof = pricing.packageSurchargeXof,
            totalPriceXof = pricing.totalPriceXof,
            platformFeeXof = pricing.platformFeeXof,
            driverEarningsXof = pricing.driverEarningsXof,
            otpCode = secureOtp,
            status = DeliveryStatus.SEARCHING_DRIVER,
            isPaid = false,
            paymentProvider = paymentProvider,
            currentDriverLat = pickupPoint.latitude,
            currentDriverLng = pickupPoint.longitude
        )

        dao.insertDelivery(delivery)

        // Process payment initialization
        processPayment(
            deliveryId = delivery.id,
            clientId = clientId,
            amountXof = pricing.totalPriceXof,
            provider = paymentProvider
        )

        Result.success(delivery)
    }

    /**
     * Driver Course Acceptance with strict atomic lock to prevent double acceptance
     */
    suspend fun acceptDelivery(
        deliveryId: String,
        driverId: String
    ): Result<DeliveryEntity> = withContext(Dispatchers.IO) {
        deliveryLock.withLock {
            val delivery = dao.getDeliveryById(deliveryId)
                ?: return@withContext Result.failure(Exception("Livraison non trouvée."))

            if (delivery.status != DeliveryStatus.SEARCHING_DRIVER && delivery.status != DeliveryStatus.REQUESTED) {
                return@withContext Result.failure(Exception("Cette course a déjà été attribuée à un autre livreur."))
            }

            val driver = dao.getDriverById(driverId)
                ?: return@withContext Result.failure(Exception("Livreur introuvable."))

            if (driver.verificationStatus != DriverVerificationStatus.VERIFIED) {
                return@withContext Result.failure(Exception("Votre compte livreur n'est pas encore validé par l'administration."))
            }

            val updatedDelivery = delivery.copy(
                driverId = driver.id,
                driverName = driver.name,
                driverPhone = driver.phone,
                driverVehicle = "${driver.vehicleType.label} - ${driver.vehicleModel}",
                driverRating = driver.rating,
                status = DeliveryStatus.DRIVER_ASSIGNED,
                acceptedAt = System.currentTimeMillis(),
                currentDriverLat = driver.currentLat,
                currentDriverLng = driver.currentLng
            )

            dao.updateDelivery(updatedDelivery)
            Result.success(updatedDelivery)
        }
    }

    /**
     * Driver lifecycle progression:
     * DRIVER_ASSIGNED -> DRIVER_ARRIVING -> PACKAGE_PICKED_UP -> IN_TRANSIT -> DRIVER_ARRIVED
     */
    suspend fun updateDeliveryProgress(
        deliveryId: String,
        newStatus: DeliveryStatus,
        driverId: String
    ): Result<DeliveryEntity> = withContext(Dispatchers.IO) {
        val delivery = dao.getDeliveryById(deliveryId)
            ?: return@withContext Result.failure(Exception("Livraison introuvable."))

        if (delivery.driverId != driverId) {
            return@withContext Result.failure(Exception("Non autorisé pour cette course."))
        }

        val updated = when (newStatus) {
            DeliveryStatus.DRIVER_ARRIVING -> delivery.copy(status = DeliveryStatus.DRIVER_ARRIVING)
            DeliveryStatus.PACKAGE_PICKED_UP -> delivery.copy(
                status = DeliveryStatus.PACKAGE_PICKED_UP,
                pickedUpAt = System.currentTimeMillis()
            )
            DeliveryStatus.IN_TRANSIT -> delivery.copy(status = DeliveryStatus.IN_TRANSIT)
            DeliveryStatus.DRIVER_ARRIVED -> delivery.copy(status = DeliveryStatus.DRIVER_ARRIVED)
            else -> delivery.copy(status = newStatus)
        }

        dao.updateDelivery(updated)
        Result.success(updated)
    }

    /**
     * Recipient OTP Validation at dropoff point
     * If valid: marks DELIVERED, records timestamp, credits driver wallet & logs WÀNDÉ commission
     */
    suspend fun verifyAndCompleteDelivery(
        deliveryId: String,
        driverId: String,
        inputOtp: String
    ): Result<DeliveryEntity> = withContext(Dispatchers.IO) {
        deliveryLock.withLock {
            val delivery = dao.getDeliveryById(deliveryId)
                ?: return@withContext Result.failure(Exception("Livraison introuvable."))

            if (delivery.driverId != driverId) {
                return@withContext Result.failure(Exception("Livreur non assigné à cette course."))
            }

            if (delivery.status == DeliveryStatus.DELIVERED) {
                return@withContext Result.failure(Exception("Cette livraison a déjà été finalisée."))
            }

            if (delivery.otpAttempts >= 5) {
                return@withContext Result.failure(Exception("Trop de tentatives OTP incorrectes. Contactez le support."))
            }

            if (delivery.otpCode != inputOtp.trim()) {
                val nextAttempts = delivery.otpAttempts + 1
                dao.updateDelivery(delivery.copy(otpAttempts = nextAttempts))
                return@withContext Result.failure(Exception("Code OTP incorrect (${5 - nextAttempts} essais restants)."))
            }

            // Correct OTP - Finalize delivery!
            val completed = delivery.copy(
                status = DeliveryStatus.DELIVERED,
                deliveredAt = System.currentTimeMillis()
            )
            dao.updateDelivery(completed)

            // Credit driver earnings to internal balance ledger
            dao.creditDriverBalance(driverId, delivery.driverEarningsXof)

            // Record ledger transactions
            dao.insertTransaction(
                TransactionEntity(
                    deliveryId = delivery.id,
                    driverId = driverId,
                    type = TransactionType.DELIVERY_EARNING,
                    amountXof = delivery.driverEarningsXof,
                    description = "Gain de livraison #${delivery.trackingNumber}",
                    status = "COMPLETED"
                )
            )
            dao.insertTransaction(
                TransactionEntity(
                    deliveryId = delivery.id,
                    driverId = driverId,
                    type = TransactionType.PLATFORM_FEE,
                    amountXof = delivery.platformFeeXof,
                    description = "Commission WÀNDÉ 10% #${delivery.trackingNumber}",
                    status = "COMPLETED"
                )
            )

            // Update driver total deliveries count
            val driver = dao.getDriverById(driverId)
            driver?.let {
                dao.updateDriver(it.copy(totalDeliveries = it.totalDeliveries + 1))
            }

            Result.success(completed)
        }
    }

    /**
     * Process payment with server-side confirmation & idempotency
     */
    suspend fun processPayment(
        deliveryId: String,
        clientId: String,
        amountXof: Int,
        provider: PaymentProvider
    ): Result<PaymentEntity> = withContext(Dispatchers.IO) {
        val payment = PaymentEntity(
            deliveryId = deliveryId,
            clientId = clientId,
            amountXof = amountXof,
            currency = "XOF",
            provider = provider,
            providerTransactionId = "WPAY-${System.currentTimeMillis()}-${(100..999).random()}",
            status = PaymentStatus.SUCCESS
        )

        dao.insertPayment(payment)

        val delivery = dao.getDeliveryById(deliveryId)
        delivery?.let {
            dao.updateDelivery(
                it.copy(
                    isPaid = true,
                    paymentProvider = provider,
                    paymentTransactionId = payment.providerTransactionId
                )
            )
        }

        Result.success(payment)
    }

    /**
     * Request driver payout to Mobile Money
     */
    suspend fun requestDriverPayout(
        driverId: String,
        amountXof: Int,
        mobileMoneyNumber: String,
        provider: PaymentProvider
    ): Result<PayoutEntity> = withContext(Dispatchers.IO) {
        val driver = dao.getDriverById(driverId)
            ?: return@withContext Result.failure(Exception("Livreur introuvable."))

        if (amountXof < 1000) {
            return@withContext Result.failure(Exception("Le montant minimum de retrait est de 1 000 FCFA."))
        }

        if (driver.balanceXof < amountXof) {
            return@withContext Result.failure(Exception("Solde insuffisant (Solde actuel : ${driver.balanceXof} FCFA)."))
        }

        // Debit driver balance immediately
        dao.debitDriverBalance(driverId, amountXof)

        val payout = PayoutEntity(
            driverId = driverId,
            driverName = driver.name,
            amountXof = amountXof,
            phone = mobileMoneyNumber.ifEmpty { driver.phone },
            provider = provider,
            status = PayoutStatus.PAYOUT_PENDING
        )
        dao.insertPayout(payout)

        // Log transaction
        dao.insertTransaction(
            TransactionEntity(
                driverId = driverId,
                type = TransactionType.PAYOUT,
                amountXof = -amountXof,
                description = "Demande de virement Mobile Money vers ${payout.phone}",
                status = "PENDING"
            )
        )

        Result.success(payout)
    }

    /**
     * Admin approve driver payout
     */
    suspend fun adminProcessPayout(payoutId: String, approve: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val payouts = dao.getAllPayouts().firstOrNull() ?: emptyList()
        val payout = payouts.find { it.id == payoutId }
            ?: return@withContext Result.failure(Exception("Demande de retrait introuvable."))

        if (approve) {
            dao.updatePayout(payout.copy(status = PayoutStatus.PAYOUT_COMPLETED, processedAt = System.currentTimeMillis()))
        } else {
            dao.updatePayout(payout.copy(status = PayoutStatus.PAYOUT_FAILED, processedAt = System.currentTimeMillis()))
            // Refund driver balance
            dao.creditDriverBalance(payout.driverId, payout.amountXof)
            dao.insertTransaction(
                TransactionEntity(
                    driverId = payout.driverId,
                    type = TransactionType.REFUND,
                    amountXof = payout.amountXof,
                    description = "Remboursement suite à rejet du virement #${payout.transactionRef}",
                    status = "COMPLETED"
                )
            )
        }
        Result.success(Unit)
    }

    /**
     * Register or Update Driver Onboarding Profile
     */
    suspend fun registerDriver(
        userId: String,
        name: String,
        phone: String,
        vehicleType: VehicleType,
        vehicleModel: String,
        vehicleNumber: String,
        habitualZone: String,
        mobileMoneyNumber: String
    ): Result<DriverEntity> = withContext(Dispatchers.IO) {
        val existing = dao.getDriverByUserId(userId)
        val driver = existing?.copy(
            name = name,
            phone = phone,
            vehicleType = vehicleType,
            vehicleModel = vehicleModel,
            vehicleNumber = vehicleNumber,
            habitualZone = habitualZone,
            mobileMoneyNumber = mobileMoneyNumber,
            verificationStatus = DriverVerificationStatus.PENDING_VERIFICATION
        ) ?: DriverEntity(
            userId = userId,
            name = name,
            phone = phone,
            vehicleType = vehicleType,
            vehicleModel = vehicleModel,
            vehicleNumber = vehicleNumber,
            habitualZone = habitualZone,
            mobileMoneyNumber = mobileMoneyNumber,
            verificationStatus = DriverVerificationStatus.PENDING_VERIFICATION
        )

        dao.insertDriver(driver)
        Result.success(driver)
    }

    /**
     * Driver Online/Offline toggle
     */
    suspend fun setDriverOnline(driverId: String, isOnline: Boolean) = withContext(Dispatchers.IO) {
        dao.setDriverOnline(driverId, isOnline)
    }

    /**
     * Admin Driver Verification
     */
    suspend fun adminSetDriverStatus(driverId: String, status: DriverVerificationStatus) = withContext(Dispatchers.IO) {
        val driver = dao.getDriverById(driverId) ?: return@withContext
        dao.updateDriver(driver.copy(verificationStatus = status))
    }

    /**
     * Submit rating & review
     */
    suspend fun submitReview(
        deliveryId: String,
        fromUserId: String,
        fromName: String,
        toUserId: String,
        rating: Int,
        comment: String
    ) = withContext(Dispatchers.IO) {
        val review = ReviewEntity(
            deliveryId = deliveryId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            fromName = fromName,
            rating = rating,
            comment = comment
        )
        dao.insertReview(review)
    }

    /**
     * Cancel delivery
     */
    suspend fun cancelDelivery(deliveryId: String, reason: String, cancelledByRole: UserRole) = withContext(Dispatchers.IO) {
        val delivery = dao.getDeliveryById(deliveryId) ?: return@withContext
        val updated = delivery.copy(
            status = DeliveryStatus.CANCELLED,
            cancelledAt = System.currentTimeMillis(),
            cancelReason = "[$cancelledByRole] $reason"
        )
        dao.updateDelivery(updated)
    }

    /**
     * Report Dispute
     */
    suspend fun reportDispute(
        deliveryId: String,
        userId: String,
        userName: String,
        role: UserRole,
        reason: String,
        details: String
    ) = withContext(Dispatchers.IO) {
        val dispute = DisputeEntity(
            deliveryId = deliveryId,
            reportedByUserId = userId,
            reporterName = userName,
            reporterRole = role,
            reason = reason,
            details = details
        )
        dao.insertDispute(dispute)
        dao.updateDeliveryStatus(deliveryId, DeliveryStatus.DISPUTED)
    }

    /**
     * Update Platform Settings
     */
    suspend fun updateSettings(settings: PlatformSettingsEntity) = withContext(Dispatchers.IO) {
        dao.saveSettings(settings)
    }

    /**
     * Simulated GPS driver tracker update along route
     */
    fun simulateLiveDriverMovement(deliveryId: String, waypoints: List<Pair<Double, Double>>) {
        appScope.launch(Dispatchers.IO) {
            for (point in waypoints) {
                dao.updateDeliveryDriverCoordinates(deliveryId, point.first, point.second)
                delay(1200) // 1.2s smooth step
            }
        }
    }
}

data class PricingBreakdown(
    val basePriceXof: Int,
    val distancePriceXof: Int,
    val packageSurchargeXof: Int,
    val totalPriceXof: Int,
    val platformFeeXof: Int,
    val driverEarningsXof: Int,
    val distanceKm: Double,
    val estimatedMinutes: Int
)
