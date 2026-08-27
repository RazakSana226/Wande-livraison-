package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.PricingBreakdown
import com.example.data.repository.WandeRepository
import com.example.model.*
import com.example.service.GeminiMapsService
import com.example.service.auth.*
import com.example.service.otp.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ClientCreateDeliveryState(
    val pickupPoint: LatLngPoint = GeminiMapsService.POPULAR_LOCATIONS[0],
    val pickupInstructions: String = "",
    val destinationPoint: LatLngPoint = GeminiMapsService.POPULAR_LOCATIONS[1],
    val recipientName: String = "",
    val recipientPhone: String = "",
    val packageDescription: String = "",
    val packageSize: PackageSize = PackageSize.PETIT,
    val specialNotes: String = "",
    val proposedPriceXof: Int = 1000,
    val customPriceInput: String = "1000",
    val pricingErrorMessage: String? = null,
    val paymentProvider: PaymentMethod = PaymentMethod.ORANGE_MONEY,
    val paymentSimulationMode: PaymentSimulationMode = PaymentSimulationMode.SIMULATE_SUCCESS,
    val pricing: PricingBreakdown? = null,
    val isCalculatingPrice: Boolean = false,
    val isSubmitting: Boolean = false,
    val searchResults: List<LatLngPoint> = emptyList(),
    val isSearchingPlaces: Boolean = false,
    val errorMessage: String? = null
)

class WandeViewModel(
    val repository: WandeRepository
) : ViewModel() {

    // Payment Gateway references
    val paymentGateway = repository.paymentGateway
    val isPaymentMockMode = paymentGateway.isMockMode
    val paymentSimulationMode = paymentGateway.simulationMode

    // Current Active Role (CLIENT, DRIVER, ADMIN)
    private val _currentRole = MutableStateFlow(UserRole.CLIENT)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    // Active User IDs
    val currentClientId = "usr_client_1"
    val currentDriverId = "drv_1"
    val currentAdminId = "usr_admin_1"

    // Form state for creating delivery
    private val _createDeliveryState = MutableStateFlow(ClientCreateDeliveryState())
    val createDeliveryState: StateFlow<ClientCreateDeliveryState> = _createDeliveryState.asStateFlow()

    // Active selected delivery for client tracking or driver detail view
    private val _selectedDeliveryId = MutableStateFlow<String?>(null)
    val selectedDeliveryId: StateFlow<String?> = _selectedDeliveryId.asStateFlow()

    // Notification toast message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Streams from Repository
    val allDeliveries: StateFlow<List<DeliveryEntity>> = repository.allDeliveries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientDeliveries: StateFlow<List<DeliveryEntity>> = repository.getDeliveriesForClient(currentClientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val driverDeliveries: StateFlow<List<DeliveryEntity>> = repository.getDeliveriesForDriver(currentDriverId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openDeliveries: StateFlow<List<DeliveryEntity>> = repository.getOpenDeliveries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDrivers: StateFlow<List<DriverEntity>> = repository.allDrivers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayouts: StateFlow<List<PayoutEntity>> = repository.allPayouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDisputes: StateFlow<List<DisputeEntity>> = repository.allDisputes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val platformSettings: StateFlow<PlatformSettingsEntity?> = repository.platformSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlatformSettingsEntity())

    val allAuditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Authentication & Email OTP State
    val authProvider = repository.authProvider
    val otpProvider = repository.otpProvider
    val currentAuthUser = authProvider.currentAuthUser
    val authState = authProvider.authState

    private val _otpState = MutableStateFlow<OtpVerificationResult?>(null)
    val otpState: StateFlow<OtpVerificationResult?> = _otpState.asStateFlow()

    private val _isOtpLoading = MutableStateFlow(false)
    val isOtpLoading: StateFlow<Boolean> = _isOtpLoading.asStateFlow()

    private val _otpCooldownSeconds = MutableStateFlow(0)
    val otpCooldownSeconds: StateFlow<Int> = _otpCooldownSeconds.asStateFlow()

    private var cooldownJob: Job? = null

    // Active delivery tracking flow
    val activeClientDelivery: StateFlow<DeliveryEntity?> = clientDeliveries.map { list ->
        list.firstOrNull { it.status != DeliveryStatus.DELIVERED && it.status != DeliveryStatus.CANCELLED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeDriverDelivery: StateFlow<DeliveryEntity?> = driverDeliveries.map { list ->
        list.firstOrNull { it.status != DeliveryStatus.DELIVERED && it.status != DeliveryStatus.CANCELLED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentDriver: StateFlow<DriverEntity?> = allDrivers.map { list ->
        list.firstOrNull { it.id == currentDriverId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var placeSearchJob: Job? = null

    init {
        updatePriceEstimation()
    }

    fun setRole(role: UserRole) {
        _currentRole.value = role
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun setSelectedDelivery(id: String?) {
        _selectedDeliveryId.value = id
    }

    // --- CLIENT ACTIONS ---

    fun setPickupPoint(point: LatLngPoint) {
        _createDeliveryState.update { it.copy(pickupPoint = point) }
        updatePriceEstimation()
    }

    fun setDestinationPoint(point: LatLngPoint) {
        _createDeliveryState.update { it.copy(destinationPoint = point) }
        updatePriceEstimation()
    }

    fun setPackageSize(size: PackageSize) {
        _createDeliveryState.update { it.copy(packageSize = size) }
        updatePriceEstimation()
    }

    fun setRecipientInfo(name: String, phone: String) {
        _createDeliveryState.update { it.copy(recipientName = name, recipientPhone = phone) }
    }

    fun setPackageDetails(description: String, notes: String, pickupInstructions: String) {
        _createDeliveryState.update {
            it.copy(
                packageDescription = description,
                specialNotes = notes,
                pickupInstructions = pickupInstructions
            )
        }
    }

    fun setPaymentProvider(provider: PaymentMethod) {
        _createDeliveryState.update { it.copy(paymentProvider = provider) }
    }

    fun setPaymentSimulationMode(mode: PaymentSimulationMode) {
        _createDeliveryState.update { it.copy(paymentSimulationMode = mode) }
    }

    fun setGlobalPaymentSimulationMode(mode: PaymentSimulationMode) {
        paymentGateway.setSimulationMode(mode)
    }

    fun togglePaymentGatewayMock(isMock: Boolean) {
        paymentGateway.setMockMode(isMock)
    }

    fun searchPlaces(query: String) {
        placeSearchJob?.cancel()
        if (query.length < 2) {
            _createDeliveryState.update { it.copy(searchResults = emptyList(), isSearchingPlaces = false) }
            return
        }
        placeSearchJob = viewModelScope.launch {
            _createDeliveryState.update { it.copy(isSearchingPlaces = true) }
            val results = GeminiMapsService.searchPlaceWithGemini(query)
            _createDeliveryState.update { it.copy(searchResults = results, isSearchingPlaces = false) }
        }
    }

    fun setProposedPrice(price: Int) {
        _createDeliveryState.update {
            it.copy(
                proposedPriceXof = price,
                customPriceInput = price.toString(),
                pricingErrorMessage = if (price < 1000) "Le prix minimum est de 1000 FCFA." else null
            )
        }
        updatePriceEstimation()
    }

    fun setCustomPriceInput(input: String) {
        val numeric = input.filter { it.isDigit() }
        val price = numeric.toIntOrNull() ?: 0
        _createDeliveryState.update {
            it.copy(
                customPriceInput = numeric,
                proposedPriceXof = price,
                pricingErrorMessage = if (numeric.isNotEmpty() && price < 1000) "Le prix minimum est de 1000 FCFA." else null
            )
        }
        if (price >= 1000) {
            updatePriceEstimation()
        }
    }

    fun updatePriceEstimation() {
        viewModelScope.launch {
            val state = _createDeliveryState.value
            val distanceKm = GeminiMapsService.calculateDistanceKm(
                state.pickupPoint.latitude, state.pickupPoint.longitude,
                state.destinationPoint.latitude, state.destinationPoint.longitude
            )
            val pricing = repository.calculateDeliveryPrice(
                distanceKm = distanceKm,
                packageSize = state.packageSize,
                customPriceXof = if (state.proposedPriceXof >= 1000) state.proposedPriceXof else 1000
            )
            _createDeliveryState.update { it.copy(pricing = pricing) }
        }
    }

    fun submitDeliveryRequest(onSuccess: (String) -> Unit) {
        val state = _createDeliveryState.value
        if (state.proposedPriceXof < 1000) {
            _userMessage.value = "Le prix minimum d'une livraison est de 1000 FCFA."
            _createDeliveryState.update { it.copy(pricingErrorMessage = "Le prix minimum est de 1000 FCFA.") }
            return
        }
        if (state.recipientName.isBlank() || state.recipientPhone.isBlank()) {
            _userMessage.value = "Veuillez renseigner le nom et téléphone du destinataire."
            return
        }
        if (state.packageDescription.isBlank()) {
            _userMessage.value = "Veuillez décrire brièvement le colis."
            return
        }

        viewModelScope.launch {
            _createDeliveryState.update { it.copy(isSubmitting = true) }
            val result = repository.createDelivery(
                clientId = currentClientId,
                clientName = "Amadou Ouédraogo",
                clientPhone = "+226 70 12 34 56",
                pickupPoint = state.pickupPoint,
                pickupInstructions = state.pickupInstructions,
                destPoint = state.destinationPoint,
                recipientName = state.recipientName,
                recipientPhone = state.recipientPhone,
                packageDesc = state.packageDescription,
                packageSize = state.packageSize,
                specialNotes = state.specialNotes,
                proposedPriceXof = state.proposedPriceXof,
                paymentProvider = state.paymentProvider,
                simulationMode = state.paymentSimulationMode
            )
            _createDeliveryState.update { it.copy(isSubmitting = false) }

            result.onSuccess { delivery ->
                _selectedDeliveryId.value = delivery.id
                _userMessage.value = "Offre de ${delivery.customerInitialOffer} FCFA envoyée aux livreurs à proximité !"
                onSuccess(delivery.id)
            }.onFailure { err ->
                _userMessage.value = err.message ?: "Échec de création de la livraison"
            }
        }
    }

    // --- CLIENT NEGOTIATION ACTIONS ---

    fun acceptDriverCounterOffer(deliveryId: String) {
        viewModelScope.launch {
            val result = repository.acceptDriverCounterOffer(deliveryId, currentClientId)
            result.onSuccess { delivery ->
                _selectedDeliveryId.value = delivery.id
                _userMessage.value = "✓ Proposition acceptée ! Votre livraison est confirmée à ${delivery.finalDeliveryPrice} FCFA."
            }.onFailure { err ->
                _userMessage.value = err.message ?: "Impossible d'accepter la contre-offre."
            }
        }
    }

    fun rejectDriverCounterOffer(deliveryId: String) {
        viewModelScope.launch {
            val result = repository.rejectDriverCounterOffer(deliveryId, currentClientId)
            result.onSuccess {
                _userMessage.value = "Proposition refusée. Votre course reste ouverte à d'autres livreurs."
            }.onFailure { err ->
                _userMessage.value = err.message ?: "Erreur lors du refus."
            }
        }
    }

    fun updateCustomerOffer(deliveryId: String, newOfferPrice: Int) {
        if (newOfferPrice < 1000) {
            _userMessage.value = "Le prix minimum est de 1000 FCFA."
            return
        }
        viewModelScope.launch {
            val result = repository.updateCustomerOffer(deliveryId, currentClientId, newOfferPrice)
            result.onSuccess {
                _userMessage.value = "Votre offre a été actualisée à $newOfferPrice FCFA."
            }.onFailure { err ->
                _userMessage.value = err.message ?: "Impossible de mettre à jour le prix."
            }
        }
    }

    // --- DRIVER ACTIONS ---

    fun toggleDriverOnline(isOnline: Boolean) {
        viewModelScope.launch {
            repository.setDriverOnline(currentDriverId, isOnline)
            _userMessage.value = if (isOnline) "🟢 Vous êtes maintenant en ligne pour recevoir des courses" else "🔴 Vous êtes hors ligne"
        }
    }

    fun acceptDeliveryRequest(deliveryId: String) {
        viewModelScope.launch {
            val result = repository.acceptDelivery(deliveryId, currentDriverId)
            result.onSuccess { delivery ->
                _selectedDeliveryId.value = delivery.id
                _userMessage.value = "Course acceptée au prix proposé (${delivery.finalDeliveryPrice} FCFA) !"
                // Start movement simulation
                val waypoints = GeminiMapsService.generateRouteWaypoints(
                    delivery.currentDriverLat, delivery.currentDriverLng,
                    delivery.destinationLat, delivery.destinationLng
                )
                repository.simulateLiveDriverMovement(delivery.id, waypoints)
            }.onFailure { err ->
                _userMessage.value = err.message ?: "Impossible d'accepter cette course."
            }
        }
    }

    fun submitDriverCounterOffer(deliveryId: String, counterPrice: Int) {
        if (counterPrice < 1000) {
            _userMessage.value = "Le prix minimum d'une livraison est de 1000 FCFA."
            return
        }
        viewModelScope.launch {
            val result = repository.submitDriverCounterOffer(deliveryId, currentDriverId, counterPrice)
            result.onSuccess {
                _userMessage.value = "Contre-offre de $counterPrice FCFA envoyée au client !"
            }.onFailure { err ->
                _userMessage.value = err.message ?: "Impossible d'envoyer la contre-offre."
            }
        }
    }

    fun updateDriverProgress(deliveryId: String, nextStatus: DeliveryStatus) {
        viewModelScope.launch {
            val result = repository.updateDeliveryProgress(deliveryId, nextStatus, currentDriverId)
            result.onSuccess {
                _userMessage.value = "Statut mis à jour : ${nextStatus.label}"
            }.onFailure { err ->
                _userMessage.value = err.message
            }
        }
    }

    fun completeDeliveryWithOtp(deliveryId: String, otpInput: String) {
        viewModelScope.launch {
            val result = repository.verifyAndCompleteDelivery(deliveryId, currentDriverId, otpInput)
            result.onSuccess { delivery ->
                _userMessage.value = "🎉 Livraison validée ! +${delivery.driverEarningsXof} FCFA crédités sur votre compte."
            }.onFailure { err ->
                _userMessage.value = "❌ ${err.message}"
            }
        }
    }

    fun requestDriverPayout(amountXof: Int, phone: String, provider: PaymentMethod) {
        viewModelScope.launch {
            val result = repository.requestDriverPayout(currentDriverId, amountXof, phone, provider)
            result.onSuccess {
                _userMessage.value = "Demande de virement de $amountXof FCFA enregistrée !"
            }.onFailure { err ->
                _userMessage.value = err.message
            }
        }
    }

    fun registerDriverProfile(
        name: String,
        phone: String,
        vehicleType: VehicleType,
        vehicleModel: String,
        vehicleNumber: String,
        habitualZone: String,
        mobileMoneyNumber: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.registerDriver(
                userId = "usr_driver_1",
                name = name,
                phone = phone,
                vehicleType = vehicleType,
                vehicleModel = vehicleModel,
                vehicleNumber = vehicleNumber,
                habitualZone = habitualZone,
                mobileMoneyNumber = mobileMoneyNumber
            )
            result.onSuccess {
                _userMessage.value = "Profil livreur soumis ! En attente de validation admin."
                onSuccess()
            }
        }
    }

    /**
     * Submit Driver KYC Documents & Selfie
     */
    fun submitDriverKyc(
        bundle: com.example.service.identity.KycDocumentBundle,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.submitDriverKyc(bundle)
            result.onSuccess { submission ->
                _userMessage.value = "✓ " + submission.message
                onSuccess()
            }.onFailure { err ->
                _userMessage.value = err.message ?: "Erreur de soumission KYC"
                onError(err.message ?: "Erreur de soumission KYC")
            }
        }
    }

    /**
     * Admin Review KYC with 3 outcomes: APPROVE, REJECT, REQUEST_NEW_PHOTO
     */
    fun adminReviewKycDecision(
        driverId: String,
        decision: com.example.service.identity.KycAdminDecision,
        feedback: String? = null
    ) {
        viewModelScope.launch {
            val result = repository.adminReviewDriverKyc(driverId, decision, feedback)
            result.onSuccess { newStatus ->
                val actionLabel = when (decision) {
                    com.example.service.identity.KycAdminDecision.APPROVE -> "approuvé"
                    com.example.service.identity.KycAdminDecision.REJECT -> "rejeté"
                    com.example.service.identity.KycAdminDecision.REQUEST_NEW_PHOTO -> "signalé pour nouvelle photo"
                }
                _userMessage.value = "Dossier livreur $actionLabel avec succès."
            }.onFailure { err ->
                _userMessage.value = "Erreur: ${err.message}"
            }
        }
    }

    // --- REVIEWS & DISPUTES & CANCELLATION ---

    fun submitRating(deliveryId: String, driverId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.submitReview(
                deliveryId = deliveryId,
                fromUserId = currentClientId,
                fromName = "Amadou Ouédraogo",
                toUserId = driverId,
                rating = rating,
                comment = comment
            )
            _userMessage.value = "Merci pour votre évaluation !"
        }
    }

    fun cancelDelivery(deliveryId: String, reason: String) {
        viewModelScope.launch {
            repository.cancelDelivery(deliveryId, reason, _currentRole.value)
            _userMessage.value = "Livraison annulée."
        }
    }

    fun reportDispute(deliveryId: String, reason: String, details: String) {
        viewModelScope.launch {
            repository.reportDispute(
                deliveryId = deliveryId,
                userId = if (_currentRole.value == UserRole.CLIENT) currentClientId else currentDriverId,
                userName = if (_currentRole.value == UserRole.CLIENT) "Amadou Ouédraogo" else "Ibrahim Traoré",
                role = _currentRole.value,
                reason = reason,
                details = details
            )
            _userMessage.value = "Litige signalé à l'équipe support WÀNDÉ."
        }
    }

    // --- ADMIN ACTIONS ---

    fun adminSetDriverStatus(driverId: String, status: DriverVerificationStatus) {
        viewModelScope.launch {
            repository.adminSetDriverStatus(driverId, status)
            _userMessage.value = "Statut livreur mis à jour : ${status.name}"
        }
    }

    fun adminProcessPayout(payoutId: String, approve: Boolean) {
        viewModelScope.launch {
            val res = repository.adminProcessPayout(payoutId, approve)
            res.onSuccess {
                _userMessage.value = if (approve) "Virement validé et payé." else "Virement rejeté et recrédité."
            }
        }
    }

    // --- AUTH & EMAIL OTP ACTIONS ---

    fun signUpWithEmail(
        email: String,
        password: String,
        name: String,
        phone: String,
        role: UserRole,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = authProvider.signUpWithEmail(email, password, name, phone, role)
            result.onSuccess {
                _userMessage.value = "Compte créé ! Un email de confirmation a été envoyé."
                _currentRole.value = role
                onSuccess()
            }.onFailure {
                _userMessage.value = it.message ?: "Erreur lors de l'inscription."
            }
        }
    }

    fun signInWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = authProvider.signInWithEmail(email, password)
            result.onSuccess { user ->
                _currentRole.value = user.role
                _userMessage.value = "Bienvenue, ${user.name} !"
                onSuccess()
            }.onFailure {
                _userMessage.value = it.message ?: "Identifiants invalides."
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authProvider.signOut()
            _userMessage.value = "Vous avez été déconnecté."
        }
    }

    fun sendEmailVerification() {
        viewModelScope.launch {
            val result = authProvider.sendEmailVerification()
            result.onSuccess {
                _userMessage.value = "Email de vérification envoyé avec succès !"
            }.onFailure {
                _userMessage.value = it.message ?: "Échec de l'envoi de l'email."
            }
        }
    }

    fun reloadAuthStatus() {
        viewModelScope.launch {
            val result = authProvider.reloadUser()
            result.onSuccess { user ->
                if (user?.isEmailVerified == true) {
                    _userMessage.value = "✓ Adresse email confirmée !"
                }
            }
        }
    }

    fun requestEmailOtp(
        email: String,
        purpose: OtpPurpose,
        recipientName: String = "Client WÀNDÉ",
        onSuccess: (EmailOtpResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isOtpLoading.value = true
            val userId = currentAuthUser.value?.id ?: currentClientId
            val result = otpProvider.generateEmailOtp(userId, email, purpose, recipientName)
            _isOtpLoading.value = false

            result.onSuccess { otpRes ->
                _userMessage.value = "Code de sécurité envoyé à ${otpRes.maskedEmail}"
                startOtpCooldown(otpRes.remainingCooldownSeconds.toInt())
                onSuccess(otpRes)
            }.onFailure {
                _userMessage.value = it.message ?: "Impossible d'envoyer le code."
            }
        }
    }

    fun verifyEmailOtp(
        email: String,
        code: String,
        purpose: OtpPurpose,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isOtpLoading.value = true
            val userId = currentAuthUser.value?.id ?: currentClientId
            val result = otpProvider.verifyEmailOtp(userId, email, code, purpose)
            _isOtpLoading.value = false

            val outcome = result.getOrNull()
            _otpState.value = outcome

            when (outcome) {
                is OtpVerificationResult.Success -> {
                    _userMessage.value = "✓ Code vérifié avec succès !"
                    authProvider.confirmEmailVerifiedManually(userId)
                    onSuccess()
                }
                is OtpVerificationResult.InvalidCode -> {
                    _userMessage.value = "Code incorrect (${outcome.attemptsRemaining} essais restants)."
                }
                is OtpVerificationResult.MaxAttemptsExceeded -> {
                    _userMessage.value = "Nombre maximal de tentatives dépassé. Code invalidé."
                }
                is OtpVerificationResult.Expired -> {
                    _userMessage.value = "Ce code a expiré. Demandez un nouveau code."
                }
                is OtpVerificationResult.Error -> {
                    _userMessage.value = outcome.message
                }
                else -> {
                    _userMessage.value = "Échec de vérification du code."
                }
            }
        }
    }

    private fun startOtpCooldown(seconds: Int) {
        cooldownJob?.cancel()
        _otpCooldownSeconds.value = seconds
        cooldownJob = viewModelScope.launch {
            while (_otpCooldownSeconds.value > 0) {
                delay(1000)
                _otpCooldownSeconds.value = _otpCooldownSeconds.value - 1
            }
        }
    }

    fun adminSaveSettings(settings: PlatformSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            _userMessage.value = "Paramètres de tarification enregistrés !"
        }
    }
}

class WandeViewModelFactory(
    private val repository: WandeRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WandeViewModel::class.java)) {
            return WandeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
