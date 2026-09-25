package com.parra.misdineros.presentation.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parra.misdineros.R
import com.parra.misdineros.data.backup.BackupCrypto
import com.parra.misdineros.designsystem.component.AppCard
import com.parra.misdineros.designsystem.component.AppLargeTopBar
import com.parra.misdineros.designsystem.component.IconTile
import com.parra.misdineros.designsystem.component.SectionLabel
import com.parra.misdineros.designsystem.component.SegmentedControl
import com.parra.misdineros.designsystem.theme.AppTheme
import com.parra.misdineros.designsystem.theme.BricolageGrotesque
import com.parra.misdineros.designsystem.theme.MisDinerosTheme
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrDefault("—")
    }

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var importPasswordError by remember { mutableStateOf(false) }
    var importPasswordInput by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    var showExportModeDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
    ) { uri -> uri?.let { viewModel.exportData(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportDialog = true
        }
    }

    val shareChooserTitle = stringResource(R.string.backup_share_chooser)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BackupEvent.Share -> {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = event.mime
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, shareChooserTitle))
                }
            }
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
                pendingImportUri = null
                showPasswordDialog = false
                snackbarHostState.showSnackbar(importSuccessMsg)
                viewModel.clearBackupState()
            }
            is BackupState.PasswordRequired -> {
                importPasswordError = false
                importPasswordInput = ""
                showPasswordDialog = true
                viewModel.clearBackupState()
            }
            is BackupState.WrongPassword -> {
                importPasswordError = true
                viewModel.clearBackupState()
            }
            is BackupState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearBackupState()
            }
            else -> {}
        }
    }

    // ── Diálogos ──────────────────────────────────────────────────────────────

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
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showExportModeDialog) {
        ExportModeDialog(
            onDismiss = { showExportModeDialog = false },
            onSaveToFile = { password ->
                showExportModeDialog = false
                viewModel.setPendingExportPassword(password)
                val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                val ext = if (password != null) "mdb" else "json"
                exportLauncher.launch("mis-dineros-backup-$ts.$ext")
            },
            onShare = { password ->
                showExportModeDialog = false
                viewModel.exportAndShare(password)
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
                    // pendingImportUri se conserva por si el archivo está cifrado
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

    if (showPasswordDialog && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                pendingImportUri = null
                importPasswordError = false
                importPasswordInput = ""
            },
            title = { Text(stringResource(R.string.backup_encrypted_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_encrypted_message))
                    OutlinedTextField(
                        value = importPasswordInput,
                        onValueChange = { importPasswordInput = it; importPasswordError = false },
                        label = { Text(stringResource(R.string.backup_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = importPasswordError,
                        supportingText = if (importPasswordError) {
                            { Text(stringResource(R.string.backup_password_wrong)) }
                        } else null,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importData(pendingImportUri!!, importPasswordInput.toCharArray())
                    },
                    enabled = importPasswordInput.isNotEmpty() && !isLoading,
                ) { Text(stringResource(R.string.backup_import_confirm_button)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    pendingImportUri = null
                    importPasswordError = false
                    importPasswordInput = ""
                }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    Scaffold(
        topBar = { AppLargeTopBar(stringResource(R.string.settings_title), scrollBehavior) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            // ── General ──────────────────────────────────────────────────────────
            SettingsGroup(stringResource(R.string.settings_section_general)) {
                DropdownSettingsItem(
                    title = stringResource(R.string.settings_currency),
                    value = settings.globalCurrencyCode,
                    options = SUPPORTED_CURRENCIES,
                    onSelect = viewModel::setCurrency,
                    leadingIcon = Icons.Outlined.Payments,
                )
            }

            // ── Apariencia ────────────────────────────────────────────────────────
            SettingsGroup(stringResource(R.string.settings_section_appearance)) {
                ThemePickerItem(
                    current = settings.appTheme,
                    onSelect = viewModel::setTheme,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    GroupDivider()
                    SwitchSettingsItem(
                        title = stringResource(R.string.settings_dynamic_color),
                        supportingText = stringResource(R.string.settings_dynamic_color_support),
                        checked = settings.dynamicColorEnabled,
                        onCheckedChange = viewModel::setDynamicColorEnabled,
                        leadingIcon = Icons.Outlined.Palette,
                    )
                }
            }

            // ── Notificaciones ────────────────────────────────────────────────────
            SettingsGroup(stringResource(R.string.settings_notifications)) {
                SwitchSettingsItem(
                    title = stringResource(R.string.settings_notif_enabled),
                    checked = settings.notificationsEnabled,
                    onCheckedChange = viewModel::setNotifsEnabled,
                    leadingIcon = Icons.Outlined.Notifications,
                )
                if (settings.notificationsEnabled) {
                    GroupDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_notif_hour)) },
                        leadingContent = { IconTile(Icons.Outlined.Schedule) },
                        trailingContent = {
                            ValuePill("%02d:%02d".format(settings.notificationHour, settings.notificationMinute))
                        },
                        colors = transparentListItemColors(),
                        modifier = Modifier.clickable { showTimePicker = true },
                    )
                    GroupDivider()
                    DropdownSettingsItem(
                        title = stringResource(R.string.settings_notif_default_days),
                        value = settings.defaultNotifyDaysBefore.toString(),
                        options = NOTIFY_DAYS_OPTIONS.map { it.toString() },
                        onSelect = { viewModel.setNotifyDays(it.toInt()) },
                        leadingIcon = Icons.Outlined.Event,
                        optionLabel = { "$it días" },
                    )
                    GroupDivider()
                    SwitchSettingsItem(
                        title = stringResource(R.string.settings_monthly_summary),
                        checked = settings.monthlySummaryEnabled,
                        onCheckedChange = viewModel::setSummaryEnabled,
                        leadingIcon = Icons.Outlined.Insights,
                    )
                    GroupDivider()
                    OutlinedButton(
                        onClick = viewModel::testNotificationNow,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.settings_test_notification))
                    }
                }
            }

            // ── Datos ─────────────────────────────────────────────────────────────
            SettingsGroup(stringResource(R.string.settings_section_data)) {
                NavSettingsItem(
                    title = stringResource(R.string.settings_categories),
                    leadingIcon = Icons.Outlined.Category,
                    onClick = onNavigateToCategories,
                )
                GroupDivider()
                NavSettingsItem(
                    title = stringResource(R.string.settings_fx_rates),
                    leadingIcon = Icons.Outlined.CurrencyExchange,
                    onClick = onNavigateToFxRates,
                )
                GroupDivider()
                NavSettingsItem(
                    title = stringResource(R.string.settings_export),
                    leadingIcon = Icons.Outlined.FileUpload,
                    enabled = !isLoading,
                    onClick = { showExportModeDialog = true },
                )
                GroupDivider()
                NavSettingsItem(
                    title = stringResource(R.string.settings_import),
                    supportingText = stringResource(R.string.settings_import_warning),
                    leadingIcon = Icons.Outlined.FileDownload,
                    enabled = !isLoading,
                    warning = true,
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                )
                GroupDivider()
                SwitchSettingsItem(
                    title = stringResource(R.string.settings_auto_backup),
                    supportingText = "Restaura tus datos al reinstalar o cambiar de móvil. Se sube cifrado a tu cuenta de Google. Máx. 25 MB.",
                    checked = settings.autoBackupEnabled,
                    onCheckedChange = viewModel::setAutoBackupEnabled,
                    leadingIcon = Icons.Outlined.Backup,
                )
            }

            // ── Acerca de ─────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = BricolageGrotesque),
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = stringResource(R.string.settings_version, versionName ?: "—"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── Diálogo de exportación ───────────────────────────────────────────────────

@Composable
private fun ExportModeDialog(
    onDismiss: () -> Unit,
    onSaveToFile: (password: CharArray?) -> Unit,
    onShare: (password: CharArray?) -> Unit,
) {
    var encryptEnabled by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }

    val passwordsMatch = password == passwordConfirm
    val passwordValid = !encryptEnabled || (password.length >= BackupCrypto.MIN_PASSWORD_LENGTH && passwordsMatch)

    fun resolvedPassword(): CharArray? = if (encryptEnabled) password.toCharArray() else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_export)) },
        text = {
            Column {
                SwitchSettingsItem(
                    title = stringResource(R.string.backup_encrypt_with_password),
                    checked = encryptEnabled,
                    onCheckedChange = { enabled ->
                        encryptEnabled = enabled
                        if (!enabled) { password = ""; passwordConfirm = "" }
                    },
                )
                if (encryptEnabled) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.backup_password)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = passwordConfirm,
                            onValueChange = { passwordConfirm = it },
                            label = { Text(stringResource(R.string.backup_password_confirm)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            isError = passwordConfirm.isNotEmpty() && !passwordsMatch,
                            supportingText = when {
                                passwordConfirm.isNotEmpty() && !passwordsMatch ->
                                    { { Text(stringResource(R.string.backup_password_mismatch)) } }
                                password.isNotEmpty() && password.length < BackupCrypto.MIN_PASSWORD_LENGTH ->
                                    { { Text(stringResource(R.string.backup_password_min_length, BackupCrypto.MIN_PASSWORD_LENGTH)) } }
                                else -> null
                            },
                        )
                        Text(
                            text = "Si pierdes la contraseña, no podrás recuperar este archivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.backup_save_to_files)) },
                    supportingContent = { Text(stringResource(R.string.backup_save_to_files_support)) },
                    leadingContent = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                    modifier = Modifier
                        .alpha(if (passwordValid) 1f else 0.38f)
                        .clickable(enabled = passwordValid) { onSaveToFile(resolvedPassword()) },
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.backup_share)) },
                    supportingContent = { Text(stringResource(R.string.backup_share_support)) },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier
                        .alpha(if (passwordValid) 1f else 0.38f)
                        .clickable(enabled = passwordValid) { onShare(resolvedPassword()) },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

// ─── Section helpers ──────────────────────────────────────────────────────────

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    SectionLabel(
        text = title,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
    AppCard(modifier = Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MisDinerosTheme.colors.cardDivider,
    )
}

@Composable
private fun transparentListItemColors() = ListItemDefaults.colors(containerColor = Color.Transparent)

@Composable
private fun ValuePill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MisDinerosTheme.colors.cardDivider)
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
            leadingContent = leadingIcon?.let { { IconTile(it) } },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ValuePill(optionLabel(value))
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            },
            colors = transparentListItemColors(),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
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
        leadingContent = leadingIcon?.let { { IconTile(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        colors = transparentListItemColors(),
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
    warning: Boolean = false,
) {
    val colors = MisDinerosTheme.colors
    val disabled = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    ListItem(
        headlineContent = { Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface else disabled) },
        supportingContent = supportingText?.let {
            { Text(it, color = if (warning && enabled) colors.onUrgentContainer else MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        leadingContent = leadingIcon?.let {
            {
                IconTile(
                    icon = it,
                    containerColor = if (warning) colors.urgentContainer else colors.iconTile,
                    contentColor = if (warning) colors.onUrgentContainer else colors.onIconTile,
                    modifier = Modifier.alpha(if (enabled) 1f else 0.38f),
                )
            }
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else disabled,
            )
        },
        colors = transparentListItemColors(),
        modifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

@Composable
private fun ThemePickerItem(current: AppTheme, onSelect: (AppTheme) -> Unit) {
    val options = listOf(
        AppTheme.SYSTEM to stringResource(R.string.settings_theme_system),
        AppTheme.LIGHT to stringResource(R.string.settings_theme_light),
        AppTheme.DARK to stringResource(R.string.settings_theme_dark),
    )
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconTile(Icons.Outlined.Contrast)
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        SegmentedControl(
            options = options.map { it.second },
            selectedIndex = options.indexOfFirst { it.first == current }.coerceAtLeast(0),
            onSelect = { index -> onSelect(options[index].first) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
