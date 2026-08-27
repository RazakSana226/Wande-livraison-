package com.example.service.identity

import com.example.model.DriverVerificationStatus
import com.example.model.IdentityDocumentType

/**
 * Metadata for liveness and capture validation.
 * Architecture Note: We never claim a single photo guarantees physical presence.
 * This structure allows both manual review and integration with external Liveness SDKs.
 */
data class LivenessCaptureMetadata(
    val faceInFrame: Boolean = true,
    val ambientLightLux: Float = 350f,
    val devicePitch: Float = 0f,
    val deviceRoll: Float = 0f,
    val eyeContactScore: Float = 0.95f,
    val capturedAt: Long = System.currentTimeMillis(),
    val captureInstructionsAccepted: Boolean = true
)

data class KycDocumentBundle(
    val driverId: String,
    val fullName: String,
    val birthDate: String,
    val phone: String,
    val city: String,
    val habitualZone: String,
    val documentType: IdentityDocumentType,
    val documentFrontUri: String,
    val documentBackUri: String?,
    val selfieUri: String,
    val livenessMetadata: LivenessCaptureMetadata
)

sealed class LivenessAssessmentResult {
    data class Success(val confidenceScore: Float, val isLive: Boolean) : LivenessAssessmentResult()
    data class RetryRequired(val reason: String) : LivenessAssessmentResult()
    data class Failed(val error: String) : LivenessAssessmentResult()
}

data class KycSubmissionResult(
    val submissionId: String,
    val status: DriverVerificationStatus,
    val message: String,
    val submittedAt: Long = System.currentTimeMillis()
)

data class KycStatusInfo(
    val driverId: String,
    val status: DriverVerificationStatus,
    val rejectionReason: String? = null,
    val submittedAt: Long? = null,
    val reviewedAt: Long? = null
)

enum class KycAdminDecision {
    APPROVE,
    REJECT,
    REQUEST_NEW_PHOTO
}

/**
 * Universal Pluggable Identity Verification Provider
 * Decouples identity verification from Firestore/UI, enabling easy future transition
 * to specialized biometric vendors (SmileID, Didomi, Onfido, AWS Rekognition).
 */
interface IdentityVerificationProvider {
    suspend fun submitKycVerification(bundle: KycDocumentBundle): Result<KycSubmissionResult>
    suspend fun getKycStatus(driverId: String): KycStatusInfo
    suspend fun assessLiveness(selfieData: ByteArray?, metadata: LivenessCaptureMetadata): LivenessAssessmentResult
    suspend fun adminReview(driverId: String, decision: KycAdminDecision, feedback: String?): Result<DriverVerificationStatus>
}
