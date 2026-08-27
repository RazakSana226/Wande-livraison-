package com.example.service.otp

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object SecurityUtils {

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically secure numeric OTP of the specified length.
     * Uses java.security.SecureRandom (never Math.random).
     */
    fun generateSecureNumericOtp(digits: Int = 6): String {
        require(digits in 4..8) { "OTP length must be between 4 and 8 digits." }
        val sb = StringBuilder(digits)
        for (i in 0 until digits) {
            sb.append(secureRandom.nextInt(10))
        }
        return sb.toString()
    }

    /**
     * Generates a random cryptographic salt.
     */
    fun generateSalt(byteLength: Int = 16): String {
        val salt = ByteArray(byteLength)
        secureRandom.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    /**
     * Hashes an OTP with a salt using SHA-256.
     * Stored in DB to prevent plaintext exposure.
     */
    fun hashOtp(otp: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val hashedBytes = md.digest(otp.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashedBytes)
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(aBytes, bBytes)
    }

    /**
     * Verifies an OTP against its stored salt and hash.
     */
    fun verifyOtp(inputOtp: String, salt: String, expectedHash: String): Boolean {
        val computedHash = hashOtp(inputOtp.trim(), salt)
        return constantTimeEquals(computedHash, expectedHash)
    }
}
