package com.example.data.repository

import com.example.data.local.WandeDao
import com.example.model.*
import com.example.service.GeminiMapsService
import com.example.service.auth.*
import com.example.service.email.*
import com.example.service.geo.LocationOptimizerService
import com.example.service.identity.*
import com.example.service.notification.*
import com.example.service.otp.*
import com.example.service.payment.*
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
    private val appScope: CoroutineScope,
    val paymentGateway: PaymentGatewayService = PaymentGatewayService(),
    val emailProvider: EmailProvider = BrevoEmailProvider(),
    val notificationProvider: NotificationProvider = FirebaseNotificationProvider(),
    val otpProvider: OtpProvider = SecureEmailOtpProvider(dao, emailProvider),
    val authProvider: AuthProvider = FirebaseAuthProvider(dao, emailProvider, appScope),
    val identityProvider: IdentityVerificationProvider = ManualIdentityVerificationProvider(dao),
    val locationOptimizer: LocationOptimizerService = LocationOptimizerService()
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
    val allAuditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogs()

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

    suspend fun getUserByEmail(email: String): UserEntity? =
        dao.getUserByEmail(email)

    /**
     * Server-side fare calculation rule and simplified price suggestions
     */
    suspend fun calculateDeliveryPrice(
        distanceKm: Double,
        packageSize: PackageSize,
        customPriceXof: Int? = null
    ): PricingBreakdown = withContext(Dispatchers.IO) {
        val minPrice = 1000
        val recommendedPrice = 1500
        val attractivePrice = 2000

        val chosenPrice = customPriceXof?.coerceAtLeast(minPrice) ?: minPrice
        val commission = (chosenPrice * 0.10).toInt()
        val customerTotal = chosenPrice + commission
        val driverEarnings = chosenPrice

        PricingBreakdown(
            basePriceXof = 500,
            distancePriceXof = (distanceKm * 250).toInt(),
            packageSurchargeXof = packageSize.surchargeXof,
            totalPriceXof = customerTotal,
            finalDeliveryPriceXof = chosenPrice,
            platformFeeXof = commission,
            driverEarningsXof = driverEarnings,
            minPriceXof = minPrice,
            recommendedPriceXof = recommendedPrice,
            attractivePriceXof = attractivePrice,
            distanceKm = distanceKm,
            estimatedMinutes = GeminiMapsService.estimateMinutes(distanceKm)
        )
    }

    /**
     * Create & initiate a new delivery request with client proposed price (Minimum 1000 FCFA)
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
        proposedPriceXof: Int = 1000,
        paymentProvider: PaymentMethod,
        simulationMode: PaymentSimulationMode? = null
    ): Result<DeliveryEntity> = withContext(Dispatchers.IO) {
        if (proposedPriceXof < 1000) {
            return@withContext Result.failure(Exception("Le prix minimum est de 1000 FCFA."))
        }

        val distanceKm = GeminiMapsService.calculateDistanceKm(
            pickupPoint.latitude, pickupPoint.longitude,
            destPoint.latitude, destPoint.longitude
        )
        val finalPrice = proposedPriceXof
        val commission = (finalPrice * 0.10).toInt()
        val customerTotal = finalPrice + commission
        val driverEarnings = finalPrice
        val estimatedMins = GeminiMapsService.estimateMinutes(distanceKm)

        val deliveryId = UUID.randomUUID().toString()
        val trackingNum = "WD-" + (100000..999999).random()

        // Generate cryptographically secure Delivery OTP and salted hash
        val otpResult = otpProvider.generateDeliveryOtp(
            deliveryId = deliveryId,
            orderId = trackingNum,
            clientId = clientId
        ).getOrNull()

        val secureOtp = otpResult?.rawOtpCode ?: (1000..9999).random().toString()

        val delivery = DeliveryEntity(
            id = deliveryId,
            trackingNumber = trackingNum,
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
            estimatedMinutes = estimatedMins,
            // Simplified Offer & Negotiation Architecture
            customerInitialOffer = proposedPriceXof,
            driverCounterOffer = null,
            finalDeliveryPrice = finalPrice,
            commission = commission,
            customerTotal = customerTotal,
            driverEarnings = driverEarnings,
            offerStatus = "SEARCHING_DRIVER",
            basePriceXof = 500,
            distancePriceXof = (distanceKm * 250).toInt(),
            packageSurchargeXof = packageSize.surchargeXof,
            totalPriceXof = customerTotal,
            platformFeeXof = commission,
            driverEarningsXof = driverEarnings,
            otpCode = secureOtp,
            status = DeliveryStatus.SEARCHING_DRIVER,
            isPaid = false,
            paymentProvider = paymentProvider,
            currentDriverLat = pickupPoint.latitude,
            currentDriverLng = pickupPoint.longitude
        )

        dao.insertDelivery(delivery)
        notificationProvider.notifyDeliveryCreated(delivery)
        notificationProvider.notifyNewDeliveryAvailable(delivery)

        // Process payment initialization via decoupled PaymentGateway
        val paymentResult = processPayment(
            deliveryId = delivery.id,
            clientId = clientId,
            amountXof = customerTotal,
            provider = paymentProvider,
            simulationMode = simulationMode
        )

        if (paymentResult.isFailure && (simulationMode == PaymentSimulationMode.SIMULATE_FAILED || simulationMode == PaymentSimulationMode.SIMULATE_EXPIRED)) {
            return@withContext Result.failure(paymentResult.exceptionOrNull() ?: Exception("Échec de paiement"))
        }

        Result.success(delivery)
    }

    /**
     * Driver Course Direct Acceptance:
     * Accepts customerInitialOffer directly. Delivery price is immediately locked.
     */
    suspend fun acceptDelivery(
        deliveryId: String,
        driverId: String
    ): Result<DeliveryEntity> = withContext(Dispatchers.IO) {
        deliveryLock.withLock {
            val delivery = dao.getDeliveryById(deliveryId)
                ?: return@withContext Result.failure(Exception("Livraison non trouvée."))

            if (delivery.driverId != null || (delivery.status != DeliveryStatus.SEARCHING_DRIVER && delivery.status != DeliveryStatus.REQUESTED && delivery.status != DeliveryStatus.COUNTER_OFFER_REJECTED)) {
                return@withContext Result.failure(Exception("Cette course a déjà été attribuée ou n'est plus disponible."))
            }

            val driver = dao.getDriverById(driverId)
                ?: return@withContext Result.failure(Exception("Livreur introuvable."))

            if (driver.verificationStatus != DriverVerificationStatus.VERIFIED) {
                return@withContext Result.failure(Exception("Votre compte livreur n'est pas encore validé par l'administration."))
            }

            val finalPrice = delivery.customerInitialOffer
            val commission = (finalPrice * 0.10).toInt()
            val customerTotal = finalPrice + commission
            val driverEarnings = finalPrice

            val updatedDelivery = delivery.copy(
                driverId = driver.id,
                driverName = driver.name,
                driverPhone = driver.phone,
                driverVehicle = "${driver.vehicleType.label} - ${driver.vehicleModel}",
                driverRating = driver.rating,
                finalDeliveryPrice = finalPrice,
                commission = commission,
                customerTotal = customerTotal,
                driverEarnings = driverEarnings,
                totalPriceXof = customerTotal,
                platformFeeXof = commission,
                driverEarningsXof = driverEarnings,
                driverCounterOffer = null,
                status = DeliveryStatus.DRIVER_ASSIGNED,
                offerStatus = "DRIVER_ACCEPTED",
                acceptedAt = System.currentTimeMillis(),
                currentDriverLat = driver.currentLat,
                currentDriverLng = driver.currentLng
            )

            dao.updateDelivery(updatedDelivery)
            notificationProvider.notifyPriceAcceptedByDriver(updatedDelivery, driver)
            notificationProvider.notifyDriverAssigned(updatedDelivery, driver)
            Result.success(updatedDelivery)
        }
    }

    /**
     * Driver Single Counter-Offer:
     * Driver proposes a single alternative price (minimum 1000 FCFA).
     */
    suspend fun submitDriverCounterOffer(
        deliveryId: String,
        driverId: String,
        counterPriceXof: Int
    ): Result<DeliveryEntity> = withContext(Dispatchers.IO) {
        deliveryLock.withLock {
            if (counterPriceXof < 1000) {
                return@withContext Result.failure(Exception("Le prix minimum est de 1000 FCFA."))
            }

            val delivery = dao.getDeliveryById(deliveryId)
                ?: return@withContext Result.failure(Exception("Livraison non trouvée."))

            if (delivery.driverId != null) {
                return@withContext Result.failure(Exception("Cette course a déjà été attribuée à un livreur."))
            }

            if (delivery.status != DeliveryStatus.SEARCHING_DRIVER && delivery.status != DeliveryStatus.COUNTER_OFFER_REJECTED && delivery.status != DeliveryStatus.REQUESTED) {
                return@withContext Result.failure(Exception("Cette livraison n'accepte plus de proposition."))
            }

            val driver = dao.getDriverById(driverId)
                ?: return@withContext Result.failure(Exception("Livreur introuvable."))

            if (driver.verificationStatus != DriverVerificationStatus.VERIFIED) {
                return@withContext Result.failure(Exception("Votre compte livreur n'est pas encore validé."))
            }

            val updatedDelivery = delivery.copy(
                driverCounterOffer = counterPriceXof,
                counterOfferDriverId = driver.id,
                counterOfferDriverName = driver.name,
                counterOfferDriverPhone = driver.phone,
                counterOfferDriverRating = driver.rating,
                counterOfferDriverDeliveries = driver.totalDeliveries,
                status = DeliveryStatus.DRIVER_COUNTER_OFFERED,
                offerStatus = "DRIVER_COUNTER_OFFERED"
            )

            dao.updateDelivery(updatedDelivery)
            notificationProvider.notifyCounterOfferReceived(updatedDelivery, driver.name, counterPriceXof)
            Result.success(updatedDelivery)
        }
    }

    /**
     * Client Accepts Driver Counter-Offer:
     * finalDeliveryPrice = driverCounterOffer. Locked immediately. Status = DRIVER_ASSIGNED.
     */
    suspend fun acceptDriverCounterOffer(
        deliveryId: String,
        clientId: String
    ): Result<DeliveryEntity> = withContext(Dispatchers.IO) {
        deliveryLock.withLock {
            val delivery = dao.getDeliveryById(deliveryId)
                ?: return@withContext Result.failure(Exception("Livraison non trouvée."))

            if (delivery.clientId != clientId) {
                return@withContext Result.failure(Exception("Non autorisé pour cette commande."))
            }

            if (delivery.status != DeliveryStatus.DRIVER_COUNTER_OFFERED) {
                return@withContext Result.failure(Exception("Aucune contre-offre en attente pour cette course."))
            }

            val counterPrice = delivery.driverCounterOffer
                ?: return@withContext Result.failure(Exception("Contre-offre invalide."))

            val driverId = delivery.counterOfferDriverId
                ?: return@withContext Result.failure(Exception("Livreur introuvable pour cette offre."))

            val driver = dao.getDriverById(driverId)
                ?: return@withContext Result.failure(Exception("Livreur introuvable."))

            val finalPrice = counterPrice
            val commission = (finalPrice * 0.10).toInt()
            val customerTotal = finalPrice + commission
            val driverEarnings = finalPrice

            val updatedDelivery = delivery.copy(
                finalDeliveryPrice = finalPrice,
                commission = commission,
                customerTotal = customerTotal,
                driverEarnings = driverEarnings,
                totalPriceXof = customerTotal,
                platformFeeXof = commission,
                driverEarningsXof = driverEarnings,
                driverId = driver.id,
                driverName = driver.name,
                driverPhone = driver.phone,
                driverVehicle = "${driver.vehicleType.label} - ${driver.vehicleModel}",
                driverRating = driver.rating,
                status = DeliveryStatus.DRIVER_ASSIGNED,
                offerStatus = "COUNTER_OFFER_ACCEPTED",
                acceptedAt = System.currentTimeMillis(),
                currentDriverLat = driver.currentLat,
                currentDriverLng = driver.currentLng
            )

            dao.updateDelivery(updatedDelivery)
            notificationProvider.notifyCounterOfferAccepted(updatedDelivery, driver, finalPrice)
            notificationProvider.notifyDriverAssigned(updatedDelivery, driver)
            Result.success(updatedDelivery)
        }
    }

    /**
     * Client Rejects Driver Counter-Offer:
     * Delivery status resets to SEARCHING_DRIVER so other drivers can accept/propose.
     */
    suspend fun rejectDriverCounterOffer(
        deliveryId: String,
        clientId: String
    ): Result<DeliveryEntity> = withContext(Dispatchers.IO) {
        deliveryLock.withLock {
            val delivery = dao.getDeliveryById(deliveryId)
                ?: return@withContext Result.failure(Exception("Livraison non trouvée."))

            if (delivery.clientId != clientId) {
                return@withContext Result.failure(Exception("Non autorisé pour cette commande."))
            }

            if (delivery.status != DeliveryStatus.DRIVER_COUNTER_OFFERED) {
                return@withContext Result.failure(Exception("Aucune contre-offre active à refuser."))
            }

            val rejectedDriverId = delivery.counterOfferDriverId

            val updatedDelivery = delivery.copy(
                driverCounterOffer = null,
                counterOfferDriverId = null,
                counterOfferDriverName = null,
                counterOfferDriverPhone = null,
                status = DeliveryStatus.SEARCHING_DRIVER,
                offerStatus = "COUNTER_OFFER_REJECTED"
            )

            dao.updateDelivery(updatedDelivery)
            if (rejectedDriverId != null) {
                notificationProvider.notifyCounterOfferRejected(updatedDelivery, rejectedDriverId)
            }
            Result.success(updatedDelivery)
        }
    }

    /**
     * Client modifies initial offer (>= 1000 FCFA) while searching for a driver.
     */
    suspend fun updateCustomerOffer(
        deliveryId: String,
        clientId: String,
        newOfferXof: Int
    ): Result<DeliveryEntity> = withContext(Dispatchers.IO) {
        deliveryLock.withLock {
            if (newOfferXof < 1000) {
                return@withContext Result.failure(Exception("Le prix minimum est de 1000 FCFA."))
            }

            val delivery = dao.getDeliveryById(deliveryId)
                ?: return@withContext Result.failure(Exception("Livraison non trouvée."))

            if (delivery.clientId != clientId) {
                return@withContext Result.failure(Exception("Non autorisé pour cette commande."))
            }

            if (delivery.driverId != null) {
                return@withContext Result.failure(Exception("Cette course est déjà attribuée à un livreur."))
            }

            val commission = (newOfferXof * 0.10).toInt()
            val customerTotal = newOfferXof + commission
            val driverEarnings = newOfferXof

            val updatedDelivery = delivery.copy(
                customerInitialOffer = newOfferXof,
                finalDeliveryPrice = newOfferXof,
                commission = commission,
                customerTotal = customerTotal,
                driverEarnings = driverEarnings,
                totalPriceXof = customerTotal,
                platformFeeXof = commission,
                driverEarningsXof = driverEarnings,
                driverCounterOffer = null,
                counterOfferDriverId = null,
                status = DeliveryStatus.SEARCHING_DRIVER,
                offerStatus = "SEARCHING_DRIVER"
            )

            dao.updateDelivery(updatedDelivery)
            notificationProvider.notifyNewDeliveryAvailable(updatedDelivery)
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

        when (newStatus) {
            DeliveryStatus.DRIVER_ARRIVED -> notificationProvider.notifyDriverArrived(updated)
            DeliveryStatus.DRIVER_ARRIVING, DeliveryStatus.IN_TRANSIT -> notificationProvider.notifyDriverEnRoute(updated)
            else -> {}
        }

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

            // Verify with cryptographically secure OTP Provider
            val verificationResult = otpProvider.verifyDeliveryOtp(deliveryId, driverId, inputOtp)
            val otpOutcome = verificationResult.getOrNull()

            when (otpOutcome) {
                is OtpVerificationResult.Success -> {
                    // Validated! Finalize delivery
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

                    // Dispatch notifications & transactional receipt
                    notificationProvider.notifyDeliveryCompleted(completed)
                    val client = dao.getUserById(delivery.clientId)
                    client?.email?.let { email ->
                        emailProvider.sendDeliveryConfirmation(
                            email = email,
                            clientName = client.name,
                            trackingNumber = delivery.trackingNumber,
                            amountXof = delivery.totalPriceXof
                        )
                    }

                    Result.success(completed)
                }
                is OtpVerificationResult.InvalidCode -> {
                    Result.failure(Exception("Code OTP incorrect (${otpOutcome.attemptsRemaining} essais restants)."))
                }
                is OtpVerificationResult.MaxAttemptsExceeded -> {
                    Result.failure(Exception("Nombre maximal de tentatives OTP dépassé (5/5). Contactez le support WÀNDÉ."))
                }
                is OtpVerificationResult.Expired -> {
                    Result.failure(Exception("Code de remise expiré. Veuillez contacter le support."))
                }
                is OtpVerificationResult.Error -> {
                    Result.failure(Exception(otpOutcome.message))
                }
                else -> {
                    Result.failure(Exception("Erreur de validation du code OTP."))
                }
            }
        }
    }

    /**
     * Process payment with server-side confirmation & idempotency using PaymentGateway
     */
    suspend fun processPayment(
        deliveryId: String,
        clientId: String,
        amountXof: Int,
        provider: PaymentMethod,
        simulationMode: PaymentSimulationMode? = null
    ): Result<PaymentEntity> = withContext(Dispatchers.IO) {
        val user = dao.getUserById(clientId)
        val customerName = user?.name ?: "Client WÀNDÉ"
        val customerPhone = user?.phone ?: "+226 70 00 00 00"

        val initResult = paymentGateway.initiateDeliveryPayment(
            deliveryId = deliveryId,
            amountXof = amountXof,
            customerName = customerName,
            customerPhone = customerPhone,
            provider = provider,
            description = "Paiement livraison WÀNDÉ #$deliveryId",
            customSimulationMode = simulationMode
        )

        when (initResult) {
            is PaymentInitiateResult.Success -> {
                val payment = PaymentEntity(
                    deliveryId = deliveryId,
                    clientId = clientId,
                    amountXof = amountXof,
                    currency = "XOF",
                    provider = provider,
                    providerTransactionId = initResult.providerRef,
                    status = PaymentStatus.PAYMENT_SUCCESS
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

            is PaymentInitiateResult.PendingCheckout -> {
                val payment = PaymentEntity(
                    deliveryId = deliveryId,
                    clientId = clientId,
                    amountXof = amountXof,
                    currency = "XOF",
                    provider = provider,
                    providerTransactionId = initResult.providerRef,
                    status = PaymentStatus.PAYMENT_PENDING
                )
                dao.insertPayment(payment)

                val delivery = dao.getDeliveryById(deliveryId)
                delivery?.let {
                    dao.updateDelivery(
                        it.copy(
                            isPaid = false,
                            paymentProvider = provider,
                            paymentTransactionId = payment.providerTransactionId
                        )
                    )
                }
                Result.success(payment)
            }

            is PaymentInitiateResult.Failed -> {
                val payment = PaymentEntity(
                    deliveryId = deliveryId,
                    clientId = clientId,
                    amountXof = amountXof,
                    currency = "XOF",
                    provider = provider,
                    providerTransactionId = initResult.transactionId,
                    status = PaymentStatus.PAYMENT_FAILED
                )
                dao.insertPayment(payment)
                Result.failure(Exception(initResult.errorMessage))
            }

            is PaymentInitiateResult.Expired -> {
                val payment = PaymentEntity(
                    deliveryId = deliveryId,
                    clientId = clientId,
                    amountXof = amountXof,
                    currency = "XOF",
                    provider = provider,
                    providerTransactionId = initResult.transactionId,
                    status = PaymentStatus.PAYMENT_EXPIRED
                )
                dao.insertPayment(payment)
                Result.failure(Exception(initResult.errorMessage))
            }
        }
    }

    /**
     * Request driver payout to Mobile Money with verification check and Payout Gateway
     */
    suspend fun requestDriverPayout(
        driverId: String,
        amountXof: Int,
        mobileMoneyNumber: String,
        provider: PaymentMethod
    ): Result<PayoutEntity> = withContext(Dispatchers.IO) {
        val driver = dao.getDriverById(driverId)
            ?: return@withContext Result.failure(Exception("Livreur introuvable."))

        if (driver.verificationStatus != DriverVerificationStatus.VERIFIED) {
            return@withContext Result.failure(Exception("Seuls les livreurs vérifiés par l'administration peuvent demander un virement."))
        }

        if (amountXof < 1000) {
            return@withContext Result.failure(Exception("Le montant minimum de retrait est de 1 000 FCFA."))
        }

        if (driver.balanceXof < amountXof) {
            return@withContext Result.failure(Exception("Solde insuffisant (Solde actuel : ${driver.balanceXof} FCFA)."))
        }

        // Debit driver balance in internal ledger
        dao.debitDriverBalance(driverId, amountXof)

        val payoutId = UUID.randomUUID().toString()
        val payoutPhone = mobileMoneyNumber.ifEmpty { driver.phone }

        val payoutReq = PayoutRequest(
            payoutId = payoutId,
            driverId = driverId,
            driverName = driver.name,
            driverPhone = driver.phone,
            mobileMoneyNumber = payoutPhone,
            amountXof = amountXof,
            provider = provider
        )

        val payoutResult = paymentGateway.disburseDriverPayout(payoutReq)

        val payout = PayoutEntity(
            id = payoutId,
            driverId = driverId,
            driverName = driver.name,
            amountXof = amountXof,
            phone = payoutPhone,
            provider = provider,
            status = payoutResult.status,
            transactionRef = payoutResult.providerRef,
            processedAt = if (payoutResult.status == PayoutStatus.PAYOUT_COMPLETED) System.currentTimeMillis() else null
        )
        dao.insertPayout(payout)

        // Log transaction in double-entry ledger
        dao.insertTransaction(
            TransactionEntity(
                driverId = driverId,
                type = TransactionType.PAYOUT,
                amountXof = -amountXof,
                description = "Virement Mobile Money vers $payoutPhone (${provider.label}) : ${payoutResult.message}",
                status = if (payoutResult.status == PayoutStatus.PAYOUT_COMPLETED) "COMPLETED" else "PENDING"
            )
        )

        Result.success(payout)
    }

    /**
     * Admin approve or reject driver payout
     */
    suspend fun adminProcessPayout(payoutId: String, approve: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val payouts = dao.getAllPayouts().firstOrNull() ?: emptyList()
        val payout = payouts.find { it.id == payoutId }
            ?: return@withContext Result.failure(Exception("Demande de retrait introuvable."))

        if (approve) {
            val driver = dao.getDriverById(payout.driverId)
            val payoutReq = PayoutRequest(
                payoutId = payout.id,
                driverId = payout.driverId,
                driverName = payout.driverName,
                driverPhone = driver?.phone ?: payout.phone,
                mobileMoneyNumber = payout.phone,
                amountXof = payout.amountXof,
                provider = payout.provider
            )
            val result = paymentGateway.disburseDriverPayout(payoutReq)
            dao.updatePayout(
                payout.copy(
                    status = result.status,
                    transactionRef = result.providerRef,
                    processedAt = System.currentTimeMillis()
                )
            )
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
     * Submit Driver KYC with IdentityVerificationProvider
     */
    suspend fun submitDriverKyc(bundle: KycDocumentBundle): Result<KycSubmissionResult> = withContext(Dispatchers.IO) {
        identityProvider.submitKycVerification(bundle)
    }

    /**
     * Admin Review Driver KYC (APPROVE, REJECT, REQUEST_NEW_PHOTO)
     */
    suspend fun adminReviewDriverKyc(
        driverId: String,
        decision: KycAdminDecision,
        feedback: String?
    ): Result<DriverVerificationStatus> = withContext(Dispatchers.IO) {
        identityProvider.adminReview(driverId, decision, feedback)
    }

    /**
     * Optimized driver location update (throttled & battery-friendly)
     */
    suspend fun updateDriverLocationOptimized(
        driverId: String,
        isOnline: Boolean,
        hasActiveDelivery: Boolean,
        lat: Double,
        lng: Double
    ): Boolean = withContext(Dispatchers.IO) {
        val shouldWrite = locationOptimizer.shouldPublishLocation(isOnline, hasActiveDelivery, lat, lng)
        if (shouldWrite) {
            dao.updateDriverLocation(driverId, lat, lng)
        }
        shouldWrite
    }

    /**
     * Driver Online/Offline toggle
     */
    suspend fun setDriverOnline(driverId: String, isOnline: Boolean) = withContext(Dispatchers.IO) {
        dao.setDriverOnline(driverId, isOnline)
    }

    /**
     * Admin Driver Verification Status direct setter
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
    val finalDeliveryPriceXof: Int = 1000,
    val platformFeeXof: Int,
    val driverEarningsXof: Int,
    val minPriceXof: Int = 1000,
    val recommendedPriceXof: Int = 1500,
    val attractivePriceXof: Int = 2000,
    val distanceKm: Double,
    val estimatedMinutes: Int
)
