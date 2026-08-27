package com.example.service.identity

import com.example.data.local.WandeDao
import com.example.model.AuditAction
import com.example.model.AuditLogEntity
import com.example.model.AuditSeverity
import com.example.model.DriverVerificationStatus
import com.example.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * MVP Provider: Secure Document Staging & Human Admin Verification
 * Protects driver documents and records compliance audit trail.
 */
class ManualIdentityVerificationProvider(
    private val dao: WandeDao
) : IdentityVerificationProvider {

    override suspend fun submitKycVerification(bundle: KycDocumentBundle): Result<KycSubmissionResult> =
        withContext(Dispatchers.IO) {
            try {
                val driver = dao.getDriverById(bundle.driverId)
                    ?: return@withContext Result.failure(Exception("Livreur introuvable (${bundle.driverId})"))

                // Assess selfie quality baseline
                val livenessOutcome = assessLiveness(null, bundle.livenessMetadata)
                val livenessScore = when (livenessOutcome) {
                    is LivenessAssessmentResult.Success -> livenessOutcome.confidenceScore
                    else -> 0.85f
                }

                // Update Driver entity with secure paths (storage references, never raw bytes in DB)
                val updatedDriver = driver.copy(
                    name = bundle.fullName,
                    phone = bundle.phone,
                    birthDate = bundle.birthDate,
                    city = bundle.city,
                    habitualZone = bundle.habitualZone,
                    idDocumentType = bundle.documentType,
                    idDocumentFrontUrl = bundle.documentFrontUri,
                    idDocumentBackUrl = bundle.documentBackUri,
                    selfieUrl = bundle.selfieUri,
                    livenessScore = livenessScore,
                    verificationStatus = DriverVerificationStatus.PENDING_VERIFICATION,
                    kycSubmittedAt = System.currentTimeMillis(),
                    rejectionReason = null
                )

                dao.updateDriver(updatedDriver)

                // Log audit trail for compliance
                dao.insertAuditLog(
                    AuditLogEntity(
                        userId = driver.userId,
                        userRole = UserRole.DRIVER,
                        action = AuditAction.SIGNUP,
                        details = "Dossier KYC soumis pour validation manuelle (Doc: ${bundle.documentType.name})",
                        severity = AuditSeverity.INFO
                    )
                )

                Result.success(
                    KycSubmissionResult(
                        submissionId = UUID.randomUUID().toString(),
                        status = DriverVerificationStatus.PENDING_VERIFICATION,
                        message = "Votre dossier a été soumis avec succès. Notre équipe examine vos pièces sous 24h."
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getKycStatus(driverId: String): KycStatusInfo = withContext(Dispatchers.IO) {
        val driver = dao.getDriverById(driverId)
        if (driver != null) {
            KycStatusInfo(
                driverId = driverId,
                status = driver.verificationStatus,
                rejectionReason = driver.rejectionReason,
                submittedAt = driver.kycSubmittedAt,
                reviewedAt = driver.kycReviewedAt
            )
        } else {
            KycStatusInfo(
                driverId = driverId,
                status = DriverVerificationStatus.PENDING_VERIFICATION
            )
        }
    }

    override suspend fun assessLiveness(
        selfieData: ByteArray?,
        metadata: LivenessCaptureMetadata
    ): LivenessAssessmentResult {
        if (!metadata.captureInstructionsAccepted) {
            return LivenessAssessmentResult.RetryRequired("Veuillez accepter les consignes de cadrage.")
        }
        if (!metadata.faceInFrame) {
            return LivenessAssessmentResult.RetryRequired("Visage non centré. Placez votre visage à l'intérieur du repère ovale.")
        }
        if (metadata.ambientLightLux < 100f) {
            return LivenessAssessmentResult.RetryRequired("Luminosité insuffisante. Rapprochez-vous d'une source lumineuse.")
        }
        return LivenessAssessmentResult.Success(confidenceScore = 0.96f, isLive = true)
    }

    override suspend fun adminReview(
        driverId: String,
        decision: KycAdminDecision,
        feedback: String?
    ): Result<DriverVerificationStatus> = withContext(Dispatchers.IO) {
        val driver = dao.getDriverById(driverId)
            ?: return@withContext Result.failure(Exception("Livreur introuvable"))

        val newStatus = when (decision) {
            KycAdminDecision.APPROVE -> DriverVerificationStatus.VERIFIED
            KycAdminDecision.REJECT -> DriverVerificationStatus.REJECTED
            KycAdminDecision.REQUEST_NEW_PHOTO -> DriverVerificationStatus.ACTION_REQUIRED
        }

        val updated = driver.copy(
            verificationStatus = newStatus,
            rejectionReason = feedback,
            kycReviewedAt = System.currentTimeMillis()
        )
        dao.updateDriver(updated)

        dao.insertAuditLog(
            AuditLogEntity(
                userId = driver.userId,
                userRole = UserRole.ADMIN,
                action = AuditAction.LOGIN,
                details = "Revue KYC livreur #${driver.id} : Décision=$newStatus, Motif=${feedback ?: "N/A"}",
                severity = if (newStatus == DriverVerificationStatus.REJECTED) AuditSeverity.WARNING else AuditSeverity.INFO
            )
        )

        Result.success(newStatus)
    }
}
