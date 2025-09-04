package com.example.bloodsugar

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bloodsugar.notifications.PersistentNotificationService
import com.example.bloodsugar.features.analysis.AnalysisScreen
import com.example.bloodsugar.features.calculator.CalculatorScreen
import com.example.bloodsugar.features.food.FoodScreen
import com.example.bloodsugar.features.home.HomeScreen
import com.example.bloodsugar.features.notifications.NotificationsScreen
import com.example.bloodsugar.features.settings.SettingsScreen
import com.example.bloodsugar.features.history.HistoryScreen
import com.example.bloodsugar.ui.theme.BloodSugarTheme
import com.example.bloodsugar.features.home.HomeViewModel

val bottomBarItems = listOf(
    Screen.Home,
    Screen.Calculator,
    Screen.Analysis
)

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, PersistentNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        handleIntent(intent)
        setContent {
            BloodSugarTheme {
                MainScreen(homeViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        intent.action?.let {
            homeViewModel.handleIntentAction(it)
            intent.action = null // Consume action
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(homeViewModel: HomeViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val drawerItems = listOf(
        Screen.Food,
        Screen.Notifications
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                drawerItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(painterResource(id = screen.icon), contentDescription = null) },
                        label = { Text(stringResource(id = screen.label)) },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                            }
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
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.settings)) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                        }
                        navController.navigate("settings")
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    bottomBarItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(painterResource(id = screen.icon), contentDescription = null) },
                            label = { Text(stringResource(id = screen.label)) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                    launchSingleTop = true
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
                                icon = { Icon(Icons.Default.FitnessCenter, stringResource(id = R.string.log_activity)) },
                                text = { Text(text = stringResource(id = R.string.log_activity)) }
                            )
                            ExtendedFloatingActionButton(
                                onClick = {
                                    homeViewModel.onLogEventClicked()
                                    isExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                icon = { Icon(Icons.Default.MedicalServices, stringResource(id = R.string.log_carbs_and_insulin)) },
                                text = { Text(text = stringResource(id = R.string.log_carbs_and_insulin)) }
                            )
                            ExtendedFloatingActionButton(
                                onClick = {
                                    homeViewModel.onLogSugarClicked()
                                    isExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                icon = { Icon(Icons.Default.WaterDrop, stringResource(id = R.string.log_sugar_level)) },
                                text = { Text(text = stringResource(id = R.string.log_sugar_level)) }
                            )
                        }
                        FloatingActionButton(onClick = { isExpanded = !isExpanded }) {
                            Icon(
                                if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = stringResource(id = R.string.add_log)
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
                composable(Screen.Home.route) { HomeScreen(homeViewModel = homeViewModel, navController = navController) }
                composable(Screen.Food.route) { FoodScreen() }
                composable(Screen.Notifications.route) { NotificationsScreen() }
                composable(Screen.Calculator.route) { CalculatorScreen(navController = navController, homeViewModel = homeViewModel) }
                composable(Screen.Analysis.route) { AnalysisScreen() }
                composable("settings") { SettingsScreen() }
                composable("history") { HistoryScreen(navController = navController, homeViewModel = homeViewModel) }
            }
        }
    }
}