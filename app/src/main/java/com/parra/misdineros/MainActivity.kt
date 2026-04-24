package com.parra.misdineros

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.parra.misdineros.designsystem.theme.AppTheme
import com.parra.misdineros.designsystem.theme.MisDinerosTheme
import com.parra.misdineros.presentation.navigation.Destination
import com.parra.misdineros.presentation.navigation.MisDinerosNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permiso de notificación concedido o denegado — sin forzar */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appTheme by mainViewModel.appTheme.collectAsStateWithLifecycle()
            MisDinerosTheme(appTheme = appTheme) {
                val navController = rememberNavController()
                val currentEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentEntry?.destination?.route

                val bottomRoutes = listOf(
                    Destination.Home.route,
                    Destination.SubscriptionList.route,
                    Destination.Stats.route,
                    Destination.Settings.route,
                )
                val showBottomBar = currentRoute in bottomRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute == Destination.Home.route,
                                    onClick = {
                                        navController.navigate(Destination.Home.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text(stringResource(R.string.nav_home)) },
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Destination.SubscriptionList.route,
                                    onClick = {
                                        navController.navigate(Destination.SubscriptionList.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                                    label = { Text(stringResource(R.string.nav_subscriptions)) },
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Destination.Stats.route,
                                    onClick = {
                                        navController.navigate(Destination.Stats.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                                    label = { Text(stringResource(R.string.nav_stats)) },
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Destination.Settings.route,
                                    onClick = {
                                        navController.navigate(Destination.Settings.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    label = { Text(stringResource(R.string.nav_settings)) },
                                )
                            }
                        }
                    },
                ) { innerPadding ->
                    MisDinerosNavHost(
                        navController = navController,
                        innerPadding = innerPadding,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
