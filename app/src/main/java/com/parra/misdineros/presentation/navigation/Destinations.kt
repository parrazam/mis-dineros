package com.parra.misdineros.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String) {

    // ─── Bottom nav ────────────────────────────────────────────────────────────
    data object Home : Destination("home")
    data object SubscriptionList : Destination("subscriptions")
    data object Stats : Destination("stats")

    // ─── Detalle / edición ─────────────────────────────────────────────────────
    data object SubscriptionEdit : Destination("subscriptions/edit?id={id}") {
        const val ARG_ID = "id"
        fun route(id: String? = null) = if (id != null) "subscriptions/edit?id=$id" else "subscriptions/edit"
    }

    data object SubscriptionDetail : Destination("subscriptions/{id}") {
        const val ARG_ID = "id"
        fun route(id: String) = "subscriptions/$id"
    }

    // ─── Ajustes ───────────────────────────────────────────────────────────────
    data object Settings : Destination("settings")
    data object FxRatesEditor : Destination("settings/fx-rates")
    data object CategoryEditor : Destination("settings/categories")
}

data class BottomNavItem(
    val destination: Destination,
    val icon: ImageVector,
    val labelRes: Int,
)

val bottomNavItems = listOf(
    BottomNavItem(Destination.Home, Icons.Default.Home, android.R.string.ok), // replaced in UI with string res
    BottomNavItem(Destination.SubscriptionList, Icons.Default.List, android.R.string.ok),
    BottomNavItem(Destination.Stats, Icons.Default.BarChart, android.R.string.ok),
)
