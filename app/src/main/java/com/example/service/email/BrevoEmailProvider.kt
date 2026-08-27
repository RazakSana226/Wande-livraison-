package com.example.service.email

import android.util.Log
import com.example.model.OtpPurpose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Brevo (formerly Sendinblue) Transactional Email Provider
 * Designed for serverless backend / Cloud Function execution or direct secure API proxy.
 *
 * NOTE: API keys should be injected via BuildConfig or backend environment variables.
 * Never hardcode production API credentials in the frontend.
 */
class BrevoEmailProvider(
    private val apiKeyProvider: () -> String = { "" },
    private val senderEmail: String = "contact@wande.bf",
    private val senderName: String = "WÀNDÉ Livraison Express"
) : EmailProvider {

    private val tag = "BrevoEmailProvider"

    override suspend fun sendVerificationEmail(
        email: String,
        recipientName: String,
        verificationToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val subject = "Vérifiez votre adresse email — WÀNDÉ"
        val htmlContent = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; background-color: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0;">
                <div style="text-align: center; margin-bottom: 24px;">
                    <h1 style="color: #1E3A8A; font-size: 28px; margin: 0; font-weight: 800;">WÀNDÉ</h1>
                    <p style="color: #64748B; font-size: 14px; margin-top: 4px;">Plateforme de livraison urbaine sécurisée</p>
                </div>
                <div style="padding: 20px 0;">
                    <p style="font-size: 16px; color: #1E293B;">Bonjour <strong>$recipientName</strong>,</p>
                    <p style="font-size: 15px; color: #475569; line-height: 1.6;">
                        Merci d'avoir rejoint WÀNDÉ ! Pour activer pleinement votre compte et sécuriser vos courses, veuillez confirmer votre adresse email.
                    </p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="https://wande.bf/verify?token=$verificationToken&email=$email" 
                           style="background-color: #F97316; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px; display: inline-block;">
                            Confirmer mon adresse email
                        </a>
                    </div>
                    <p style="font-size: 13px; color: #94A3B8; text-align: center;">
                        Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br/>
                        https://wande.bf/verify?token=$verificationToken
                    </p>
                </div>
                <div style="border-top: 1px solid #e2e8f0; padding-top: 16px; margin-top: 24px; text-align: center;">
                    <p style="font-size: 12px; color: #94A3B8;">WÀNDÉ • Ouagadougou, Burkina Faso • Support : support@wande.bf</p>
                </div>
            </div>
        """.trimIndent()

        sendViaBrevoApi(email, recipientName, subject, htmlContent)
    }

    override suspend fun sendEmailOtp(
        email: String,
        recipientName: String,
        otpCode: String,
        purpose: OtpPurpose
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val subject = "[Code $otpCode] Votre code de sécurité WÀNDÉ"
        val htmlContent = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; background-color: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0;">
                <div style="text-align: center; margin-bottom: 24px;">
                    <h1 style="color: #1E3A8A; font-size: 28px; margin: 0; font-weight: 800;">WÀNDÉ</h1>
                    <p style="color: #64748B; font-size: 14px; margin-top: 4px;">Sécurité & Authentification</p>
                </div>
                <div style="padding: 20px 0; text-align: center;">
                    <p style="font-size: 16px; color: #1E293B;">Bonjour <strong>$recipientName</strong>,</p>
                    <p style="font-size: 15px; color: #475569;">Voici votre code temporaire pour : <strong>${purpose.label}</strong></p>
                    
                    <div style="background-color: #F8FAFC; border: 2px dashed #CBD5E1; border-radius: 12px; padding: 20px; margin: 24px auto; max-width: 280px;">
                        <span style="font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #1E3A8A; font-family: monospace;">$otpCode</span>
                    </div>
                    
                    <p style="font-size: 14px; color: #DC2626; font-weight: 600;">
                        ⏱️ Ce code expire dans 10 minutes.
                    </p>
                    <p style="font-size: 13px; color: #64748B; margin-top: 16px;">
                        Ne partagez jamais ce code. L'équipe WÀNDÉ ne vous le demandera jamais par téléphone ou message.
                    </p>
                </div>
                <div style="border-top: 1px solid #e2e8f0; padding-top: 16px; margin-top: 24px; text-align: center;">
                    <p style="font-size: 12px; color: #94A3B8;">WÀNDÉ • Ouagadougou, Burkina Faso</p>
                </div>
            </div>
        """.trimIndent()

        sendViaBrevoApi(email, recipientName, subject, htmlContent)
    }

    override suspend fun sendPasswordResetEmail(
        email: String,
        resetToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val subject = "Réinitialisation de votre mot de passe WÀNDÉ"
        val htmlContent = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px;">
                <h2 style="color: #1E3A8A;">Réinitialisation de mot de passe</h2>
                <p>Une demande de réinitialisation de mot de passe a été effectuée pour votre compte WÀNDÉ ($email).</p>
                <div style="text-align: center; margin: 24px 0;">
                    <a href="https://wande.bf/reset-password?token=$resetToken" 
                       style="background-color: #1E3A8A; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;">
                        Changer mon mot de passe
                    </a>
                </div>
                <p style="font-size: 13px; color: #64748B;">Si vous n'êtes pas à l'origine de cette demande, ignorez cet email en toute sécurité.</p>
            </div>
        """.trimIndent()

        sendViaBrevoApi(email, "Utilisateur WÀNDÉ", subject, htmlContent)
    }

    override suspend fun sendDeliveryConfirmation(
        email: String,
        clientName: String,
        trackingNumber: String,
        amountXof: Int
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val subject = "Colis livré avec succès ! [Course #$trackingNumber]"
        val htmlContent = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px;">
                <h2 style="color: #16A34A; text-align: center;">✓ Livraison confirmée</h2>
                <p>Bonjour <strong>$clientName</strong>,</p>
                <p>Votre livraison <strong>#$trackingNumber</strong> a été remise et validée avec succès par le code de sécurité.</p>
                <div style="background-color: #F8FAFC; padding: 16px; border-radius: 8px; margin: 16px 0;">
                    <p style="margin: 4px 0;"><strong>Course :</strong> #$trackingNumber</p>
                    <p style="margin: 4px 0;"><strong>Montant :</strong> $amountXof FCFA</p>
                    <p style="margin: 4px 0;"><strong>Statut :</strong> Livré & Clôturé</p>
                </div>
                <p style="font-size: 14px; color: #64748B;">Merci d'avoir fait confiance à WÀNDÉ.</p>
            </div>
        """.trimIndent()

        sendViaBrevoApi(email, clientName, subject, htmlContent)
    }

    override suspend fun sendSecurityAlert(
        email: String,
        alertType: String,
        details: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val subject = "⚠️ Alerte de sécurité — Compte WÀNDÉ"
        val htmlContent = """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; background-color: #ffffff; border: 1px solid #ef4444; border-radius: 12px;">
                <h2 style="color: #DC2626;">Alerte de sécurité</h2>
                <p>Un événement de sécurité a été détecté sur votre compte : <strong>$alertType</strong></p>
                <p style="color: #475569;">$details</p>
                <p style="font-size: 13px; color: #64748B; margin-top: 16px;">Si vous n'êtes pas à l'origine de cette action, connectez-vous immédiatement pour sécuriser votre compte.</p>
            </div>
        """.trimIndent()

        sendViaBrevoApi(email, "Client WÀNDÉ", subject, htmlContent)
    }

    /**
     * Executes the HTTP REST POST request to Brevo API v3 or operates in sandbox mode if no API key is provided
     */
    private suspend fun sendViaBrevoApi(
        toEmail: String,
        toName: String,
        subject: String,
        htmlContent: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()

        if (apiKey.isBlank()) {
            // Development / Sandbox mode: Log transactional dispatch without failing
            Log.d(tag, "[SANDBOX BREVO DISPATCH] To: $toEmail ($toName) | Subject: $subject")
            return@withContext Result.success(true)
        }

        try {
            val url = URL("https://api.brevo.com/v3/smtp/email")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("accept", "application/json")
            conn.setRequestProperty("api-key", apiKey)
            conn.setRequestProperty("content-type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val payload = JSONObject().apply {
                put("sender", JSONObject().apply {
                    put("name", senderName)
                    put("email", senderEmail)
                })
                put("to", JSONArray().apply {
                    put(JSONObject().apply {
                        put("email", toEmail)
                        put("name", toName)
                    })
                })
                put("subject", subject)
                put("htmlContent", htmlContent)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                Log.i(tag, "Email successfully sent via Brevo to $toEmail (HTTP $responseCode)")
                Result.success(true)
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                Log.e(tag, "Brevo API Error ($responseCode): $errorStream")
                Result.failure(Exception("Brevo API Error: $responseCode - $errorStream"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to connect to Brevo API: ${e.message}", e)
            Result.failure(e)
        }
    }
}
