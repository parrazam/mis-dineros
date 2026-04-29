package com.parra.misdineros.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parra.misdineros.R
import com.parra.misdineros.designsystem.theme.AppTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val SUPPORTED_CURRENCIES = listOf(
    "EUR", "USD", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "MXN", "BRL",
    "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "RON", "BGN", "HRK", "RUB",
    "TRY", "INR", "KRW", "SGD", "HKD", "NZD",
)

private val NOTIFY_DAYS_OPTIONS = listOf(1, 2, 3, 5, 7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToFxRates: () -> Unit,
    onNavigateToCategories: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrDefault("—")
    }

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { viewModel.exportData(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportDialog = true
        }
    }

    val isLoading = backupState is BackupState.Loading
    val exportSuccessMsg = stringResource(R.string.backup_export_success)
    val importSuccessMsg = stringResource(R.string.backup_import_success)

    LaunchedEffect(backupState) {
        when (val state = backupState) {
            is BackupState.ExportSuccess -> {
                snackbarHostState.showSnackbar(exportSuccessMsg)
                viewModel.clearBackupState()
            }
            is BackupState.ImportSuccess -> {
                snackbarHostState.showSnackbar(importSuccessMsg)
                viewModel.clearBackupState()
            }
            is BackupState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearBackupState()
            }
            else -> {}
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = settings.notificationHour,
            initialMinute = settings.notificationMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.settings_notif_hour)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setNotifTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showImportDialog && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false; pendingImportUri = null },
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importData(pendingImportUri!!)
                    showImportDialog = false
                    pendingImportUri = null
                }) { Text(stringResource(R.string.backup_import_confirm_button)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    pendingImportUri = null
                }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {
            // ── General ──────────────────────────────────────────────────────────
            SettingsSectionTitle("General")
            DropdownSettingsItem(
                title = stringResource(R.string.settings_currency),
                value = settings.globalCurrencyCode,
                options = SUPPORTED_CURRENCIES,
                onSelect = viewModel::setCurrency,
                leadingIcon = Icons.Default.CurrencyExchange,
            )
            HorizontalDivider()

            // ── Apariencia ────────────────────────────────────────────────────────
            SettingsSectionTitle("Apariencia")
            ThemePickerItem(
                current = settings.appTheme,
                onSelect = viewModel::setTheme,
            )
            HorizontalDivider()

            // ── Notificaciones ────────────────────────────────────────────────────
            SettingsSectionTitle(stringResource(R.string.settings_notifications))
            SwitchSettingsItem(
                title = stringResource(R.string.settings_notif_enabled),
                checked = settings.notificationsEnabled,
                onCheckedChange = viewModel::setNotifsEnabled,
                leadingIcon = Icons.Default.Notifications,
            )
            if (settings.notificationsEnabled) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_notif_hour)) },
                    supportingContent = {
                        Text("%02d:%02d".format(settings.notificationHour, settings.notificationMinute))
                    },
                    modifier = Modifier.clickable { showTimePicker = true },
                )
                DropdownSettingsItem(
                    title = stringResource(R.string.settings_notif_default_days),
                    value = settings.defaultNotifyDaysBefore.toString(),
                    options = NOTIFY_DAYS_OPTIONS.map { it.toString() },
                    onSelect = { viewModel.setNotifyDays(it.toInt()) },
                    optionLabel = { "$it días" },
                )
                SwitchSettingsItem(
                    title = stringResource(R.string.settings_monthly_summary),
                    checked = settings.monthlySummaryEnabled,
                    onCheckedChange = viewModel::setSummaryEnabled,
                )
            }
            HorizontalDivider()

            // ── Datos ─────────────────────────────────────────────────────────────
            SettingsSectionTitle("Datos")
            NavSettingsItem(
                title = stringResource(R.string.settings_categories),
                leadingIcon = Icons.Default.Category,
                onClick = onNavigateToCategories,
            )
            NavSettingsItem(
                title = stringResource(R.string.settings_fx_rates),
                leadingIcon = Icons.Default.CurrencyExchange,
                onClick = onNavigateToFxRates,
            )
            NavSettingsItem(
                title = stringResource(R.string.settings_export),
                leadingIcon = Icons.Default.FileUpload,
                enabled = !isLoading,
                onClick = {
                    val timestamp = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                    exportLauncher.launch("mis-dineros-backup-$timestamp.json")
                },
            )
            NavSettingsItem(
                title = stringResource(R.string.settings_import),
                leadingIcon = Icons.Default.FileDownload,
                enabled = !isLoading,
                onClick = { importLauncher.launch(arrayOf("application/json")) },
            )
            HorizontalDivider()

            // ── Acerca de ─────────────────────────────────────────────────────────
            SettingsSectionTitle(stringResource(R.string.settings_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version, versionName ?: "—")) },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            )
        }
    }
}

// ─── Section helpers ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp, end = 16.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSettingsItem(
    title: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    leadingIcon: ImageVector? = null,
    optionLabel: (String) -> String = { it },
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(optionLabel(value)) },
            leadingContent = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            trailingContent = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelect(option); expanded = false },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun SwitchSettingsItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingIcon: ImageVector? = null,
    supportingText: String? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
    )
}

@Composable
private fun NavSettingsItem(
    title: String,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = leadingIcon?.let { { Icon(it, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) } },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
        modifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePickerItem(current: AppTheme, onSelect: (AppTheme) -> Unit) {
    val options = listOf(
        AppTheme.SYSTEM to "Sistema",
        AppTheme.LIGHT to "Claro",
        AppTheme.DARK to "Oscuro",
    )
    ListItem(
        headlineContent = { Text("Tema") },
        trailingContent = {
            SingleChoiceSegmentedButtonRow {
                options.forEachIndexed { index, (theme, label) ->
                    SegmentedButton(
                        selected = current == theme,
                        onClick = { onSelect(theme) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        },
    )
}
