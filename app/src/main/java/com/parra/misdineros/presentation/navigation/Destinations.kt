package com.parra.misdineros.presentation.navigation

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
