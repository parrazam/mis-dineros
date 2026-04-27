package com.parra.misdineros.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.parra.misdineros.presentation.home.HomeScreen
import com.parra.misdineros.presentation.settings.CategoryEditorScreen
import com.parra.misdineros.presentation.settings.FxRatesEditorScreen
import com.parra.misdineros.presentation.settings.SettingsScreen
import com.parra.misdineros.presentation.stats.StatsScreen
import com.parra.misdineros.presentation.subscriptions.detail.SubscriptionDetailScreen
import com.parra.misdineros.presentation.subscriptions.edit.SubscriptionEditScreen
import com.parra.misdineros.presentation.subscriptions.list.SubscriptionListScreen

@Composable
fun MisDinerosNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home,
        modifier = modifier.padding(innerPadding),
    ) {
        composable<Destination.Home> {
            HomeScreen(
                onNavigateToSubscriptions = { navController.navigate(Destination.SubscriptionList) },
                onNavigateToDetail = { id -> navController.navigate(Destination.SubscriptionDetail(id)) },
            )
        }

        composable<Destination.SubscriptionList> {
            SubscriptionListScreen(
                onNavigateToEdit = { id -> navController.navigate(Destination.SubscriptionEdit(id)) },
                onNavigateToDetail = { id -> navController.navigate(Destination.SubscriptionDetail(id)) },
                onAddNew = { navController.navigate(Destination.SubscriptionEdit()) },
            )
        }

        composable<Destination.Stats> {
            StatsScreen()
        }

        composable<Destination.Settings> {
            SettingsScreen(
                onNavigateToFxRates = { navController.navigate(Destination.FxRatesEditor) },
                onNavigateToCategories = { navController.navigate(Destination.CategoryEditor) },
            )
        }

        composable<Destination.SubscriptionDetail>(
            deepLinks = listOf(navDeepLink<Destination.SubscriptionDetail>(basePath = "misdineros://subscription")),
        ) {
            SubscriptionDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Destination.SubscriptionEdit(id))
                },
            )
        }

        composable<Destination.SubscriptionEdit> {
            SubscriptionEditScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<Destination.FxRatesEditor> {
            FxRatesEditorScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Destination.CategoryEditor> {
            CategoryEditorScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
