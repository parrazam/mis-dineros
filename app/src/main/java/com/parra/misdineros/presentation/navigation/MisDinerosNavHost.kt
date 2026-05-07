package com.parra.misdineros.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
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

private val tabEnter = fadeIn()
private val tabExit = fadeOut()

private val slideEnter = slideInHorizontally(initialOffsetX = { it }) + fadeIn()
private val slideExit = slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
private val slidePopEnter = slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn()
private val slidePopExit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()

@Composable
fun MisDinerosNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home,
        modifier = modifier
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding),
        enterTransition = { tabEnter },
        exitTransition = { tabExit },
        popEnterTransition = { tabEnter },
        popExitTransition = { tabExit },
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
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit },
        ) {
            SubscriptionDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Destination.SubscriptionEdit(id))
                },
            )
        }

        composable<Destination.SubscriptionEdit>(
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit },
        ) {
            SubscriptionEditScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<Destination.FxRatesEditor>(
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit },
        ) {
            FxRatesEditorScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Destination.CategoryEditor>(
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit },
        ) {
            CategoryEditorScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
