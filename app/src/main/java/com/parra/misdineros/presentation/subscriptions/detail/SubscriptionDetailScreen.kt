package com.parra.misdineros.presentation.subscriptions.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parra.misdineros.R
import com.parra.misdineros.core.money.MoneyFormatter
import com.parra.misdineros.designsystem.component.AppCard
import com.parra.misdineros.designsystem.component.IconTile
import com.parra.misdineros.designsystem.component.ProportionBar
import com.parra.misdineros.designsystem.component.SectionLabel
import com.parra.misdineros.designsystem.component.ServiceIcon
import com.parra.misdineros.designsystem.component.StatusPill
import com.parra.misdineros.designsystem.component.URGENT_RENEWAL_DAYS
import com.parra.misdineros.designsystem.component.daysUntilLabel
import com.parra.misdineros.designsystem.theme.MisDinerosTheme
import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.Subscription
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val RenewalDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM")
private val ShortDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val AnnualDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM")

@Composable
fun SubscriptionDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: SubscriptionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SubscriptionDetailUiEvent.Deleted -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onNavigateBack,
                )
                state.subscription?.let { sub ->
                    CircleIconButton(
                        icon = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_subscription),
                        onClick = { onNavigateToEdit(sub.id) },
                    )
                }
            }
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val subscription = state.subscription ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DetailHeader(subscription = subscription, category = state.category)

            RenewalCard(subscription = subscription)

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                    InfoRow(
                        icon = Icons.Outlined.Sync,
                        label = stringResource(R.string.detail_cycle),
                        value = when (subscription.billingCycle) {
                            BillingCycle.MONTHLY -> stringResource(R.string.billing_monthly)
                            BillingCycle.ANNUAL -> stringResource(R.string.billing_annual)
                        },
                    )
                    InfoDivider()
                    InfoRow(
                        icon = Icons.Outlined.CalendarMonth,
                        label = stringResource(R.string.detail_billing_day),
                        value = when (subscription.billingCycle) {
                            BillingCycle.MONTHLY -> stringResource(R.string.detail_billing_day_monthly, subscription.billingAnchorDay)
                            BillingCycle.ANNUAL -> stringResource(
                                R.string.detail_billing_day_annual,
                                subscription.nextRenewalDate.format(AnnualDayFormat),
                            )
                        },
                    )
                    subscription.notifyDaysBefore?.let { days ->
                        InfoDivider()
                        InfoRow(
                            icon = Icons.Outlined.Notifications,
                            label = stringResource(R.string.detail_notify),
                            value = stringResource(R.string.detail_notify_days, days),
                        )
                    }
                    InfoDivider()
                    InfoRow(
                        icon = Icons.Outlined.Payments,
                        label = stringResource(R.string.field_currency),
                        value = subscription.currencyCode,
                    )
                }
            }

            if (!subscription.notes.isNullOrBlank()) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SectionLabel(text = stringResource(R.string.field_notes))
                        Text(subscription.notes, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.togglePause() },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary),
                ) {
                    Icon(
                        if (subscription.isPaused) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (subscription.isPaused) stringResource(R.string.action_resume)
                        else stringResource(R.string.action_pause),
                        fontWeight = FontWeight.Bold,
                    )
                }

                Button(
                    onClick = { showDeleteConfirm = true },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.action_delete), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_subscription_title)) },
            text = { Text(stringResource(R.string.delete_subscription_message, state.subscription?.name ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; viewModel.delete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val colors = MisDinerosTheme.colors
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, CircleShape),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DetailHeader(subscription: Subscription, category: Category?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ServiceIcon(
            iconRef = subscription.iconRef,
            fallbackName = subscription.name,
            size = 88.dp,
        )
        Text(
            text = subscription.name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (category != null) {
            val categoryColor = Color(category.colorArgb)
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(categoryColor),
                )
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            Text(
                text = MoneyFormatter.format(subscription.amountMinor, subscription.currencyCode),
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = when (subscription.billingCycle) {
                    BillingCycle.MONTHLY -> stringResource(R.string.amount_per_month_suffix)
                    BillingCycle.ANNUAL -> stringResource(R.string.amount_per_year_suffix)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(
            text = when (subscription.billingCycle) {
                BillingCycle.MONTHLY -> stringResource(
                    R.string.home_per_year,
                    MoneyFormatter.format(subscription.annualAmountMinor, subscription.currencyCode),
                )
                BillingCycle.ANNUAL -> stringResource(
                    R.string.detail_per_month,
                    MoneyFormatter.format(subscription.monthlyAmountMinor, subscription.currencyCode),
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RenewalCard(subscription: Subscription) {
    val colors = MisDinerosTheme.colors
    val today = remember { LocalDate.now() }
    val next = subscription.nextRenewalDate
    val previous = when (subscription.billingCycle) {
        BillingCycle.MONTHLY -> next.minusMonths(1)
        BillingCycle.ANNUAL -> next.minusYears(1)
    }
    val totalDays = ChronoUnit.DAYS.between(previous, next).coerceAtLeast(1L)
    val elapsedDays = ChronoUnit.DAYS.between(previous, today).coerceIn(0L, totalDays)
    val daysUntil = ChronoUnit.DAYS.between(today, next)
    val urgent = daysUntil <= URGENT_RENEWAL_DAYS

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.hero,
        contentColor = colors.onHero,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.field_renewal_date),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onHeroMuted,
                    )
                    Text(
                        text = next.format(RenewalDateFormat),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                if (!subscription.isPaused && daysUntil >= 0) {
                    StatusPill(
                        text = daysUntilLabel(daysUntil),
                        containerColor = if (urgent) Color(0xFFF2A27A) else Color.White.copy(alpha = 0.16f),
                        contentColor = if (urgent) Color(0xFF3A1706) else colors.onHero,
                    )
                }
            }
            if (!subscription.isPaused) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProportionBar(
                        fraction = elapsedDays.toFloat() / totalDays,
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.16f),
                        height = 8.dp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = previous.format(ShortDateFormat),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onHeroMuted,
                        )
                        Text(
                            text = stringResource(R.string.detail_cycle_progress, elapsedDays.toInt(), totalDays.toInt()),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onHeroMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(icon = icon)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(color = MisDinerosTheme.colors.cardDivider)
}
