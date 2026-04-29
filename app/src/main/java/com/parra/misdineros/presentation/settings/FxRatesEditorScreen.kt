package com.parra.misdineros.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.parra.misdineros.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FxRatesEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: FxRatesEditorViewModel = hiltViewModel(),
) {
    val rates by viewModel.rates.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Restablecer tasas") },
            text = { Text("Se restaurarán los valores predeterminados incluidos con la aplicación. ¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetToDefaults()
                    showResetDialog = false
                }) { Text("Restablecer") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancelar") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasas de cambio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restablecer")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (rates.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(innerPadding)) {
            Text(
                text = "Tasas con base EUR · pulsa ✓ del teclado para guardar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn {
                items(rates, key = { it.quote }) { rate ->
                    FxRateRow(
                        quote = rate.quote,
                        initialRate = rate.rate,
                        onSave = { newRate -> viewModel.saveRate(rate.quote, newRate) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FxRateRow(
    quote: String,
    initialRate: Double,
    onSave: (Double) -> Unit,
) {
    var text by rememberSaveable(initialRate) { mutableStateOf("%.4f".format(initialRate)) }
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "EUR → $quote",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                text.replace(",", ".").toDoubleOrNull()?.let { onSave(it) }
                focusManager.clearFocus()
            }),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
}
