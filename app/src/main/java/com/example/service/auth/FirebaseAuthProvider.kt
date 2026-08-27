package com.example.service.auth

import com.example.data.local.WandeDao
import com.example.model.AuditAction
import com.example.model.AuditLogEntity
import com.example.model.AuditSeverity
import com.example.model.UserEntity
import com.example.model.UserRole
import com.example.service.email.EmailProvider
import com.example.service.otp.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class FirebaseAuthProvider(
    private val dao: WandeDao,
    private val emailProvider: EmailProvider,
    private val scope: CoroutineScope
) : AuthProvider {

    private val _currentAuthUser = MutableStateFlow<AuthUser?>(null)
    override val currentAuthUser: StateFlow<AuthUser?> = _currentAuthUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Initialize with default demo client if exists
        scope.launch(Dispatchers.IO) {
            val user = dao.getUserById("usr_client_1")
            user?.let {
                val authUser = AuthUser(
                    id = it.id,
                    email = it.email ?: "amadou.ouedraogo@wande.bf",
                    name = it.name,
                    phone = it.phone,
                    role = it.role,
                    isEmailVerified = it.isEmailVerified,
                    photoUrl = it.photoUrl
                )
                _currentAuthUser.value = authUser
                _authState.value = if (it.isEmailVerified) AuthState.Authenticated(authUser) else AuthState.RequiresEmailVerification(authUser)
            }
        }
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        name: String,
        phone: String,
        role: UserRole
    ): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading
            val normalizedEmail = email.trim().lowercase()

            if (!isValidEmail(normalizedEmail)) {
                _authState.value = AuthState.Error("Adresse email invalide.")
                return@withContext Result.failure(IllegalArgumentException("Format d'email invalide."))
            }

            if (password.length < 6) {
                _authState.value = AuthState.Error("Le mot de passe doit contenir au moins 6 caractères.")
                return@withContext Result.failure(IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères."))
            }

            // Check if user already exists
            val existing = dao.getUserByEmail(normalizedEmail)
            if (existing != null) {
                _authState.value = AuthState.Error("Un compte existe déjà avec cette adresse email.")
                return@withContext Result.failure(IllegalStateException("Un compte existe déjà avec cet email."))
            }

            val newUserId = "usr_" + UUID.randomUUID().toString().take(8)
            val newUser = UserEntity(
                id = newUserId,
                role = role,
                name = name.trim(),
                phone = phone.trim(),
                email = normalizedEmail,
                isEmailVerified = false,
                status = "ACTIVE"
            )
            dao.insertUser(newUser)

            // Audit log
            dao.insertAuditLog(
                AuditLogEntity(
                    userId = newUserId,
                    userEmail = normalizedEmail,
                    userRole = role,
                    action = AuditAction.SIGNUP,
                    details = "Nouveau compte créé via Email/Password ($role)",
                    severity = AuditSeverity.INFO
                )
            )

            val authUser = AuthUser(
                id = newUser.id,
                email = normalizedEmail,
                name = newUser.name,
                phone = newUser.phone,
                role = newUser.role,
                isEmailVerified = false
            )
            _currentAuthUser.value = authUser
            _authState.value = AuthState.RequiresEmailVerification(authUser)

            // Trigger transactional verification email via BrevoEmailProvider
            emailProvider.sendVerificationEmail(
                email = normalizedEmail,
                recipientName = name,
                verificationToken = UUID.randomUUID().toString()
            )

            Result.success(authUser)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Erreur d'inscription.")
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading
            val normalizedEmail = email.trim().lowercase()

            val user = dao.getUserByEmail(normalizedEmail)
            if (user == null) {
                dao.insertAuditLog(
                    AuditLogEntity(
                        userEmail = normalizedEmail,
                        action = AuditAction.SUSPICIOUS_ATTEMPT,
                        details = "Tentative de connexion avec email inconnu: $normalizedEmail",
                        severity = AuditSeverity.WARNING
                    )
                )
                _authState.value = AuthState.Error("Identifiants incorrects ou compte inexistant.")
                return@withContext Result.failure(IllegalArgumentException("Identifiants incorrects."))
            }

            val authUser = AuthUser(
                id = user.id,
                email = user.email ?: normalizedEmail,
                name = user.name,
                phone = user.phone,
                role = user.role,
                isEmailVerified = user.isEmailVerified,
                photoUrl = user.photoUrl
            )
            _currentAuthUser.value = authUser

            dao.insertAuditLog(
                AuditLogEntity(
                    userId = user.id,
                    userEmail = normalizedEmail,
                    userRole = user.role,
                    action = AuditAction.LOGIN,
                    details = "Connexion réussie (Vérifié: ${user.isEmailVerified})",
                    severity = AuditSeverity.INFO
                )
            )

            if (user.isEmailVerified) {
                _authState.value = AuthState.Authenticated(authUser)
            } else {
                _authState.value = AuthState.RequiresEmailVerification(authUser)
            }

            Result.success(authUser)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Erreur de connexion.")
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentAuthUser.value
        user?.let {
            dao.insertAuditLog(
                AuditLogEntity(
                    userId = it.id,
                    userEmail = it.email,
                    userRole = it.role,
                    action = AuditAction.LOGOUT,
                    details = "Déconnexion de l'utilisateur",
                    severity = AuditSeverity.INFO
                )
            )
        }
        _currentAuthUser.value = null
        _authState.value = AuthState.Idle
        Result.success(Unit)
    }

    override suspend fun sendEmailVerification(): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentAuthUser.value
            ?: return@withContext Result.failure(IllegalStateException("Aucun utilisateur connecté."))

        val result = emailProvider.sendVerificationEmail(
            email = user.email,
            recipientName = user.name,
            verificationToken = UUID.randomUUID().toString()
        )

        if (result.isSuccess) {
            dao.insertAuditLog(
                AuditLogEntity(
                    userId = user.id,
                    userEmail = user.email,
                    userRole = user.role,
                    action = AuditAction.EMAIL_VERIFICATION_SENT,
                    details = "Email de vérification envoyé à ${user.email}",
                    severity = AuditSeverity.INFO
                )
            )
            Result.success(Unit)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Erreur lors de l'envoi de l'email."))
        }
    }

    override suspend fun reloadUser(): Result<AuthUser?> = withContext(Dispatchers.IO) {
        val currentUser = _currentAuthUser.value ?: return@withContext Result.success(null)
        val user = dao.getUserById(currentUser.id)
        if (user != null) {
            val updated = currentUser.copy(
                email = user.email ?: currentUser.email,
                name = user.name,
                phone = user.phone,
                isEmailVerified = user.isEmailVerified
            )
            _currentAuthUser.value = updated
            _authState.value = if (updated.isEmailVerified) AuthState.Authenticated(updated) else AuthState.RequiresEmailVerification(updated)
            Result.success(updated)
        } else {
            Result.success(currentUser)
        }
    }

    override suspend fun isEmailVerified(): Boolean {
        return _currentAuthUser.value?.isEmailVerified == true
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val user = dao.getUserByEmail(normalizedEmail)
        if (user == null) {
            // Anti-enumeration: still return success to prevent email probing
            return@withContext Result.success(Unit)
        }

        emailProvider.sendPasswordResetEmail(normalizedEmail, UUID.randomUUID().toString())
        dao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                userEmail = normalizedEmail,
                userRole = user.role,
                action = AuditAction.EMAIL_VERIFICATION_SENT,
                details = "Demande de réinitialisation de mot de passe",
                severity = AuditSeverity.INFO
            )
        )
        Result.success(Unit)
    }

    override suspend fun updateEmail(newEmail: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentAuthUser.value
            ?: return@withContext Result.failure(IllegalStateException("Non connecté."))

        val normalized = newEmail.trim().lowercase()
        if (!isValidEmail(normalized)) {
            return@withContext Result.failure(IllegalArgumentException("Format d'email invalide."))
        }

        val existing = dao.getUserByEmail(normalized)
        if (existing != null && existing.id != user.id) {
            return@withContext Result.failure(IllegalStateException("Cette adresse email est déjà utilisée."))
        }

        dao.updateUserEmail(user.id, normalized)
        val updated = user.copy(email = normalized, isEmailVerified = false)
        _currentAuthUser.value = updated
        _authState.value = AuthState.RequiresEmailVerification(updated)

        // Send verification to new email
        emailProvider.sendVerificationEmail(normalized, user.name, UUID.randomUUID().toString())

        dao.insertAuditLog(
            AuditLogEntity(
                userId = user.id,
                userEmail = normalized,
                userRole = user.role,
                action = AuditAction.EMAIL_VERIFICATION_SENT,
                details = "Mise à jour de l'email vers $normalized (Non vérifié)",
                severity = AuditSeverity.INFO
            )
        )

        Result.success(Unit)
    }

    override suspend fun confirmEmailVerifiedManually(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId)
            ?: return@withContext Result.failure(Exception("Utilisateur non trouvé."))

        dao.updateEmailVerification(userId, true)
        val current = _currentAuthUser.value
        if (current != null && current.id == userId) {
            val updated = current.copy(isEmailVerified = true)
            _currentAuthUser.value = updated
            _authState.value = AuthState.Authenticated(updated)
        }

        dao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                userEmail = user.email,
                userRole = user.role,
                action = AuditAction.EMAIL_VERIFIED,
                details = "Adresse email vérifiée avec succès (${user.email})",
                severity = AuditSeverity.INFO
            )
        )

        Result.success(Unit)
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
