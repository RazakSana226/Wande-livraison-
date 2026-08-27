package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.OtpPurpose
import com.example.service.otp.OtpVerificationResult
import com.example.service.otp.maskEmail
import com.example.ui.viewmodel.WandeViewModel

@Composable
fun EmailOtpDialog(
    email: String,
    purpose: OtpPurpose,
    viewModel: WandeViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var otpInput by remember { mutableStateOf("") }
    val isOtpLoading by viewModel.isOtpLoading.collectAsState()
    val otpState by viewModel.otpState.collectAsState()
    val cooldownSeconds by viewModel.otpCooldownSeconds.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("email_otp_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Sécurité",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("otp_dialog_close_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Code de vérification",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Saisissez le code à 6 chiffres envoyé à :\n${maskEmail(email)}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // OTP Display Boxes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (0 until 6).forEach { index ->
                        val char = otpInput.getOrNull(index)?.toString() ?: ""
                        val isCurrent = index == otpInput.length
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (char.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else if (char.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Hidden/Focused text field for typing
                OutlinedTextField(
                    value = otpInput,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            otpInput = it
                            if (it.length == 6) {
                                viewModel.verifyEmailOtp(email, it, purpose) {
                                    onSuccess()
                                    onDismiss()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .padding(vertical = 8.dp)
                        .testTag("otp_input_field"),
                    placeholder = { Text("Code à 6 chiffres", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (otpInput.length == 6) {
                                viewModel.verifyEmailOtp(email, otpInput, purpose) {
                                    onSuccess()
                                    onDismiss()
                                }
                            }
                        }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Error message display
                AnimatedVisibility(visible = otpState is OtpVerificationResult.InvalidCode || otpState is OtpVerificationResult.MaxAttemptsExceeded || otpState is OtpVerificationResult.Expired) {
                    val errorMsg = when (val s = otpState) {
                        is OtpVerificationResult.InvalidCode -> "Code erroné (${s.attemptsRemaining} essais restants)"
                        is OtpVerificationResult.MaxAttemptsExceeded -> "Nombre maximal d'essais dépassé."
                        is OtpVerificationResult.Expired -> "Ce code a expiré."
                        else -> ""
                    }
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Resend Button with Cooldown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (cooldownSeconds <= 0) {
                                viewModel.requestEmailOtp(email, purpose)
                            }
                        },
                        enabled = cooldownSeconds <= 0 && !isOtpLoading,
                        modifier = Modifier.testTag("resend_otp_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Renvoyer",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (cooldownSeconds > 0) "Renvoyer (${cooldownSeconds}s)" else "Renvoyer le code",
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (otpInput.length == 6) {
                                viewModel.verifyEmailOtp(email, otpInput, purpose) {
                                    onSuccess()
                                    onDismiss()
                                }
                            }
                        },
                        enabled = otpInput.length == 6 && !isOtpLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("verify_otp_submit_button")
                    ) {
                        if (isOtpLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Valider")
                        }
                    }
                }
            }
        }
    }
}
