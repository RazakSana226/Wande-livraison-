package com.example.model

enum class UserRole {
    CLIENT,
    DRIVER,
    ADMIN
}

enum class DriverVerificationStatus(val label: String) {
    PENDING_VERIFICATION("En attente de validation"),
    VERIFIED("Vérifié & Actif"),
    REJECTED("Dossier Rejeté"),
    SUSPENDED("Suspendu"),
    ACTION_REQUIRED("Action requise (Document à refaire)")
}

enum class IdentityDocumentType(val label: String) {
    CNI("Carte Nationale d'Identité (CNI)"),
    PASSPORT("Passeport"),
    PERMIS_CONDUIRE("Permis de conduire"),
    CARTE_CONSULAIRE("Carte consulaire")
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
    SEARCHING_DRIVER("Recherche de livreur", "Offre envoyée aux livreurs disponibles à proximité"),
    DRIVER_ACCEPTED("Offre acceptée", "Le livreur a accepté le prix proposé"),
    DRIVER_COUNTER_OFFERED("Contre-offre reçue", "Le livreur a proposé un autre montant"),
    COUNTER_OFFER_ACCEPTED("Contre-offre validée", "Le client a accepté la contre-offre du livreur"),
    COUNTER_OFFER_REJECTED("Contre-offre refusée", "Le client a refusé la proposition du livreur"),
    DRIVER_ASSIGNED("Livreur assigné", "Course attribuée et verrouillée"),
    DRIVER_ARRIVING("Livreur en route vers récupération", "Le livreur se dirige vers le point de départ"),
    PACKAGE_PICKED_UP("Colis récupéré", "Le livreur a pris en charge le colis"),
    IN_TRANSIT("En cours d'acheminement", "Le colis est en route vers la destination"),
    DELIVERY_IN_PROGRESS("Livraison en cours", "Le colis est en cours de livraison"),
    DRIVER_ARRIVED("Livreur arrivé à destination", "Le livreur attend la confirmation du destinataire"),
    DELIVERED("Livré avec succès", "La livraison est terminée et confirmée par OTP"),
    CANCELLED("Course annulée", "La livraison a été annulée"),
    // Backward compatibility aliases
    REQUESTED("Demande initiée", "La commande est en attente de validation"),
    DISPUTED("En litige", "Un signalement est en cours d'examen")
}

enum class PaymentMethod(val label: String) {
    ORANGE_MONEY("Orange Money"),
    MOOV_MONEY("Moov Money"),
    CINETPAY("CinetPay"),
    PAYDUNYA("PayDunya"),
    WAVE("Wave"),
    CASH_ON_DELIVERY("Paiement à la livraison")
}

typealias PaymentChannel = PaymentMethod

enum class PaymentStatus(val label: String) {
    PAYMENT_PENDING("En attente de paiement"),
    PAYMENT_SUCCESS("Paiement validé"),
    PAYMENT_FAILED("Paiement échoué"),
    PAYMENT_EXPIRED("Session de paiement expirée"),
    // Backward compatibility aliases
    PENDING("En attente"),
    PROCESSING("Traitement"),
    SUCCESS("Succès"),
    FAILED("Échec"),
    REFUNDED("Remboursé")
}

enum class PaymentSimulationMode(val label: String, val description: String) {
    SIMULATE_SUCCESS("Succès immédiat (200 OK)", "Valide le paiement instantanément"),
    SIMULATE_PENDING("En attente (Pending)", "Simule une validation asynchrone par Mobile Money"),
    SIMULATE_FAILED("Échec de paiement (Error)", "Simule un solde insuffisant ou rejet opérateur"),
    SIMULATE_EXPIRED("Session expirée (Timeout)", "Simule l'abandon ou délai dépassé (15 min)")
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

enum class OtpPurpose(val label: String) {
    EMAIL_VERIFICATION("Vérification d'adresse email"),
    LOGIN("Connexion sécurisée"),
    PASSWORD_RESET("Réinitialisation de mot de passe"),
    SECURITY_CONFIRMATION("Confirmation de sécurité")
}

enum class DeliveryOtpStatus(val label: String) {
    PENDING("En attente de remise"),
    VERIFIED("Code validé par livreur"),
    EXPIRED("Code expiré"),
    CANCELLED("Course annulée")
}

enum class AuditAction(val label: String) {
    LOGIN("Connexion utilisateur"),
    LOGOUT("Déconnexion utilisateur"),
    SIGNUP("Création de compte"),
    EMAIL_VERIFICATION_SENT("Email de vérification envoyé"),
    EMAIL_VERIFIED("Email vérifié avec succès"),
    OTP_GENERATED("Code OTP généré"),
    OTP_VERIFIED_SUCCESS("Code OTP validé"),
    OTP_VERIFIED_FAILURE("Échec validation OTP"),
    DELIVERY_OTP_GENERATED("OTP de livraison généré"),
    DELIVERY_OTP_VERIFIED_SUCCESS("Remise de colis validée par OTP"),
    DELIVERY_OTP_VERIFIED_FAILURE("Code de remise incorrect"),
    DELIVERY_COMPLETED("Livraison finalisée"),
    SUSPICIOUS_ATTEMPT("Tentative suspecte détectée"),
    RATE_LIMIT_EXCEEDED("Limite de requêtes dépassée"),
    UNAUTHORIZED_ACCESS("Tentative d'accès non autorisé")
}

enum class AuditSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val landmark: String = ""
)
