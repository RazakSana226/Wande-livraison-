package com.example.ui.screens.driver

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DriverVerificationStatus
import com.example.model.VehicleType
import com.example.ui.components.DriverVerificationBadge
import com.example.ui.theme.WandePrimary
import com.example.ui.viewmodel.WandeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverOnboardingScreen(
    viewModel: WandeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val driver by viewModel.currentDriver.collectAsState()

    var name by remember { mutableStateOf(driver?.name ?: "Ibrahim Traoré") }
    var phone by remember { mutableStateOf(driver?.phone ?: "+226 76 98 76 54") }
    var vehicleType by remember { mutableStateOf(driver?.vehicleType ?: VehicleType.MOTO) }
    var vehicleModel by remember { mutableStateOf(driver?.vehicleModel ?: "Yamaha 125cc") }
    var vehicleNumber by remember { mutableStateOf(driver?.vehicleNumber ?: "11-AB-2044-BF") }
    var habitualZone by remember { mutableStateOf(driver?.habitualZone ?: "Ouaga Centre / Ouaga 2000") }
    var mobileMoneyNumber by remember { mutableStateOf(driver?.mobileMoneyNumber ?: "+226 76 98 76 54") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profil Livreur & Documents",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("onboarding_back_button")) {
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
            // Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Statut du dossier KYC / Livreur :",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        driver?.verificationStatus?.let { status ->
                            DriverVerificationBadge(status = status)
                        }
                    }
                }
            }

            // Information Form
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
                            text = "Informations personnelles & Véhicule",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nom complet") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Téléphone") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Type de véhicule :", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VehicleType.values().forEach { vType ->
                                FilterChip(
                                    selected = vehicleType == vType,
                                    onClick = { vehicleType = vType },
                                    label = { Text(vType.label) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = vehicleModel,
                            onValueChange = { vehicleModel = it },
                            label = { Text("Modèle et cylindrée") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = vehicleNumber,
                            onValueChange = { vehicleNumber = it },
                            label = { Text("Numéro d'immatriculation / Plaque") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = habitualZone,
                            onValueChange = { habitualZone = it },
                            label = { Text("Zone d'activité habituelle") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = mobileMoneyNumber,
                            onValueChange = { mobileMoneyNumber = it },
                            label = { Text("Numéro de paiement Mobile Money") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.registerDriverProfile(
                                    name = name,
                                    phone = phone,
                                    vehicleType = vehicleType,
                                    vehicleModel = vehicleModel,
                                    vehicleNumber = vehicleNumber,
                                    habitualZone = habitualZone,
                                    mobileMoneyNumber = mobileMoneyNumber,
                                    onSuccess = onBack
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_driver_profile_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WandePrimary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enregistrer mon profil", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}
