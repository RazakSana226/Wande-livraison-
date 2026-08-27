package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.DeliveryStatusBadge
import com.example.ui.components.DriverVerificationBadge
import com.example.ui.components.MetricStatCard
import com.example.ui.components.PayoutStatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.WandeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDashboardScreen(
    viewModel: WandeViewModel,
    onTrackDelivery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val deliveries by viewModel.allDeliveries.collectAsState()
    val drivers by viewModel.allDrivers.collectAsState()
    val payouts by viewModel.allPayouts.collectAsState()
    val disputes by viewModel.allDisputes.collectAsState()
    val settings by viewModel.platformSettings.collectAsState()

    var selectedAdminTab by remember { mutableStateOf("OVERVIEW") }

    val totalRevenueXof = remember(deliveries) {
        deliveries.filter { it.status == DeliveryStatus.DELIVERED }.sumOf { it.totalPriceXof }
    }
    val totalCommissionXof = remember(deliveries) {
        deliveries.filter { it.status == DeliveryStatus.DELIVERED }.sumOf { it.platformFeeXof }
    }
    val activeDeliveriesCount = remember(deliveries) {
        deliveries.count { it.status != DeliveryStatus.DELIVERED && it.status != DeliveryStatus.CANCELLED }
    }
    val pendingDriversCount = remember(drivers) {
        drivers.count { it.verificationStatus == DriverVerificationStatus.PENDING_VERIFICATION }
    }
    val pendingPayoutsCount = remember(payouts) {
        payouts.count { it.status == PayoutStatus.PAYOUT_PENDING }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Admin Sub-tabs Navigation
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                Pair("OVERVIEW", "Tableau de bord"),
                Pair("DRIVERS", "Livreurs ($pendingDriversCount en attente)"),
                Pair("DELIVERIES", "Courses actives ($activeDeliveriesCount)"),
                Pair("FINANCES", "Finances & Virements ($pendingPayoutsCount)"),
                Pair("AUDIT", "Audit & Sécurité"),
                Pair("SETTINGS", "Configuration")
            )
            items(tabs) { (key, label) ->
                FilterChip(
                    selected = selectedAdminTab == key,
                    onClick = { selectedAdminTab = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WandePrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("admin_tab_${key.lowercase()}")
                )
            }
        }

        when (selectedAdminTab) {
            "OVERVIEW" -> AdminOverviewContent(
                totalRevenue = totalRevenueXof,
                totalCommission = totalCommissionXof,
                activeDeliveries = activeDeliveriesCount,
                pendingDrivers = pendingDriversCount,
                pendingPayouts = pendingPayoutsCount,
                disputesCount = disputes.size,
                recentDeliveries = deliveries.take(5),
                onTrackDelivery = onTrackDelivery
            )
            "DRIVERS" -> AdminDriversContent(
                drivers = drivers,
                onApprove = { viewModel.adminReviewKycDecision(it, com.example.service.identity.KycAdminDecision.APPROVE) },
                onReject = { viewModel.adminReviewKycDecision(it, com.example.service.identity.KycAdminDecision.REJECT, "Dossier non conforme") },
                onRequestNewPhoto = { viewModel.adminReviewKycDecision(it, com.example.service.identity.KycAdminDecision.REQUEST_NEW_PHOTO, "Photo floue / CNI illisible. Veuillez reprendre.") },
                onSuspend = { viewModel.adminSetDriverStatus(it, DriverVerificationStatus.SUSPENDED) }
            )
            "DELIVERIES" -> AdminDeliveriesContent(
                deliveries = deliveries,
                onTrackDelivery = onTrackDelivery
            )
            "FINANCES" -> AdminFinancesContent(
                payouts = payouts,
                onApprovePayout = { viewModel.adminProcessPayout(it, true) },
                onRejectPayout = { viewModel.adminProcessPayout(it, false) }
            )
            "AUDIT" -> AdminAuditTabContent(viewModel = viewModel)
            "SETTINGS" -> AdminSettingsContent(
                currentSettings = settings ?: PlatformSettingsEntity(),
                onSave = { viewModel.adminSaveSettings(it) }
            )
        }
    }
}

@Composable
private fun AdminAuditTabContent(viewModel: WandeViewModel) {
    val auditLogs by viewModel.allAuditLogs.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Traçabilité des actions & Sécurité (OTP, Connexions, Virements)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(auditLogs, key = { it.id }) { log ->
            AuditLogCard(log = log)
        }
    }
}

@Composable
private fun AdminOverviewContent(
    totalRevenue: Int,
    totalCommission: Int,
    activeDeliveries: Int,
    pendingDrivers: Int,
    pendingPayouts: Int,
    disputesCount: Int,
    recentDeliveries: List<DeliveryEntity>,
    onTrackDelivery: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Indicateurs Clés (KPI)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Financial KPIs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Commission WÀNDÉ (10%)",
                    value = "$totalCommission FCFA",
                    icon = Icons.Default.MonetizationOn,
                    accentColor = WandePrimary,
                    subtitle = "Revenus nets plateforme",
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Volume Total Livraisons",
                    value = "$totalRevenue FCFA",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    accentColor = WandeAccentDark,
                    subtitle = "Chiffre d'affaires brut",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Operations KPIs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Courses en cours",
                    value = "$activeDeliveries",
                    icon = Icons.Default.TwoWheeler,
                    accentColor = Color(0xFF0E7490),
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Dossiers KYC à valider",
                    value = "$pendingDrivers",
                    icon = Icons.Default.HourglassTop,
                    accentColor = if (pendingDrivers > 0) WandeAccentDark else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Virements en attente",
                    value = "$pendingPayouts",
                    icon = Icons.Default.AccountBalance,
                    accentColor = if (pendingPayouts > 0) StatusError else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Litiges signalés",
                    value = "$disputesCount",
                    icon = Icons.Default.ReportProblem,
                    accentColor = if (disputesCount > 0) StatusError else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Live Feed
        item {
            Text(
                text = "Dernières activités de livraison",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(recentDeliveries) { delivery ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onTrackDelivery(delivery.id) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "#${delivery.trackingNumber} • ${delivery.clientName}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${delivery.pickupAddress} ➔ ${delivery.destinationAddress}",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${delivery.totalPriceXof} FCFA",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        DeliveryStatusBadge(status = delivery.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminDriversContent(
    drivers: List<DriverEntity>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onRequestNewPhoto: (String) -> Unit,
    onSuspend: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(drivers) { driver ->
            var isExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(WandePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                            }
                            Column {
                                Text(
                                    text = driver.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${driver.phone} • ${driver.vehicleType.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        DriverVerificationBadge(status = driver.verificationStatus)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Véhicule : ${driver.vehicleModel} (${driver.vehicleNumber}) • Zone : ${driver.habitualZone}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Pièce : ${driver.idDocumentType.label} • Ville : ${driver.city} (Né le ${driver.birthDate})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Solde : ${driver.balanceXof} FCFA • Courses : ${driver.totalDeliveries} • Note : ★ ${driver.rating} • Score Liveness : ${(driver.livenessScore * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = WandePrimaryDark
                    )

                    // KYC Document Inspection Section
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isExpanded = !isExpanded },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) "Masquer les pièces jointes KYC ▲" else "Examiner les pièces jointes KYC (3 fichiers) ▼",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = WandePrimary
                            )
                        }
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = WandePrimary, modifier = Modifier.size(24.dp))
                                    Text("CNI Face Avant", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = WandePrimary, modifier = Modifier.size(24.dp))
                                    Text("CNI Face Arrière", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Face, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(24.dp))
                                    Text("Selfie Caméra", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (driver.verificationStatus != DriverVerificationStatus.VERIFIED) {
                            Button(
                                onClick = { onApprove(driver.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = WandePrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approuver")
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (driver.verificationStatus == DriverVerificationStatus.PENDING_VERIFICATION || driver.verificationStatus == DriverVerificationStatus.ACTION_REQUIRED) {
                            OutlinedButton(
                                onClick = { onRequestNewPhoto(driver.id) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Photo à refaire")
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (driver.verificationStatus != DriverVerificationStatus.REJECTED) {
                            OutlinedButton(
                                onClick = { onReject(driver.id) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Rejeter", color = StatusError)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (driver.verificationStatus == DriverVerificationStatus.VERIFIED) {
                            TextButton(onClick = { onSuspend(driver.id) }) {
                                Text("Suspendre", color = StatusError)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminDeliveriesContent(
    deliveries: List<DeliveryEntity>,
    onTrackDelivery: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(deliveries) { delivery ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onTrackDelivery(delivery.id) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${delivery.trackingNumber} • Code OTP: ${delivery.otpCode}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = WandePrimary
                        )
                        DeliveryStatusBadge(status = delivery.status)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Client: ${delivery.clientName} ➔ Dest: ${delivery.recipientName}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Livreur: ${delivery.driverName ?: "Non assigné"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Prix: ${delivery.totalPriceXof} FCFA (Com: ${delivery.platformFeeXof} F)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Voir le direct ➔",
                            style = MaterialTheme.typography.labelSmall,
                            color = WandePrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminFinancesContent(
    payouts: List<PayoutEntity>,
    onApprovePayout: (String) -> Unit,
    onRejectPayout: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Demandes de retraits livreurs (${payouts.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (payouts.isEmpty()) {
            item {
                Text("Aucune demande de retrait.", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(payouts) { payout ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = payout.driverName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Mobile Money: ${payout.phone} (${payout.provider.label})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${payout.amountXof} FCFA",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WandePrimary
                                    )
                                )
                                PayoutStatusBadge(status = payout.status)
                            }
                        }

                        if (payout.status == PayoutStatus.PAYOUT_PENDING) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onApprovePayout(payout.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = WandePrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Valider le paiement")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { onRejectPayout(payout.id) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Rejeter", color = StatusError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSettingsContent(
    currentSettings: PlatformSettingsEntity,
    onSave: (PlatformSettingsEntity) -> Unit
) {
    var basePriceStr by remember { mutableStateOf(currentSettings.basePriceXof.toString()) }
    var pricePerKmStr by remember { mutableStateOf(currentSettings.pricePerKmXof.toString()) }
    var minPriceStr by remember { mutableStateOf(currentSettings.minimumPriceXof.toString()) }
    var commissionStr by remember { mutableStateOf(currentSettings.commissionPercent.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Grille Tarifaire & Commission",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = basePriceStr,
                        onValueChange = { basePriceStr = it },
                        label = { Text("Frais de prise en charge / Base (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pricePerKmStr,
                        onValueChange = { pricePerKmStr = it },
                        label = { Text("Tarif par kilomètre (FCFA/km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = minPriceStr,
                        onValueChange = { minPriceStr = it },
                        label = { Text("Tarif minimum par course (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = commissionStr,
                        onValueChange = { commissionStr = it },
                        label = { Text("Pourcentage Commission WÀNDÉ (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val updated = currentSettings.copy(
                                basePriceXof = basePriceStr.toIntOrNull() ?: 500,
                                pricePerKmXof = pricePerKmStr.toIntOrNull() ?: 250,
                                minimumPriceXof = minPriceStr.toIntOrNull() ?: 1000,
                                commissionPercent = commissionStr.toIntOrNull() ?: 10,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(updated)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WandePrimary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enregistrer les tarifs", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Passerelle de Paiement & Virements",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "MOCK / TEST MODE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = StatusSuccess),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Statut : Architecture PaymentProvider découplée et prête pour CinetPay V2.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🔒 Sécurité des identifiants :", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Text("• Aucun secret de paiement (API Key, Site ID) n'est exposé côté frontend.", style = MaterialTheme.typography.bodySmall)
                            Text("• L'intégration de production CinetPay s'exécute exclusivement côté serveur (Proxy & IPN Webhook).", style = MaterialTheme.typography.bodySmall)
                            Text("• Les fonds utilisateurs ne sont pas conservés dans Firestore mais gérés par la passerelle de paiement.", style = MaterialTheme.typography.bodySmall)
                            Text("• Le système de versement aux livreurs vérifie automatiquement l'éligibilité KYC avant décaissement.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
