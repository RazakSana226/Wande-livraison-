package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.PricingBreakdown
import com.example.data.repository.WandeRepository
import com.example.model.*
import com.example.service.GeminiMapsService
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
    val paymentProvider: PaymentProvider = PaymentProvider.ORANGE_MONEY,
    val pricing: PricingBreakdown? = null,
    val isCalculatingPrice: Boolean = false,
    val isSubmitting: Boolean = false,
    val searchResults: List<LatLngPoint> = emptyList(),
    val isSearchingPlaces: Boolean = false,
    val errorMessage: String? = null
)

class WandeViewModel(
    private val repository: WandeRepository
) : ViewModel() {

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

    fun setPaymentProvider(provider: PaymentProvider) {
        _createDeliveryState.update { it.copy(paymentProvider = provider) }
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

    fun updatePriceEstimation() {
        viewModelScope.launch {
            val state = _createDeliveryState.value
            val distanceKm = GeminiMapsService.calculateDistanceKm(
                state.pickupPoint.latitude, state.pickupPoint.longitude,
                state.destinationPoint.latitude, state.destinationPoint.longitude
            )
            val pricing = repository.calculateDeliveryPrice(distanceKm, state.packageSize)
            _createDeliveryState.update { it.copy(pricing = pricing) }
        }
    }

    fun submitDeliveryRequest(onSuccess: (String) -> Unit) {
        val state = _createDeliveryState.value
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
                paymentProvider = state.paymentProvider
            )
            _createDeliveryState.update { it.copy(isSubmitting = false) }

            result.onSuccess { delivery ->
                _selectedDeliveryId.value = delivery.id
                _userMessage.value = "Demande envoyée ! Recherche de livreurs à proximité..."
                onSuccess(delivery.id)
            }.onFailure { err ->
                _userMessage.value = "Erreur: ${err.message}"
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
                _userMessage.value = "Course acceptée avec succès !"
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

    fun requestDriverPayout(amountXof: Int, phone: String, provider: PaymentProvider) {
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
