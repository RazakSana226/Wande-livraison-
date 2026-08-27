package com.example.service.auth

import com.example.model.UserRole

data class AuthUser(
    val id: String,
    val email: String,
    val name: String,
    val phone: String,
    val role: UserRole,
    val isEmailVerified: Boolean = false,
    val firebaseUid: String? = null,
    val photoUrl: String? = null,
    val token: String? = null
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: AuthUser) : AuthState()
    data class RequiresEmailVerification(val user: AuthUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class AuthEvent {
    data class Success(val user: AuthUser) : AuthEvent()
    data class VerificationEmailSent(val email: String) : AuthEvent()
    data class PasswordResetSent(val email: String) : AuthEvent()
    data class Error(val message: String) : AuthEvent()
}
