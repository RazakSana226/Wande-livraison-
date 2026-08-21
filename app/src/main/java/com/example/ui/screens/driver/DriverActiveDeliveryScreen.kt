package com.example.ui.screens.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeliveryEntity
import com.example.model.DeliveryStatus
import com.example.model.LatLngPoint
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.components.WandeInteractiveMap
import com.example.ui.theme.*
import com.example.ui.viewmodel.WandeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverActiveDeliveryScreen(
    deliveryId: String,
    viewModel: WandeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deliveries by viewModel.allDeliveries.collectAsState()
    val delivery = deliveries.find { it.id == deliveryId }

    var showOtpDialog by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }
    var showDisputeDialog by remember { mutableStateOf(false) }
    var disputeReason by remember { mutableStateOf("") }
    var disputeDetails by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Course #${delivery?.trackingNumber ?: ""}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Gain net : +${delivery?.driverEarningsXof ?: 0} FCFA",
                            style = MaterialTheme.typography.labelSmall,
                            color = WandePrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("driver_nav_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showDisputeDialog = true }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Signaler un problème")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (delivery != null && delivery.status != DeliveryStatus.DELIVERED && delivery.status != DeliveryStatus.CANCELLED) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        when (delivery.status) {
                            DeliveryStatus.DRIVER_ASSIGNED -> {
                                Button(
                                    onClick = {
                                        viewModel.updateDriverProgress(delivery.id, DeliveryStatus.DRIVER_ARRIVING)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("action_step_arriving"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WandePrimary)
                                ) {
                                    Icon(Icons.Default.TwoWheeler, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("En route vers récupération", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            DeliveryStatus.DRIVER_ARRIVING -> {
                                Button(
                                    onClick = {
                                        viewModel.updateDriverProgress(delivery.id, DeliveryStatus.PACKAGE_PICKED_UP)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("action_step_picked_up"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                                ) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Colis récupéré au départ", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            DeliveryStatus.PACKAGE_PICKED_UP -> {
                                Button(
                                    onClick = {
                                        viewModel.updateDriverProgress(delivery.id, DeliveryStatus.IN_TRANSIT)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("action_step_in_transit"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E7490))
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("En cours de livraison vers destination", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            DeliveryStatus.IN_TRANSIT -> {
                                Button(
                                    onClick = {
                                        viewModel.updateDriverProgress(delivery.id, DeliveryStatus.DRIVER_ARRIVED)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("action_step_arrived"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA16207))
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Arrivé à destination", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            DeliveryStatus.DRIVER_ARRIVED -> {
                                Button(
                                    onClick = { showOtpDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("action_step_validate_otp"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WandeAccent, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Saisir le Code OTP de Réception", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (delivery == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WandePrimary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Route Map
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        WandeInteractiveMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            pickupPoint = LatLngPoint(delivery.pickupLat, delivery.pickupLng, delivery.pickupAddress),
                            destinationPoint = LatLngPoint(delivery.destinationLat, delivery.destinationLng, delivery.destinationAddress),
                            driverLat = delivery.currentDriverLat,
                            driverLng = delivery.currentDriverLng,
                            deliveryStatus = delivery.status
                        )
                    }
                }
            }

            // Current Active Stage Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Étape en cours :",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DeliveryStatusBadge(status = delivery.status)
                        }
                        Text(
                            text = delivery.status.description,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Client (Expéditeur) Card with Call
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Expéditeur (Client)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WandePrimary
                                )
                                Text(
                                    text = delivery.clientName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = delivery.clientPhone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { /* Call client phone */ },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(WandePrimary.copy(alpha = 0.12f))
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Appeler expéditeur", tint = WandePrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lieu de récupération : ${delivery.pickupAddress}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                        if (delivery.pickupInstructions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "Notes : ${delivery.pickupInstructions}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Recipient (Destinataire) Card with Call
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Destinataire",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusError
                                )
                                Text(
                                    text = delivery.recipientName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = delivery.recipientPhone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { /* Call recipient phone */ },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(StatusError.copy(alpha = 0.12f))
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Appeler destinataire", tint = StatusError)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lieu de livraison : ${delivery.destinationAddress}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                        if (delivery.specialNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = WandeAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Repère : ${delivery.specialNotes}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = WandeAccentDark,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Package Details & Financials
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Colis & Rémunération",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${delivery.packageDescription} (${delivery.packageSize.label})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Votre gain net (90%) :", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "+${delivery.driverEarningsXof} FCFA",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = WandePrimary)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Commission WÀNDÉ (10%) :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("-${delivery.platformFeeXof} FCFA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // OTP Entry Dialog (Server-Validated)
    if (showOtpDialog) {
        AlertDialog(
            onDismissRequest = { showOtpDialog = false },
            title = {
                Text(
                    text = "Valider la remise du colis 🔒",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Demandez le code secret à 4 chiffres au destinataire (${delivery?.recipientName}) :",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { if (it.length <= 4) otpInput = it },
                        placeholder = { Text("Code à 4 chiffres", textAlign = TextAlign.Center) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("otp_input_field"),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 6.sp
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (otpInput.isNotBlank()) {
                            viewModel.completeDeliveryWithOtp(deliveryId, otpInput)
                            showOtpDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WandePrimary),
                    modifier = Modifier.testTag("submit_otp_button")
                ) {
                    Text("Valider la livraison")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dispute dialog
    if (showDisputeDialog) {
        AlertDialog(
            onDismissRequest = { showDisputeDialog = false },
            title = { Text("Signaler un problème sur cette course") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("L'équipe support WÀNDÉ examinera ce dossier.")
                    OutlinedTextField(
                        value = disputeReason,
                        onValueChange = { disputeReason = it },
                        label = { Text("Motif (ex: client injoignable, adresse fausse)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = disputeDetails,
                        onValueChange = { disputeDetails = it },
                        label = { Text("Détails") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reportDispute(deliveryId, disputeReason, disputeDetails)
                        showDisputeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WandePrimary)
                ) {
                    Text("Envoyer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisputeDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}
