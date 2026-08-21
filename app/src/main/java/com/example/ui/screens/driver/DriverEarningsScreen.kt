package com.example.ui.screens.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.model.PaymentProvider
import com.example.model.TransactionType
import com.example.ui.components.PayoutStatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.WandeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverEarningsScreen(
    viewModel: WandeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val driver by viewModel.currentDriver.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val payouts by viewModel.allPayouts.collectAsState()

    var showPayoutDialog by remember { mutableStateOf(false) }
    var payoutAmountStr by remember { mutableStateOf("") }
    var payoutPhone by remember { mutableStateOf(driver?.mobileMoneyNumber ?: "+226 76 98 76 54") }
    var payoutProvider by remember { mutableStateOf(PaymentProvider.ORANGE_MONEY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Portefeuille & Revenus",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("earnings_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = WandePrimaryDark)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Solde disponible",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${driver?.balanceXof ?: 0} FCFA",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showPayoutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("request_payout_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WandeAccent,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Demander un virement Mobile Money",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Payout Requests Section
            item {
                Text(
                    text = "Demandes de retraits (${payouts.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (payouts.isEmpty()) {
                item {
                    Text(
                        text = "Aucun retrait effectué pour le moment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(payouts) { payout ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                    text = "Virement vers ${payout.phone}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Réf: #${payout.transactionRef} • ${payout.provider.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "-${payout.amountXof} FCFA",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = StatusError
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                PayoutStatusBadge(status = payout.status)
                            }
                        }
                    }
                }
            }

            // Ledger Transactions History
            item {
                Text(
                    text = "Historique des transactions (Grand livre)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = "Aucune transaction enregistrée.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(transactions) { tx ->
                    val isCredit = tx.amountXof > 0
                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(Date(tx.createdAt))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isCredit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isCredit) Color(0xFF15803D) else Color(0xFFB91C1C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${if (isCredit) "+" else ""}${tx.amountXof} FCFA",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCredit) Color(0xFF15803D) else Color(0xFFB91C1C)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Payout Request Dialog
    if (showPayoutDialog) {
        AlertDialog(
            onDismissRequest = { showPayoutDialog = false },
            title = { Text("Demander un virement Mobile Money") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Solde disponible : ${driver?.balanceXof ?: 0} FCFA (Min : 1 000 FCFA)")
                    OutlinedTextField(
                        value = payoutAmountStr,
                        onValueChange = { payoutAmountStr = it },
                        label = { Text("Montant à retirer (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = payoutPhone,
                        onValueChange = { payoutPhone = it },
                        label = { Text("Numéro Mobile Money (+226...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Opérateur :", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = payoutProvider == PaymentProvider.ORANGE_MONEY,
                            onClick = { payoutProvider = PaymentProvider.ORANGE_MONEY },
                            label = { Text("Orange") }
                        )
                        FilterChip(
                            selected = payoutProvider == PaymentProvider.MOOV_MONEY,
                            onClick = { payoutProvider = PaymentProvider.MOOV_MONEY },
                            label = { Text("Moov") }
                        )
                        FilterChip(
                            selected = payoutProvider == PaymentProvider.WAVE,
                            onClick = { payoutProvider = PaymentProvider.WAVE },
                            label = { Text("Wave") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payoutAmountStr.toIntOrNull() ?: 0
                        if (amount >= 1000) {
                            viewModel.requestDriverPayout(amount, payoutPhone, payoutProvider)
                            showPayoutDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WandePrimary)
                ) {
                    Text("Confirmer le virement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayoutDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
