package com.example.service.auth

import com.example.model.UserRole
import kotlinx.coroutines.flow.StateFlow

/**
 * Pluggable Authentication Provider Interface
 * Allows seamless switching between Firebase Auth, Custom Backend Auth, or Sandbox Auth
 */
interface AuthProvider {
    val currentAuthUser: StateFlow<AuthUser?>
    val authState: StateFlow<AuthState>

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        name: String,
        phone: String,
        role: UserRole
    ): Result<AuthUser>

    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<AuthUser>

    suspend fun signOut(): Result<Unit>

    suspend fun sendEmailVerification(): Result<Unit>

    suspend fun reloadUser(): Result<AuthUser?>

    suspend fun isEmailVerified(): Boolean

    suspend fun sendPasswordReset(email: String): Result<Unit>

    suspend fun updateEmail(newEmail: String): Result<Unit>

    suspend fun confirmEmailVerifiedManually(userId: String): Result<Unit>
}
