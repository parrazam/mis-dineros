package com.parra.misdineros.presentation.subscriptions.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parra.misdineros.R
import com.parra.misdineros.designsystem.component.ServiceIcon
import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.presentation.subscriptions.iconpicker.IconPickerBottomSheet
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SubscriptionEditUiEvent.Saved -> onNavigateBack()
            }
        }
    }

    var showIconPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { viewModel.onImagePicked(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditing) stringResource(R.string.edit_subscription)
                        else stringResource(R.string.add_subscription)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save() }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Icon + Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showIconPicker = true },
                ) {
                    ServiceIcon(
                        iconRef = state.iconRef,
                        fallbackName = state.name,
                        size = 56.dp,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Cambiar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.onNameChange(it) },
                    label = { Text(stringResource(R.string.field_name)) },
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            // Amount + Currency
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = { viewModel.onAmountChange(it) },
                    label = { Text(stringResource(R.string.field_amount)) },
                    isError = state.amountError != null,
                    supportingText = state.amountError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )

                var currencyExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = state.currencyCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.field_currency)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false },
                    ) {
                        SUPPORTED_CURRENCIES.forEach { code ->
                            DropdownMenuItem(
                                text = { Text(code) },
                                onClick = { viewModel.onCurrencyChange(code); currencyExpanded = false },
                            )
                        }
                    }
                }
            }

            // Billing cycle
            Text(stringResource(R.string.field_cycle), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                BillingCycle.entries.forEachIndexed { index, cycle ->
                    SegmentedButton(
                        selected = state.billingCycle == cycle,
                        onClick = { viewModel.onBillingCycleChange(cycle) },
                        shape = SegmentedButtonDefaults.itemShape(index, BillingCycle.entries.size),
                        label = {
                            Text(
                                when (cycle) {
                                    BillingCycle.MONTHLY -> stringResource(R.string.billing_monthly)
                                    BillingCycle.ANNUAL -> stringResource(R.string.billing_annual)
                                }
                            )
                        },
                    )
                }
            }

            // Renewal date — read-only field with overlay click
            Box {
                OutlinedTextField(
                    value = state.nextRenewalDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.field_renewal_date)) },
                    trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            // Category
            var categoryExpanded by remember { mutableStateOf(false) }
            val selectedCategory = state.categories.find { it.id == state.categoryId }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.field_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                ) {
                    state.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = { viewModel.onCategoryChange(category.id); categoryExpanded = false },
                        )
                    }
                }
            }

            // Notify days
            val notifyOptions = listOf(null, 1, 3, 7)
            var notifyExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = notifyExpanded,
                onExpandedChange = { notifyExpanded = it },
            ) {
                OutlinedTextField(
                    value = when (state.notifyDaysBefore) {
                        null -> "Predeterminado global"
                        1 -> "1 día antes"
                        else -> "${state.notifyDaysBefore} días antes"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.field_notify_days)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(notifyExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = notifyExpanded,
                    onDismissRequest = { notifyExpanded = false },
                ) {
                    notifyOptions.forEach { days ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (days) {
                                        null -> "Predeterminado global"
                                        1 -> "1 día antes"
                                        else -> "$days días antes"
                                    }
                                )
                            },
                            onClick = { viewModel.onNotifyDaysChange(days); notifyExpanded = false },
                        )
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.onNotesChange(it) },
                label = { Text(stringResource(R.string.field_notes)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
        }
    }

    // Icon picker bottom sheet
    if (showIconPicker) {
        IconPickerBottomSheet(
            sheetState = iconSheetState,
            onDismiss = { scope.launch { iconSheetState.hide() }.invokeOnCompletion { showIconPicker = false } },
            onIconSelected = { viewModel.onIconRefChange(it) },
            onPickFromGallery = { imagePickerLauncher.launch("image/*") },
        )
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.nextRenewalDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onRenewalDateChange(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
