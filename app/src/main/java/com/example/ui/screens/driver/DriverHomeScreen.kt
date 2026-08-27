package com.example.ui.screens.driver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeliveryEntity
import com.example.model.DeliveryStatus
import com.example.model.DriverVerificationStatus
import com.example.ui.components.DriverVerificationBadge
import com.example.ui.components.MetricStatCard
import com.example.ui.components.WandeInteractiveMap
import com.example.ui.theme.*
import com.example.ui.viewmodel.WandeViewModel

@Composable
fun DriverHomeScreen(
    viewModel: WandeViewModel,
    onNavigateToActiveDelivery: (String) -> Unit,
    onNavigateToEarnings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToOnboarding: () -> Unit = onNavigateToProfile,
    modifier: Modifier = Modifier
) {
    val driver by viewModel.currentDriver.collectAsState()
    val activeDelivery by viewModel.activeDriverDelivery.collectAsState()
    val openDeliveries by viewModel.openDeliveries.collectAsState()
    val isOnline = driver?.isOnline ?: false

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Driver Status & Online Toggle Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("driver_status_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnline) WandePrimaryDark else Color(0xFF0F172A)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) WandePrimaryLight else Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = driver?.name ?: "Livreur WÀNDÉ",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "${driver?.vehicleModel ?: "Yamaha 125"} • ★ ${driver?.rating ?: 5.0}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Switch(
                            checked = isOnline,
                            onCheckedChange = { viewModel.toggleDriverOnline(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = WandeCyan,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.testTag("driver_online_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444))
                            )
                            Text(
                                text = if (isOnline) "🟢 EN LIGNE (Prêt à recevoir)" else "🔴 HORS LIGNE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        driver?.verificationStatus?.let { status ->
                            DriverVerificationBadge(status = status)
                        }
                    }
                }
            }
        }

        // KYC Verification Banner (if pending, rejected, or action required)
        if (driver?.verificationStatus != DriverVerificationStatus.VERIFIED) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToOnboarding() }
                        .testTag("driver_kyc_verification_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (driver?.verificationStatus == DriverVerificationStatus.ACTION_REQUIRED) Color(0xFFFFF7ED) else Color(0xFFEFF6FF)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (driver?.verificationStatus == DriverVerificationStatus.ACTION_REQUIRED) Color(0xFFFDBA74) else Color(0xFFBFDBFE)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (driver?.verificationStatus == DriverVerificationStatus.ACTION_REQUIRED) Color(0xFFEA580C) else WandePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (driver?.verificationStatus == DriverVerificationStatus.ACTION_REQUIRED) Icons.Default.Warning else Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (driver?.verificationStatus == DriverVerificationStatus.ACTION_REQUIRED) "Action requise sur votre dossier KYC" else "Vérification d'identité (KYC)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (driver?.verificationStatus == DriverVerificationStatus.ACTION_REQUIRED) Color(0xFF9A3412) else WandePrimaryDark
                            )
                            Text(
                                text = if (driver?.verificationStatus == DriverVerificationStatus.ACTION_REQUIRED) (driver?.rejectionReason ?: "Veuillez reprendre une photo nette de votre pièce.") else "Complétez votre pièce d'identité et selfie pour débloquer toutes les courses.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (driver?.verificationStatus == DriverVerificationStatus.ACTION_REQUIRED) Color(0xFFC2410C) else WandeBlack.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Ouvrir",
                            tint = if (driver?.verificationStatus == DriverVerificationStatus.ACTION_REQUIRED) Color(0xFFEA580C) else WandePrimary
                        )
                    }
                }
            }
        }

        // Metrics Grid (Solde, Courses, Gain)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Solde portefeuille",
                    value = "${driver?.balanceXof ?: 0} F",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = WandePrimary,
                    subtitle = "Disponible pour retrait",
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToEarnings() }
                )
                MetricStatCard(
                    title = "Courses totales",
                    value = "${driver?.totalDeliveries ?: 0}",
                    icon = Icons.Default.CheckCircle,
                    accentColor = WandeAccentDark,
                    subtitle = "Taux succès 98%",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Active Delivery Course Card (if any in progress)
        activeDelivery?.let { delivery ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToActiveDelivery(delivery.id) }
                        .testTag("driver_active_course_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(WandePrimary)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COURSE EN COURS • #${delivery.trackingNumber}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = WandePrimary
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = WandeAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "+${delivery.driverEarningsXof} FCFA",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WandeAccentDark
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Destination : ${delivery.destinationAddress}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Destinataire : ${delivery.recipientName} (${delivery.recipientPhone})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { onNavigateToActiveDelivery(delivery.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WandePrimary)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ouvrir la navigation et valider")
                        }
                    }
                }
            }
        }

        // Incoming / Available Deliveries Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Demandes à proximité (${openDeliveries.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isOnline) {
                    Text(
                        text = "Radar actif 🛰️",
                        style = MaterialTheme.typography.labelSmall,
                        color = WandePrimary
                    )
                }
            }
        }

        if (!isOnline) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Vous êtes actuellement hors ligne",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Activez le bouton 'EN LIGNE' ci-dessus pour recevoir des courses instantanément.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (openDeliveries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = WandeAccent,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "En attente de nouvelles courses...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Dès qu'un client passe commande dans votre rayon, vous recevrez une alerte prioritaire.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(openDeliveries) { delivery ->
                DriverIncomingRequestCard(
                    delivery = delivery,
                    currentDriverId = viewModel.currentDriverId,
                    onAccept = { viewModel.acceptDeliveryRequest(delivery.id) },
                    onCounterOffer = { counterPrice ->
                        viewModel.submitDriverCounterOffer(delivery.id, counterPrice)
                    }
                )
            }
        }

        // Quick Tools Bottom Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToEarnings() }
                        .testTag("driver_wallet_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(WandePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = WandePrimary)
                        }
                        Column {
                            Text("Portefeuille", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Virements", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToProfile() }
                        .testTag("driver_profile_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(WandeAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = WandeAccentDark)
                        }
                        Column {
                            Text("Profil Livreur", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Documents", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DriverIncomingRequestCard(
    delivery: DeliveryEntity,
    currentDriverId: String,
    onAccept: () -> Unit,
    onCounterOffer: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCounterOfferDialog by remember { mutableStateOf(false) }
    var counterOfferInput by remember {
        mutableStateOf(
            ((delivery.customerInitialOffer + 500).coerceAtLeast(1000)).toString()
        )
    }
    var counterOfferError by remember { mutableStateOf<String?>(null) }

    val isThisDriverCounterOffering = delivery.counterOfferDriverId == currentDriverId
    val hasCounterOfferPending = delivery.status == DeliveryStatus.DRIVER_COUNTER_OFFERED && isThisDriverCounterOffering
    val isCounterOfferRejected = delivery.status == DeliveryStatus.COUNTER_OFFER_REJECTED && isThisDriverCounterOffering

    val clientOffer = delivery.customerInitialOffer
    val driverNetAtClientOffer = (clientOffer * 0.90).toInt()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("incoming_request_${delivery.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WandeAccent.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = WandeAccentDark, modifier = Modifier.size(16.dp))
                        Text(
                            text = "NOUVELLE DEMANDE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = WandeAccentDark)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Offre client : $clientOffer FCFA",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = WandePrimary
                        )
                    )
                    Text(
                        text = "Gain net (90%) : $driverNetAtClientOffer F",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pickup and dropoff points
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WandePrimary))
                    Box(modifier = Modifier.width(1.5.dp).height(24.dp).background(Color(0xFFCBD5E1)))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusError))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column {
                        Text("Départ :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(delivery.pickupAddress, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                    Column {
                        Text("Arrivée :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(delivery.destinationAddress, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Distance : ~${delivery.distanceKm} km • ${delivery.packageSize.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Colis : ${delivery.packageDescription}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (hasCounterOfferPending) {
                // Pending Counter-Offer state
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFFD97706),
                            strokeWidth = 2.dp
                        )
                        Column {
                            Text(
                                text = "Contre-offre de ${delivery.driverCounterOffer} FCFA envoyée",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "En attente de réponse du client...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            } else if (isCounterOfferRejected) {
                // Counter-Offer Rejected state
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Contre-offre déclinée par le client",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = "Vous pouvez toujours accepter l'offre initiale de $clientOffer FCFA.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("accept_initial_offer_button_${delivery.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WandePrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Accepter au prix client ($clientOffer FCFA)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                // Two standard actions: Accept OR Propose Counter-Offer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCounterOfferDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("counter_offer_button_${delivery.id}"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, WandePrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WandePrimary)
                    ) {
                        Text(
                            text = "Proposer un prix",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("accept_course_button_${delivery.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WandePrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Accepter ($clientOffer F)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    // Counter Offer Modal Dialog
    if (showCounterOfferDialog) {
        AlertDialog(
            onDismissRequest = { showCounterOfferDialog = false },
            title = {
                Text(
                    text = "Proposer votre prix",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Offre initiale du client : $clientOffer FCFA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Saisissez votre contre-offre (minimum 1 000 FCFA). Vous ne pourrez faire qu'une seule proposition.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = counterOfferInput,
                        onValueChange = {
                            val digits = it.filter { ch -> ch.isDigit() }
                            counterOfferInput = digits
                            val amount = digits.toIntOrNull() ?: 0
                            counterOfferError = if (digits.isNotEmpty() && amount < 1000) {
                                "Le montant minimum est de 1 000 FCFA"
                            } else null
                        },
                        label = { Text("Votre proposition (FCFA)") },
                        trailingIcon = {
                            Text(
                                text = "FCFA",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        isError = counterOfferError != null,
                        supportingText = {
                            counterOfferError?.let {
                                Text(text = it, color = StatusError, style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("driver_counter_offer_input")
                    )

                    val proposedAmount = counterOfferInput.toIntOrNull() ?: 0
                    if (proposedAmount >= 1000) {
                        val netEarnings = (proposedAmount * 0.90).toInt()
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Votre gain net (90%) : $netEarnings FCFA\nCommission WÀNDÉ (10%) : ${(proposedAmount * 0.10).toInt()} FCFA",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF15803D)),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = counterOfferInput.toIntOrNull() ?: 0
                        if (amount >= 1000) {
                            onCounterOffer(amount)
                            showCounterOfferDialog = false
                        } else {
                            counterOfferError = "Le montant minimum est de 1 000 FCFA"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WandePrimary),
                    modifier = Modifier.testTag("submit_counter_offer_button")
                ) {
                    Text("Envoyer l'offre")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCounterOfferDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
