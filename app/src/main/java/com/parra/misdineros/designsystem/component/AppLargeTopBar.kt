package com.parra.misdineros.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable

/**
 * Barra superior de las pantallas principales: título grande que se encoge al hacer scroll
 * (`exitUntilCollapsedScrollBehavior`). El fondo es el de la pantalla también al encogerse,
 * para que no aparezca una franja de otro tono sobre el fondo cálido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLargeTopBar(title: String, scrollBehavior: TopAppBarScrollBehavior) {
    LargeTopAppBar(
        title = { Text(title) },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
    )
}
