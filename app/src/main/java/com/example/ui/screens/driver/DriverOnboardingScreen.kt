package com.example.ui.screens.driver

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.DriverVerificationStatus
import com.example.model.IdentityDocumentType
import com.example.model.VehicleType
import com.example.service.identity.KycDocumentBundle
import com.example.service.identity.LivenessCaptureMetadata
import com.example.ui.components.DriverTrustBadge
import com.example.ui.components.DriverVerificationBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.WandeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverOnboardingScreen(
    viewModel: WandeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val driver by viewModel.currentDriver.collectAsState()

    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1: Personal info
    var name by remember { mutableStateOf(driver?.name ?: "Ibrahim Traoré") }
    var birthDate by remember { mutableStateOf(driver?.birthDate ?: "12/05/1996") }
    var phone by remember { mutableStateOf(driver?.phone ?: "+226 76 98 76 54") }
    var city by remember { mutableStateOf(driver?.city ?: "Ouagadougou") }
    var habitualZone by remember { mutableStateOf(driver?.habitualZone ?: "Ouaga Centre / Ouaga 2000") }
    var vehicleType by remember { mutableStateOf(driver?.vehicleType ?: VehicleType.MOTO) }
    var vehicleModel by remember { mutableStateOf(driver?.vehicleModel ?: "Yamaha 125cc") }
    var vehicleNumber by remember { mutableStateOf(driver?.vehicleNumber ?: "11-AB-2044-BF") }
    var mobileMoneyNumber by remember { mutableStateOf(driver?.mobileMoneyNumber ?: "+226 76 98 76 54") }

    // Step 2: Document info
    var docType by remember { mutableStateOf(driver?.idDocumentType ?: IdentityDocumentType.CNI) }
    var frontDocUploaded by remember { mutableStateOf(true) }
    var backDocUploaded by remember { mutableStateOf(true) }

    // Step 3: Selfie / Liveness info
    var selfieCaptured by remember { mutableStateOf(true) }
    var faceCentered by remember { mutableStateOf(true) }
    var glassesRemoved by remember { mutableStateOf(true) }
    var goodLighting by remember { mutableStateOf(true) }

    // Step 4: Submission State
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Vérification d'Identité (KYC)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = WandeBlack
                        )
                        Text(
                            text = "Étape $currentStep sur 4",
                            style = MaterialTheme.typography.labelSmall,
                            color = WandePrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("onboarding_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = WandeBgIce
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, WandeBorder)
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
                                text = "Statut de votre compte",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = WandeBlack
                            )
                            driver?.verificationStatus?.let { status ->
                                DriverVerificationBadge(status = status)
                            }
                        }

                        if (driver?.verificationStatus == DriverVerificationStatus.ACTION_REQUIRED) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFF7ED),
                                border = BorderStroke(1.dp, Color(0xFFFDBA74)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC2410C))
                                    Text(
                                        text = driver?.rejectionReason ?: "Photo de pièce floue. Veuillez soumettre une photo nette.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9A3412)
                                    )
                                }
                            }
                        }

                        // Progress Step Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (step in 1..4) {
                                val isDone = step <= currentStep
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isDone) WandePrimary else WandeBorder)
                                )
                            }
                        }
                    }
                }
            }

            // Step Content
            when (currentStep) {
                1 -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, WandeBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "1. Informations Personnelles & Véhicule",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = WandePrimary
                                )
                                Text(
                                    text = "Renseignez vos coordonnées exactes telles qu'elles figurent sur votre pièce d'identité.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Nom complet") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = birthDate,
                                    onValueChange = { birthDate = it },
                                    label = { Text("Date de naissance (JJ/MM/AAAA)") },
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("Numéro de téléphone") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = { Text("Ville de résidence") },
                                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = habitualZone,
                                    onValueChange = { habitualZone = it },
                                    label = { Text("Zone d'activité habituelle (Ex: Ouaga 2000, Gounghin)") },
                                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Text(
                                    text = "Type de véhicule :",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VehicleType.values().forEach { vType ->
                                        FilterChip(
                                            selected = vehicleType == vType,
                                            onClick = { vehicleType = vType },
                                            label = { Text(vType.label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = WandePrimary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = vehicleModel,
                                    onValueChange = { vehicleModel = it },
                                    label = { Text("Modèle et marque du véhicule") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = vehicleNumber,
                                    onValueChange = { vehicleNumber = it },
                                    label = { Text("Immatriculation / Plaque") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = mobileMoneyNumber,
                                    onValueChange = { mobileMoneyNumber = it },
                                    label = { Text("Numéro Mobile Money pour vos gains") },
                                    leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { currentStep = 2 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("kyc_step1_next_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WandePrimary)
                                ) {
                                    Text("Continuer vers les documents ➔", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, WandeBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "2. Document Officiel d'Identité",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = WandePrimary
                                )
                                Text(
                                    text = "Sélectionnez le type de pièce et prenez en photo l'avant et l'arrière.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IdentityDocumentType.values().forEach { type ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { docType = type }
                                                .border(
                                                    width = if (docType == type) 2.dp else 1.dp,
                                                    color = if (docType == type) WandePrimary else WandeBorder,
                                                    shape = RoundedCornerShape(10.dp)
                                                ),
                                            color = if (docType == type) WandePrimary.copy(alpha = 0.08f) else Color.White
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = type.label,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (docType == type) FontWeight.Bold else FontWeight.Normal
                                                    ),
                                                    color = if (docType == type) WandePrimary else WandeBlack
                                                )
                                                RadioButton(
                                                    selected = docType == type,
                                                    onClick = { docType = type },
                                                    colors = RadioButtonDefaults.colors(selectedColor = WandePrimary)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Document Front Card
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, if (frontDocUploaded) Color(0xFF86EFAC) else WandeBorder, RoundedCornerShape(12.dp)),
                                    color = if (frontDocUploaded) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (frontDocUploaded) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                                                contentDescription = null,
                                                tint = if (frontDocUploaded) Color(0xFF15803D) else WandePrimary
                                            )
                                            Column {
                                                Text(
                                                    text = "Face Avant (${docType.name})",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = WandeBlack
                                                )
                                                Text(
                                                    text = if (frontDocUploaded) "Document capturé avec succès" else "Prendre une photo nette",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { frontDocUploaded = true },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(if (frontDocUploaded) "Reprendre" else "Photographier")
                                        }
                                    }
                                }

                                // Document Back Card
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, if (backDocUploaded) Color(0xFF86EFAC) else WandeBorder, RoundedCornerShape(12.dp)),
                                    color = if (backDocUploaded) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (backDocUploaded) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                                                contentDescription = null,
                                                tint = if (backDocUploaded) Color(0xFF15803D) else WandePrimary
                                            )
                                            Column {
                                                Text(
                                                    text = "Face Arrière (${docType.name})",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = WandeBlack
                                                )
                                                Text(
                                                    text = if (backDocUploaded) "Document capturé avec succès" else "Prendre une photo nette",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { backDocUploaded = true },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(if (backDocUploaded) "Reprendre" else "Photographier")
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = 1 },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Précédent")
                                    }
                                    Button(
                                        onClick = { currentStep = 3 },
                                        modifier = Modifier
                                            .weight(2f)
                                            .height(50.dp)
                                            .testTag("kyc_step2_next_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = WandePrimary)
                                    ) {
                                        Text("Passer au Selfie ➔", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, WandeBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "3. Selfie de Vérification Biométrique",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = WandePrimary
                                )
                                Text(
                                    text = "Cette étape permet de vérifier que vous êtes bien le titulaire de la pièce.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Oval Framing Guide
                                Box(
                                    modifier = Modifier
                                        .size(190.dp, 240.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .border(3.dp, WandePrimary, RoundedCornerShape(100.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Visage",
                                            tint = WandePrimary,
                                            modifier = Modifier.size(72.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Cadrez votre visage",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = WandePrimary
                                        )
                                    }
                                }

                                // Checklist of instructions
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, WandeBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Consignes pour validation rapide :", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        InstructionRow(text = "Placez votre visage dans le repère ovale", checked = faceCentered)
                                        InstructionRow(text = "Retirez chapeaux, casquettes et lunettes teintées", checked = glassesRemoved)
                                        InstructionRow(text = "Placez-vous dans un endroit bien éclairé", checked = goodLighting)
                                        InstructionRow(text = "Regardez directement l'objectif de la caméra", checked = true)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = 2 },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Précédent")
                                    }
                                    Button(
                                        onClick = { currentStep = 4 },
                                        modifier = Modifier
                                            .weight(2f)
                                            .height(50.dp)
                                            .testTag("kyc_step3_next_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = WandePrimary)
                                    ) {
                                        Text("Vérifier et Soumettre ➔", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, WandeBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "4. Récapitulatif du Dossier KYC",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = WandePrimary
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, WandeBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SummaryRow(label = "Nom complet", value = name)
                                        SummaryRow(label = "Téléphone", value = phone)
                                        SummaryRow(label = "Date de naissance", value = birthDate)
                                        SummaryRow(label = "Ville & Zone", value = "$city ($habitualZone)")
                                        SummaryRow(label = "Véhicule", value = "${vehicleType.label} - $vehicleModel ($vehicleNumber)")
                                        SummaryRow(label = "Document", value = docType.label)
                                        SummaryRow(label = "Photos justificatives", value = "Face avant ✓ | Face arrière ✓ | Selfie ✓")
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = WandePrimary.copy(alpha = 0.08f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = WandePrimary)
                                        Text(
                                            text = "Vos pièces sont stockées de façon sécurisée et cryptée conformément aux directives de protection des données personnelles.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = WandePrimaryDark
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        isSubmitting = true
                                        val bundle = KycDocumentBundle(
                                            driverId = driver?.id ?: "drv_1",
                                            fullName = name,
                                            birthDate = birthDate,
                                            phone = phone,
                                            city = city,
                                            habitualZone = habitualZone,
                                            documentType = docType,
                                            documentFrontUri = "id_card_front.jpg",
                                            documentBackUri = "id_card_back.jpg",
                                            selfieUri = "driver_selfie.jpg",
                                            livenessMetadata = LivenessCaptureMetadata(
                                                faceInFrame = true,
                                                ambientLightLux = 420f,
                                                captureInstructionsAccepted = true
                                            )
                                        )
                                        viewModel.submitDriverKyc(
                                            bundle = bundle,
                                            onSuccess = {
                                                isSubmitting = false
                                                onBack()
                                            },
                                            onError = {
                                                isSubmitting = false
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("submit_kyc_bundle_button"),
                                    enabled = !isSubmitting,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WandePrimary)
                                ) {
                                    if (isSubmitting) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Envoi du dossier...")
                                    } else {
                                        Icon(Icons.Default.Send, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Envoyer mon dossier KYC", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
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

@Composable
private fun InstructionRow(text: String, checked: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (checked) Color(0xFF15803D) else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = WandeBlack)
    }
}
