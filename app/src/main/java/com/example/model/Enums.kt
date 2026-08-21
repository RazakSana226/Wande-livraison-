package com.example.model

enum class UserRole {
    CLIENT,
    DRIVER,
    ADMIN
}

enum class DriverVerificationStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    REJECTED,
    SUSPENDED
}

enum class VehicleType(val label: String, val iconName: String) {
    MOTO("Moto", "two_wheeler"),
    TRICYCLE("Tricycle / Triporteur", "electric_rickshaw"),
    VOITURE("Voiture / Break", "directions_car")
}

enum class PackageSize(val label: String, val surchargeXof: Int, val description: String) {
    PETIT("Petit colis (< 3 kg)", 0, "Documents, repas, petits objets"),
    MOYEN("Colis moyen (3 - 10 kg)", 500, "Cartons, vêtements, électronique"),
    GRAND("Grand colis (10 - 25 kg)", 1500, "Électroménager, sacs volumineux")
}

enum class DeliveryStatus(val label: String, val description: String) {
    REQUESTED("Demande initiée", "La commande est en attente de validation"),
    SEARCHING_DRIVER("Recherche de livreur", "Recherche des livreurs à proximité..."),
    DRIVER_ASSIGNED("Livreur assigné", "Un livreur a accepté la course"),
    DRIVER_ARRIVING("Livreur en route vers récupération", "Le livreur se dirige vers le point de départ"),
    PACKAGE_PICKED_UP("Colis récupéré", "Le livreur a pris en charge le colis"),
    IN_TRANSIT("En cours d'acheminement", "Le colis est en route vers la destination"),
    DRIVER_ARRIVED("Livreur arrivé à destination", "Le livreur attend la confirmation du destinataire"),
    DELIVERED("Livré avec succès", "La livraison est terminée et confirmée par OTP"),
    CANCELLED("Course annulée", "La livraison a été annulée"),
    DISPUTED("En litige", "Un signalement est en cours d'examen")
}

enum class PaymentProvider(val label: String) {
    ORANGE_MONEY("Orange Money"),
    MOOV_MONEY("Moov Money"),
    CINETPAY("CinetPay"),
    PAYDUNYA("PayDunya"),
    WAVE("Wave"),
    CASH_ON_DELIVERY("Paiement à la livraison")
}

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    REFUNDED
}

enum class TransactionType(val label: String) {
    DELIVERY_EARNING("Gain course"),
    PLATFORM_FEE("Commission WÀNDÉ"),
    PAYOUT("Virement Mobile Money"),
    REFUND("Remboursement client"),
    ADJUSTMENT("Ajustement")
}

enum class PayoutStatus(val label: String) {
    PAYOUT_PENDING("En attente"),
    PAYOUT_PROCESSING("En cours de traitement"),
    PAYOUT_COMPLETED("Effectué"),
    PAYOUT_FAILED("Échoué")
}

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val landmark: String = ""
)
