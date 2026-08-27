package com.example.service.otp

import com.example.data.local.WandeDao
import com.example.model.*
import com.example.service.email.EmailProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class SecureEmailOtpProvider(
    private val dao: WandeDao,
    private val emailProvider: EmailProvider
) : OtpProvider {

    private val mutex = Mutex()

    companion object {
        const val EMAIL_OTP_LENGTH = 6
        const val DELIVERY_OTP_LENGTH = 4
        const val OTP_EXPIRATION_MS = 10 * 60 * 1000L // 10 minutes
        const val DELIVERY_OTP_EXPIRATION_MS = 24 * 60 * 60 * 1000L // 24 hours
        const val MAX_ATTEMPTS = 5
        const val RATE_LIMIT_WINDOW_MS = 15 * 60 * 1000L // 15 minutes
        const val MAX_REQUESTS_IN_WINDOW = 3
        const val RESEND_COOLDOWN_MS = 60 * 1000L // 60 seconds
    }

    override suspend fun generateEmailOtp(
        userId: String,
        email: String,
        purpose: OtpPurpose,
        recipientName: String
    ): Result<EmailOtpResult> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val normalizedEmail = email.trim().lowercase()
            val now = System.currentTimeMillis()

            // 1. Rate Limiting Check (Max 3 in 15 min)
            val windowStart = now - RATE_LIMIT_WINDOW_MS
            val recentOtps = dao.getRecentEmailOtps(normalizedEmail, windowStart)
            if (recentOtps.size >= MAX_REQUESTS_IN_WINDOW) {
                val oldestInWindow = recentOtps.minByOrNull { it.createdAt }?.createdAt ?: windowStart
                val cooldownRemainingSec = ((oldestInWindow + RATE_LIMIT_WINDOW_MS - now) / 1000).coerceAtLeast(1)

                dao.insertAuditLog(
                    AuditLogEntity(
                        userId = userId,
                        userEmail = normalizedEmail,
                        action = AuditAction.RATE_LIMIT_EXCEEDED,
                        details = "Limite de 3 demandes OTP par 15 min dépassée. Cooldown: ${cooldownRemainingSec}s",
                        severity = AuditSeverity.WARNING
                    )
                )

                return@withContext Result.failure(
                    IllegalStateException("Trop de tentatives. Veuillez patienter ${cooldownRemainingSec / 60 + 1} minutes avant de réessayer.")
                )
            }

            // 2. Cooldown check (60 seconds between resends)
            val latestOtp = recentOtps.maxByOrNull { it.createdAt }
            if (latestOtp != null && (now - latestOtp.createdAt) < RESEND_COOLDOWN_MS) {
                val waitSec = ((latestOtp.createdAt + RESEND_COOLDOWN_MS - now) / 1000).coerceAtLeast(1)
                return@withContext Result.failure(
                    IllegalStateException("Veuillez attendre $waitSec secondes avant de demander un nouveau code.")
                )
            }

            // 3. Invalidate previous active OTPs for this email
            dao.invalidateEmailOtps(normalizedEmail)

            // 4. Generate cryptographically secure 6-digit OTP
            val rawOtp = SecurityUtils.generateSecureNumericOtp(EMAIL_OTP_LENGTH)
            val salt = SecurityUtils.generateSalt()
            val hashedOtp = SecurityUtils.hashOtp(rawOtp, salt)
            val expiresAt = now + OTP_EXPIRATION_MS

            val otpEntity = EmailOtpEntity(
                id = "otp_" + UUID.randomUUID().toString().take(8),
                userId = userId,
                email = normalizedEmail,
                hashedOtp = hashedOtp,
                salt = salt,
                purpose = purpose,
                createdAt = now,
                expiresAt = expiresAt,
                attempts = 0,
                maxAttempts = MAX_ATTEMPTS,
                isInvalidated = false
            )
            dao.insertEmailOtp(otpEntity)

            // 5. Send OTP via Brevo Email Provider
            emailProvider.sendEmailOtp(
                email = normalizedEmail,
                recipientName = recipientName,
                otpCode = rawOtp,
                purpose = purpose
            )

            // 6. Security Audit Log (Never storing raw OTP in audit logs!)
            dao.insertAuditLog(
                AuditLogEntity(
                    userId = userId,
                    userEmail = normalizedEmail,
                    action = AuditAction.OTP_GENERATED,
                    details = "Code OTP généré avec succès pour ${purpose.label}. Hash SHA-256 sécurisé.",
                    severity = AuditSeverity.INFO
                )
            )

            Result.success(
                EmailOtpResult(
                    id = otpEntity.id,
                    userId = userId,
                    email = normalizedEmail,
                    purpose = purpose,
                    expiresAt = expiresAt,
                    remainingCooldownSeconds = 60,
                    attemptsRemaining = MAX_ATTEMPTS,
                    rawCodeForDebug = rawOtp // populated for test/sandbox inspections
                )
            )
        }
    }

    override suspend fun verifyEmailOtp(
        userId: String,
        email: String,
        inputCode: String,
        purpose: OtpPurpose
    ): Result<OtpVerificationResult> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val normalizedEmail = email.trim().lowercase()
            val now = System.currentTimeMillis()

            val otpEntity = dao.getLatestActiveEmailOtp(normalizedEmail)
                ?: return@withContext Result.success(OtpVerificationResult.Error("Aucun code actif trouvé. Demandez un nouveau code."))

            // Check purpose
            if (otpEntity.purpose != purpose) {
                return@withContext Result.success(OtpVerificationResult.Error("Code invalide pour cette opération."))
            }

            // Check expiration
            if (now > otpEntity.expiresAt) {
                dao.updateEmailOtp(otpEntity.copy(isInvalidated = true))
                dao.insertAuditLog(
                    AuditLogEntity(
                        userId = userId,
                        userEmail = normalizedEmail,
                        action = AuditAction.OTP_VERIFIED_FAILURE,
                        details = "Code OTP expiré (> 10 minutes)",
                        severity = AuditSeverity.WARNING
                    )
                )
                return@withContext Result.success(OtpVerificationResult.Expired)
            }

            // Check max attempts
            if (otpEntity.attempts >= otpEntity.maxAttempts) {
                dao.updateEmailOtp(otpEntity.copy(isInvalidated = true))
                dao.insertAuditLog(
                    AuditLogEntity(
                        userId = userId,
                        userEmail = normalizedEmail,
                        action = AuditAction.SUSPICIOUS_ATTEMPT,
                        details = "Nombre maximum de tentatives OTP dépassé (5/5). Code invalidé.",
                        severity = AuditSeverity.CRITICAL
                    )
                )
                return@withContext Result.success(OtpVerificationResult.MaxAttemptsExceeded)
            }

            // Verify salted hash
            val isValid = SecurityUtils.verifyOtp(inputCode, otpEntity.salt, otpEntity.hashedOtp)

            if (!isValid) {
                val newAttempts = otpEntity.attempts + 1
                val isNowInvalidated = newAttempts >= otpEntity.maxAttempts
                dao.updateEmailOtp(otpEntity.copy(attempts = newAttempts, isInvalidated = isNowInvalidated))

                dao.insertAuditLog(
                    AuditLogEntity(
                        userId = userId,
                        userEmail = normalizedEmail,
                        action = AuditAction.OTP_VERIFIED_FAILURE,
                        details = "Code OTP erroné. Tentative $newAttempts/${otpEntity.maxAttempts}",
                        severity = AuditSeverity.WARNING
                    )
                )

                return@withContext if (isNowInvalidated) {
                    Result.success(OtpVerificationResult.MaxAttemptsExceeded)
                } else {
                    Result.success(OtpVerificationResult.InvalidCode(otpEntity.maxAttempts - newAttempts))
                }
            }

            // Valid code! Mark as verified and invalidate so it cannot be re-used
            dao.updateEmailOtp(
                otpEntity.copy(
                    verifiedAt = now,
                    isInvalidated = true
                )
            )

            // Update user's email verification state in DB
            dao.updateEmailVerification(userId, true)

            dao.insertAuditLog(
                AuditLogEntity(
                    userId = userId,
                    userEmail = normalizedEmail,
                    action = AuditAction.OTP_VERIFIED_SUCCESS,
                    details = "Code OTP validé avec succès pour ${purpose.label}",
                    severity = AuditSeverity.INFO
                )
            )

            Result.success(OtpVerificationResult.Success)
        }
    }

    override suspend fun generateDeliveryOtp(
        deliveryId: String,
        orderId: String,
        clientId: String,
        driverId: String?
    ): Result<DeliveryOtpResult> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val rawOtp = SecurityUtils.generateSecureNumericOtp(DELIVERY_OTP_LENGTH)
        val salt = SecurityUtils.generateSalt()
        val hashedOtp = SecurityUtils.hashOtp(rawOtp, salt)
        val expiresAt = now + DELIVERY_OTP_EXPIRATION_MS

        val entity = DeliveryOtpEntity(
            id = "dotp_" + UUID.randomUUID().toString().take(8),
            deliveryId = deliveryId,
            orderId = orderId,
            clientId = clientId,
            driverId = driverId,
            hashedOtp = hashedOtp,
            salt = salt,
            createdAt = now,
            expiresAt = expiresAt,
            attempts = 0,
            maxAttempts = MAX_ATTEMPTS,
            status = DeliveryOtpStatus.PENDING
        )
        dao.insertDeliveryOtp(entity)

        dao.insertAuditLog(
            AuditLogEntity(
                userId = clientId,
                action = AuditAction.DELIVERY_OTP_GENERATED,
                details = "Code de remise sécurisé généré pour la course #$orderId",
                severity = AuditSeverity.INFO
            )
        )

        Result.success(
            DeliveryOtpResult(
                id = entity.id,
                deliveryId = deliveryId,
                rawOtpCode = rawOtp,
                expiresAt = expiresAt,
                attemptsRemaining = MAX_ATTEMPTS
            )
        )
    }

    override suspend fun verifyDeliveryOtp(
        deliveryId: String,
        driverId: String,
        inputCode: String
    ): Result<OtpVerificationResult> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val otpEntity = dao.getDeliveryOtpByDeliveryId(deliveryId)

            val delivery = dao.getDeliveryById(deliveryId)
                ?: return@withContext Result.success(OtpVerificationResult.Error("Livraison introuvable."))

            // Verify driver assignment
            if (delivery.driverId != driverId) {
                dao.insertAuditLog(
                    AuditLogEntity(
                        userId = driverId,
                        action = AuditAction.UNAUTHORIZED_ACCESS,
                        details = "Tentative de validation de livraison par un livreur non assigné (course: $deliveryId)",
                        severity = AuditSeverity.CRITICAL
                    )
                )
                return@withContext Result.success(OtpVerificationResult.Error("Vous n'êtes pas le livreur assigné à cette course."))
            }

            if (delivery.status == DeliveryStatus.DELIVERED) {
                return@withContext Result.success(OtpVerificationResult.Error("Cette livraison a déjà été validée et clôturée."))
            }

            // Verify with entity or fallback to delivery.otpCode
            val isValid = if (otpEntity != null) {
                if (otpEntity.status == DeliveryOtpStatus.VERIFIED) {
                    return@withContext Result.success(OtpVerificationResult.Error("Code déjà utilisé."))
                }
                if (now > otpEntity.expiresAt) {
                    dao.updateDeliveryOtp(otpEntity.copy(status = DeliveryOtpStatus.EXPIRED))
                    return@withContext Result.success(OtpVerificationResult.Expired)
                }
                if (otpEntity.attempts >= otpEntity.maxAttempts) {
                    return@withContext Result.success(OtpVerificationResult.MaxAttemptsExceeded)
                }

                val match = SecurityUtils.verifyOtp(inputCode, otpEntity.salt, otpEntity.hashedOtp)
                if (!match) {
                    val nextAttempts = otpEntity.attempts + 1
                    dao.updateDeliveryOtp(otpEntity.copy(attempts = nextAttempts))
                    if (nextAttempts >= otpEntity.maxAttempts) {
                        return@withContext Result.success(OtpVerificationResult.MaxAttemptsExceeded)
                    }
                    return@withContext Result.success(OtpVerificationResult.InvalidCode(otpEntity.maxAttempts - nextAttempts))
                } else {
                    dao.updateDeliveryOtp(
                        otpEntity.copy(
                            status = DeliveryOtpStatus.VERIFIED,
                            verifiedAt = now,
                            verifiedByDriverId = driverId
                        )
                    )
                    true
                }
            } else {
                // Direct fallback verification using delivery.otpCode
                if (delivery.otpAttempts >= MAX_ATTEMPTS) {
                    return@withContext Result.success(OtpVerificationResult.MaxAttemptsExceeded)
                }
                if (delivery.otpCode != inputCode.trim()) {
                    val next = delivery.otpAttempts + 1
                    dao.updateDelivery(delivery.copy(otpAttempts = next))
                    if (next >= MAX_ATTEMPTS) {
                        return@withContext Result.success(OtpVerificationResult.MaxAttemptsExceeded)
                    }
                    return@withContext Result.success(OtpVerificationResult.InvalidCode(MAX_ATTEMPTS - next))
                }
                true
            }

            if (isValid) {
                dao.insertAuditLog(
                    AuditLogEntity(
                        userId = driverId,
                        userRole = UserRole.DRIVER,
                        action = AuditAction.DELIVERY_OTP_VERIFIED_SUCCESS,
                        details = "Code de remise validé avec succès par le livreur $driverId pour la course #${delivery.trackingNumber}",
                        severity = AuditSeverity.INFO
                    )
                )
                Result.success(OtpVerificationResult.Success)
            } else {
                Result.success(OtpVerificationResult.InvalidCode(0))
            }
        }
    }
}
