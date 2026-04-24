package com.parra.misdineros.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.parra.misdineros.presentation.home.HomeScreen
import com.parra.misdineros.presentation.settings.SettingsScreen
import com.parra.misdineros.presentation.stats.StatsScreen
import com.parra.misdineros.presentation.subscriptions.list.SubscriptionListScreen

@Composable
fun MisDinerosNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        modifier = modifier.padding(innerPadding),
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                onNavigateToSubscriptions = { navController.navigate(Destination.SubscriptionList.route) },
                onNavigateToDetail = { id -> navController.navigate(Destination.SubscriptionDetail.route(id)) },
            )
        }

        composable(Destination.SubscriptionList.route) {
            SubscriptionListScreen(
                onNavigateToEdit = { id -> navController.navigate(Destination.SubscriptionEdit.route(id)) },
                onNavigateToDetail = { id -> navController.navigate(Destination.SubscriptionDetail.route(id)) },
                onAddNew = { navController.navigate(Destination.SubscriptionEdit.route()) },
            )
        }

        composable(Destination.Stats.route) {
            StatsScreen()
        }

        composable(Destination.Settings.route) {
            SettingsScreen(
                onNavigateToFxRates = { navController.navigate(Destination.FxRatesEditor.route) },
                onNavigateToCategories = { navController.navigate(Destination.CategoryEditor.route) },
            )
        }

        composable(
            route = Destination.SubscriptionDetail.route,
            arguments = listOf(navArgument(Destination.SubscriptionDetail.ARG_ID) { type = NavType.StringType }),
        ) {
            // Placeholder — se implementa en Fase 4
        }

        composable(
            route = Destination.SubscriptionEdit.route,
            arguments = listOf(
                navArgument(Destination.SubscriptionEdit.ARG_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            // Placeholder — se implementa en Fase 4
        }

        composable(Destination.FxRatesEditor.route) {
            // Placeholder — se implementa en Fase 7
        }

        composable(Destination.CategoryEditor.route) {
            // Placeholder — se implementa en Fase 7
        }
    }
}
