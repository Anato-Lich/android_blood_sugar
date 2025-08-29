package com.example.bloodsugar

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bloodsugar.notifications.PersistentNotificationService
import com.example.bloodsugar.ui.screens.CalculatorScreen
import com.example.bloodsugar.ui.screens.FoodScreen
import com.example.bloodsugar.ui.screens.HomeScreen
import com.example.bloodsugar.ui.screens.NotificationsScreen
import com.example.bloodsugar.ui.screens.SettingsScreen
import com.example.bloodsugar.ui.theme.BloodSugarTheme
import com.example.bloodsugar.viewmodel.HomeViewModel
import com.example.bloodsugar.viewmodel.SharedViewModel
import com.example.bloodsugar.Screen

val items = listOf(
    Screen.Home,
    Screen.Food,
    Screen.Notifications,
    Screen.Calculator,
    Screen.Analysis
)

class MainActivity : ComponentActivity() {
    
    private val sharedViewModel: SharedViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, PersistentNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        setContent {
            BloodSugarTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blood Sugar") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(painterResource(id = screen.icon), contentDescription = null) },
                        label = { Text(screen.route) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            if (currentDestination?.route == Screen.Home.route) {
                var isExpanded by remember { mutableStateOf(false) }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isExpanded) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                homeViewModel.onLogActivityClicked()
                                isExpanded = false
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            icon = { Icon(Icons.Default.FitnessCenter, "Log Activity") },
                            text = { Text(text = "Activity") }
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                homeViewModel.onLogEventClicked()
                                isExpanded = false
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            icon = { Icon(Icons.Default.MedicalServices, "Log Insulin/Carbs") },
                            text = { Text(text = "Carbs and Insulin") }
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                homeViewModel.onLogSugarClicked()
                                isExpanded = false
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            icon = { Icon(Icons.Default.WaterDrop, "Log Blood Sugar") },
                            text = { Text(text = "Sugar level") }
                        )
                    }
                    FloatingActionButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Add Log"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Home.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(homeViewModel = homeViewModel) }
            composable(Screen.Food.route) { FoodScreen() }
            composable(Screen.Notifications.route) { NotificationsScreen() }
            composable(Screen.Calculator.route) { CalculatorScreen(navController = navController, homeViewModel = homeViewModel) }
            composable(Screen.Analysis.route) { com.example.bloodsugar.ui.screens.AnalysisScreen() }
            composable("settings") { SettingsScreen(navController) }
        }
    }
}