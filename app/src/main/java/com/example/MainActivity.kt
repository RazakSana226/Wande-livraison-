package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.WandeRepository
import com.example.model.UserRole
import com.example.ui.components.WandeRoleHeader
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.client.ClientHistoryScreen
import com.example.ui.screens.client.ClientHomeScreen
import com.example.ui.screens.client.ClientTrackingScreen
import com.example.ui.screens.client.CreateDeliveryScreen
import com.example.ui.screens.driver.DriverActiveDeliveryScreen
import com.example.ui.screens.driver.DriverEarningsScreen
import com.example.ui.screens.driver.DriverHomeScreen
import com.example.ui.screens.driver.DriverOnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WandeViewModel
import com.example.ui.viewmodel.WandeViewModelFactory

sealed class Screen(val route: String) {
    object ClientHome : Screen("client_home")
    object CreateDelivery : Screen("create_delivery")
    object ClientTracking : Screen("client_tracking/{deliveryId}") {
        fun createRoute(deliveryId: String) = "client_tracking/$deliveryId"
    }
    object ClientHistory : Screen("client_history")

    object DriverHome : Screen("driver_home")
    object DriverActiveDelivery : Screen("driver_active_delivery/{deliveryId}") {
        fun createRoute(deliveryId: String) = "driver_active_delivery/$deliveryId"
    }
    object DriverEarnings : Screen("driver_earnings")
    object DriverOnboarding : Screen("driver_onboarding")

    object AdminDashboard : Screen("admin_dashboard")
}

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: WandeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this, lifecycleScope)
        val repository = WandeRepository(database.wandeDao(), lifecycleScope)
        val factory = WandeViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[WandeViewModel::class.java]

        setContent {
            MyApplicationTheme {
                WandeApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun WandeApp(viewModel: WandeViewModel) {
    val navController = rememberNavController()
    val currentRole by viewModel.currentRole.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val allDeliveries by viewModel.allDeliveries.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val activeCount = remember(allDeliveries) {
        allDeliveries.count {
            it.status != com.example.model.DeliveryStatus.DELIVERED &&
            it.status != com.example.model.DeliveryStatus.CANCELLED
        }
    }

    // Listen for toast/feedback messages
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            // Show role switcher on top-level home destinations
            val isTopLevel = currentRoute == Screen.ClientHome.route ||
                    currentRoute == Screen.DriverHome.route ||
                    currentRoute == Screen.AdminDashboard.route

            if (isTopLevel) {
                WandeRoleHeader(
                    currentRole = currentRole,
                    onRoleSelected = { role ->
                        viewModel.setRole(role)
                        when (role) {
                            UserRole.CLIENT -> navController.navigate(Screen.ClientHome.route) {
                                popUpTo(Screen.ClientHome.route) { inclusive = true }
                            }
                            UserRole.DRIVER -> navController.navigate(Screen.DriverHome.route) {
                                popUpTo(Screen.DriverHome.route) { inclusive = true }
                            }
                            UserRole.ADMIN -> navController.navigate(Screen.AdminDashboard.route) {
                                popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                            }
                        }
                    },
                    activeOrderCount = activeCount
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.ClientHome.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- CLIENT ROUTES ---
            composable(Screen.ClientHome.route) {
                ClientHomeScreen(
                    viewModel = viewModel,
                    onRequestDelivery = { navController.navigate(Screen.CreateDelivery.route) },
                    onTrackDelivery = { deliveryId ->
                        navController.navigate(Screen.ClientTracking.createRoute(deliveryId))
                    },
                    onViewHistory = { navController.navigate(Screen.ClientHistory.route) }
                )
            }

            composable(Screen.CreateDelivery.route) {
                CreateDeliveryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onDeliveryCreated = { deliveryId ->
                        navController.navigate(Screen.ClientTracking.createRoute(deliveryId)) {
                            popUpTo(Screen.ClientHome.route)
                        }
                    }
                )
            }

            composable(
                route = Screen.ClientTracking.route,
                arguments = listOf(navArgument("deliveryId") { type = NavType.StringType })
            ) { backStack ->
                val deliveryId = backStack.arguments?.getString("deliveryId") ?: ""
                ClientTrackingScreen(
                    deliveryId = deliveryId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ClientHistory.route) {
                ClientHistoryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onTrackDelivery = { deliveryId ->
                        navController.navigate(Screen.ClientTracking.createRoute(deliveryId))
                    }
                )
            }

            // --- DRIVER ROUTES ---
            composable(Screen.DriverHome.route) {
                DriverHomeScreen(
                    viewModel = viewModel,
                    onNavigateToActiveDelivery = { deliveryId ->
                        navController.navigate(Screen.DriverActiveDelivery.createRoute(deliveryId))
                    },
                    onNavigateToEarnings = { navController.navigate(Screen.DriverEarnings.route) },
                    onNavigateToProfile = { navController.navigate(Screen.DriverOnboarding.route) }
                )
            }

            composable(
                route = Screen.DriverActiveDelivery.route,
                arguments = listOf(navArgument("deliveryId") { type = NavType.StringType })
            ) { backStack ->
                val deliveryId = backStack.arguments?.getString("deliveryId") ?: ""
                DriverActiveDeliveryScreen(
                    deliveryId = deliveryId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DriverEarnings.route) {
                DriverEarningsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DriverOnboarding.route) {
                DriverOnboardingScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // --- ADMIN ROUTES ---
            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(
                    viewModel = viewModel,
                    onTrackDelivery = { deliveryId ->
                        navController.navigate(Screen.ClientTracking.createRoute(deliveryId))
                    }
                )
            }
        }
    }
}
