package com.parra.misdineros.presentation.subscriptions.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SubscriptionListScreen(
    onNavigateToEdit: (String?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onAddNew: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Lista de suscripciones — Fase 4")
    }
}
