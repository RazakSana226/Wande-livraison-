package com.example.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LatLngPoint
import com.example.model.PackageSize
import com.example.model.PaymentMethod
import com.example.model.PaymentSimulationMode
import com.example.service.GeminiMapsService
import com.example.ui.components.WandeInteractiveMap
import com.example.ui.theme.*
import com.example.ui.viewmodel.WandeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDeliveryScreen(
    viewModel: WandeViewModel,
    onBack: () -> Unit,
    onDeliveryCreated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.createDeliveryState.collectAsState()
    val settings by viewModel.platformSettings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectingTarget by remember { mutableStateOf<String?>("pickup") } // "pickup" or "destination"
    var recipientName by remember { mutableStateOf(state.recipientName.ifEmpty { "Fatimata Kaboré" }) }
    var recipientPhone by remember { mutableStateOf(state.recipientPhone.ifEmpty { "+226 78 55 44 33" }) }
    var packageDesc by remember { mutableStateOf(state.packageDescription.ifEmpty { "Carton moyen avec vêtements et documents" }) }
    var pickupNotes by remember { mutableStateOf(state.pickupInstructions.ifEmpty { "Portail vert, sonner à l'interphone" }) }
    var specialInstructions by remember { mutableStateOf(state.specialNotes.ifEmpty { "Maison avec portail bleu après la pharmacie" }) }

    LaunchedEffect(Unit) {
        viewModel.setRecipientInfo(recipientName, recipientPhone)
        viewModel.setPackageDetails(packageDesc, specialInstructions, pickupNotes)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nouvelle livraison",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total à payer (XOF)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${state.pricing?.totalPriceXof ?: 1000} FCFA",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = WandePrimary
                                )
                            )
                        }
                        Text(
                            text = "~ ${state.pricing?.distanceKm ?: 3.5} km • ${state.pricing?.estimatedMinutes ?: 15} min",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.setRecipientInfo(recipientName, recipientPhone)
                            viewModel.setPackageDetails(packageDesc, specialInstructions, pickupNotes)
                            viewModel.submitDeliveryRequest { deliveryId ->
                                onDeliveryCreated(deliveryId)
                            }
                        },
                        enabled = !state.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_delivery_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WandePrimary,
                            contentColor = Color.White
                        )
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confirmer et Commander",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Interactive Map & Pin Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Itinéraire & Localisation",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = selectingTarget == "pickup",
                                    onClick = { selectingTarget = "pickup" },
                                    label = { Text("Départ 🟢") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WandePrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = WandePrimary
                                    )
                                )
                                FilterChip(
                                    selected = selectingTarget == "destination",
                                    onClick = { selectingTarget = "destination" },
                                    label = { Text("Arrivée 🔴") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StatusError.copy(alpha = 0.15f),
                                        selectedLabelColor = StatusError
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        WandeInteractiveMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp),
                            pickupPoint = state.pickupPoint,
                            destinationPoint = state.destinationPoint,
                            isSelectable = true,
                            onLocationSelected = { selectedPoint ->
                                if (selectingTarget == "pickup") {
                                    viewModel.setPickupPoint(selectedPoint)
                                } else {
                                    viewModel.setDestinationPoint(selectedPoint)
                                }
                            }
                        )
                    }
                }
            }

            // Places Search with Maps Grounding
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Rechercher une adresse (Google Maps / IA)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.searchPlaces(it)
                            },
                            placeholder = { Text("Ex: Ouaga 2000, Gounghin, pharmacie...") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = WandePrimary)
                            },
                            trailingIcon = {
                                if (state.isSearchingPlaces) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_place_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Search suggestions chips
                        if (state.searchResults.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Résultats suggérés :",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            state.searchResults.forEach { place ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (selectingTarget == "pickup") {
                                                viewModel.setPickupPoint(place)
                                            } else {
                                                viewModel.setDestinationPoint(place)
                                            }
                                            searchQuery = ""
                                            viewModel.searchPlaces("")
                                        },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = if (selectingTarget == "pickup") WandePrimary else StatusError,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = place.address,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (place.landmark.isNotEmpty()) {
                                                Text(
                                                    text = place.landmark,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pickup & Destination Summary Fields
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Pickup
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(WandePrimary))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Point de départ (Récupération)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WandePrimary
                                )
                                Text(
                                    text = state.pickupPoint.address,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        OutlinedTextField(
                            value = pickupNotes,
                            onValueChange = { pickupNotes = it },
                            placeholder = { Text("Précisions pour la récupération (ex: portail vert)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        // Destination
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(StatusError))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Destination (Livraison)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusError
                                )
                                Text(
                                    text = state.destinationPoint.address,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        OutlinedTextField(
                            value = specialInstructions,
                            onValueChange = { specialInstructions = it },
                            placeholder = { Text("Instructions de repérage (ex: maison portail bleu après pharmacie)") },
                            modifier = Modifier.fillMaxWidth().testTag("instructions_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Recipient Details
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Destinataire",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = recipientName,
                            onValueChange = { recipientName = it },
                            label = { Text("Nom complet du destinataire") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("recipient_name_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = recipientPhone,
                            onValueChange = { recipientPhone = it },
                            label = { Text("Numéro de téléphone (+226...)") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("recipient_phone_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Package Sizing
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Taille et description du colis",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = packageDesc,
                            onValueChange = { packageDesc = it },
                            label = { Text("Description du colis") },
                            placeholder = { Text("Ex: Repas, documents, vêtements...") },
                            modifier = Modifier.fillMaxWidth().testTag("package_desc_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Text(
                            text = "Choisir le format :",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        PackageSize.values().forEach { size ->
                            val isSelected = state.packageSize == size
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setPackageSize(size) }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) WandePrimary else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                color = if (isSelected) WandePrimary.copy(alpha = 0.08f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = size.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = size.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = if (size.surchargeXof == 0) "Inclus" else "+${size.surchargeXof} FCFA",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) WandePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Simplified Price Selection & Transparent Fee Breakdown (Minimum 1000 FCFA)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("price_selection_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Proposez votre prix",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = WandePrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Min: 1 000 FCFA",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WandePrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Choisissez l'une des 3 suggestions ou entrez un montant personnalisé :",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 3 Pricing Suggestions
                        val suggestions = listOf(
                            Triple(1000, "Prix minimum", "Minimum"),
                            Triple(1500, "Prix recommandé", "Recommandé"),
                            Triple(2000, "Offre plus attractive", "Express")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestions.forEach { (price, subtitle, tag) ->
                                val isSelected = state.proposedPriceXof == price
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { viewModel.setProposedPrice(price) }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) WandePrimary else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(14.dp)
                                        ),
                                    color = if (isSelected) WandePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) WandePrimary else Color(0xFF64748B).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    color = if (isSelected) Color.White else Color(0xFF475569)
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "$price F",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) WandePrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Custom Price Input
                        OutlinedTextField(
                            value = state.customPriceInput,
                            onValueChange = { viewModel.setCustomPriceInput(it) },
                            label = { Text("Ou entrez un montant libre (FCFA)") },
                            trailingIcon = {
                                Text(
                                    text = "FCFA",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = WandePrimary,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            isError = state.pricingErrorMessage != null,
                            supportingText = {
                                if (state.pricingErrorMessage != null) {
                                    Text(
                                        text = state.pricingErrorMessage ?: "",
                                        color = StatusError,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                } else {
                                    Text(
                                        text = "Montant libre supérieur ou égal à 1 000 FCFA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_price_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Transparent Breakdown Card
                        val proposedPrice = if (state.proposedPriceXof >= 1000) state.proposedPriceXof else 1000
                        val commission = (proposedPrice * 0.10).toInt()
                        val totalCustomer = proposedPrice + commission

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Prix proposé pour la course :",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$proposedPrice FCFA",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Frais de service WÀNDÉ (10%) :",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$commission FCFA",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TOTAL À PAYER :",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$totalCustomer FCFA",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = WandePrimary
                                        )
                                    )
                                }
                            }
                        }

                        // Informational Notice
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = WandePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Les suggestions de prix ne garantissent pas l'acceptation immédiate par un livreur. Plus l'offre est attractive, plus vite un livreur accepte.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF334155),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Payment Provider Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mode de paiement",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = WandeSecondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "TEST / SANDBOX",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WandeSecondary
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Architecture découplée (Prêt pour CinetPay V2 Serveur).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        PaymentMethod.values().forEach { provider ->
                            val isSelected = state.paymentProvider == provider
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.setPaymentProvider(provider) }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) WandePrimary else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                color = if (isSelected) WandePrimary.copy(alpha = 0.08f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = when (provider) {
                                            PaymentMethod.ORANGE_MONEY, PaymentMethod.MOOV_MONEY, PaymentMethod.WAVE -> Icons.Default.PhoneAndroid
                                            PaymentMethod.CINETPAY, PaymentMethod.PAYDUNYA -> Icons.Default.AccountBalance
                                            PaymentMethod.CASH_ON_DELIVERY -> Icons.Default.Payments
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) WandePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = provider.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (provider == PaymentMethod.CINETPAY) {
                                            Text(
                                                text = "Agrégateur multi-opérateurs (Orange, Moov, Wave, Cartes)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Simulation selector for testing various states
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Simulateur de statut (Tests & Évaluation) :",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        PaymentSimulationMode.values().forEach { simMode ->
                            val isModeSelected = state.paymentSimulationMode == simMode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setPaymentSimulationMode(simMode) }
                                    .border(
                                        width = if (isModeSelected) 1.5.dp else 1.dp,
                                        color = if (isModeSelected) when (simMode) {
                                            PaymentSimulationMode.SIMULATE_SUCCESS -> StatusSuccess
                                            PaymentSimulationMode.SIMULATE_PENDING -> StatusWarning
                                            PaymentSimulationMode.SIMULATE_FAILED -> StatusError
                                            PaymentSimulationMode.SIMULATE_EXPIRED -> Color(0xFF6B7280)
                                        } else Color(0xFFE5E7EB),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                color = if (isModeSelected) when (simMode) {
                                    PaymentSimulationMode.SIMULATE_SUCCESS -> StatusSuccess.copy(alpha = 0.08f)
                                    PaymentSimulationMode.SIMULATE_PENDING -> StatusWarning.copy(alpha = 0.08f)
                                    PaymentSimulationMode.SIMULATE_FAILED -> StatusError.copy(alpha = 0.08f)
                                    PaymentSimulationMode.SIMULATE_EXPIRED -> Color(0xFF6B7280).copy(alpha = 0.08f)
                                } else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isModeSelected,
                                        onClick = { viewModel.setPaymentSimulationMode(simMode) }
                                    )
                                    Column {
                                        Text(
                                            text = simMode.label,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = simMode.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
