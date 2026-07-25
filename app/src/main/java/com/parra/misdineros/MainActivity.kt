package com.parra.misdineros

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.parra.misdineros.designsystem.theme.MisDinerosTheme
import com.parra.misdineros.presentation.navigation.Destination
import com.parra.misdineros.presentation.navigation.MisDinerosNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var navController: NavHostController? = null

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permiso de notificación concedido o denegado — sin forzar */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (resources.getBoolean(R.bool.lock_portrait_orientation)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            val themeConfig by mainViewModel.themeConfig.collectAsStateWithLifecycle()
            MisDinerosTheme(appTheme = themeConfig.appTheme, dynamicColor = themeConfig.dynamicColor) {
                val navController = rememberNavController().also { this@MainActivity.navController = it }
                val currentEntry by navController.currentBackStackEntryAsState()
                val currentDestination = currentEntry?.destination

                val showBottomBar = currentDestination?.run {
                    hasRoute<Destination.Home>() ||
                    hasRoute<Destination.SubscriptionList>() ||
                    hasRoute<Destination.Stats>() ||
                    hasRoute<Destination.Settings>()
                } ?: false

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentDestination?.hasRoute<Destination.Home>() == true,
                                    onClick = {
                                        navController.navigate(Destination.Home) {
                                            popUpTo<Destination.Home> { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text(stringResource(R.string.nav_home)) },
                                )
                                NavigationBarItem(
                                    selected = currentDestination?.hasRoute<Destination.SubscriptionList>() == true,
                                    onClick = {
                                        navController.navigate(Destination.SubscriptionList) {
                                            popUpTo<Destination.Home> { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                    label = { Text(stringResource(R.string.nav_subscriptions)) },
                                )
                                NavigationBarItem(
                                    selected = currentDestination?.hasRoute<Destination.Stats>() == true,
                                    onClick = {
                                        navController.navigate(Destination.Stats) {
                                            popUpTo<Destination.Home> { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                                    label = { Text(stringResource(R.string.nav_stats)) },
                                )
                                NavigationBarItem(
                                    selected = currentDestination?.hasRoute<Destination.Settings>() == true,
                                    onClick = {
                                        navController.navigate(Destination.Settings) {
                                            popUpTo<Destination.Home> { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navController?.handleDeepLink(intent)
    }

    /**
     * Se pide una sola vez por creación de la activity y solo si falta el permiso. Lanzarlo en
     * cada onResume gastaba el cupo de diálogos del sistema, que tras un par de descartes deja
     * de mostrarlos y deniega en silencio.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
