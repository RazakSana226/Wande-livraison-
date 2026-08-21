package com.example.ui.screens.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DeliveryStatus
import com.example.ui.theme.WandePrimary
import com.example.ui.viewmodel.WandeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientHistoryScreen(
    viewModel: WandeViewModel,
    onBack: () -> Unit,
    onTrackDelivery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val deliveries by viewModel.clientDeliveries.collectAsState()
    var selectedFilter by remember { mutableStateOf("TOUTES") }

    val filteredDeliveries = remember(deliveries, selectedFilter) {
        when (selectedFilter) {
            "EN_COURS" -> deliveries.filter { it.status != DeliveryStatus.DELIVERED && it.status != DeliveryStatus.CANCELLED }
            "LIVREES" -> deliveries.filter { it.status == DeliveryStatus.DELIVERED }
            "ANNULEES" -> deliveries.filter { it.status == DeliveryStatus.CANCELLED }
            else -> deliveries
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Historique des courses",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("history_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    Pair("TOUTES", "Toutes (${deliveries.size})"),
                    Pair("EN_COURS", "En cours"),
                    Pair("LIVREES", "Livrées"),
                    Pair("ANNULEES", "Annulées")
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WandePrimary.copy(alpha = 0.15f),
                            selectedLabelColor = WandePrimary
                        )
                    )
                }
            }

            if (filteredDeliveries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Aucune course dans cette catégorie",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDeliveries) { delivery ->
                        ClientDeliveryCard(
                            delivery = delivery,
                            onClick = { onTrackDelivery(delivery.id) }
                        )
                    }
                }
            }
        }
    }
}
