package com.example.service.identity

/**
 * External Vendor Adapter Interface for Advanced Biometric & Liveness SDKs
 * e.g., SmileID, Onfido, AWS Rekognition, Didomi.
 * Allows instant plug-and-play in production without touching domain or UI logic.
 */
interface KycLivenessProvider {
    val vendorName: String
    suspend fun verifyBiometrics(
        selfieBytes: ByteArray,
        documentBytes: ByteArray,
        metadata: LivenessCaptureMetadata
    ): BiometricVerificationResult
}

data class BiometricVerificationResult(
    val isMatch: Boolean,
    val matchConfidence: Float,
    val livenessPassed: Boolean,
    val providerReference: String,
    val details: String
)

/**
 * Default Simulator / Test Adapter for Development & Offline testing
 */
class MockKycLivenessProvider(
    override val vendorName: String = "WÀNDÉ Smart Liveness Engine (Local)"
) : KycLivenessProvider {
    override suspend fun verifyBiometrics(
        selfieBytes: ByteArray,
        documentBytes: ByteArray,
        metadata: LivenessCaptureMetadata
    ): BiometricVerificationResult {
        return BiometricVerificationResult(
            isMatch = true,
            matchConfidence = 0.985f,
            livenessPassed = metadata.faceInFrame && metadata.ambientLightLux > 100f,
            providerReference = "LIV-" + System.currentTimeMillis(),
            details = "Vérification biométrique locale simulée avec succès."
        )
    }
}
