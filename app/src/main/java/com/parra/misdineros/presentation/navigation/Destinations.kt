package com.parra.misdineros.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

sealed interface Destination {
    // ─── Bottom nav ────────────────────────────────────────────────────────────
    @Serializable
    data object Home : Destination

    @Serializable
    data object SubscriptionList : Destination

    @Serializable
    data object Stats : Destination

    // ─── Detalle / edición ─────────────────────────────────────────────────────
    @Serializable
    data class SubscriptionEdit(val id: String? = null) : Destination

    @Serializable
    data class SubscriptionDetail(val id: String) : Destination

    // ─── Ajustes ───────────────────────────────────────────────────────────────
    @Serializable
    data object Settings : Destination

    @Serializable
    data object FxRatesEditor : Destination

    @Serializable
    data object CategoryEditor : Destination
}

data class BottomNavItem(
    val route: Destination,
    val icon: ImageVector,
    val labelRes: Int,
)

val bottomNavItems = listOf(
    BottomNavItem(Destination.Home, Icons.Default.Home, android.R.string.ok),
    BottomNavItem(Destination.SubscriptionList, Icons.Default.List, android.R.string.ok),
    BottomNavItem(Destination.Stats, Icons.Default.BarChart, android.R.string.ok),
)
