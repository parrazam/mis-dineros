package com.parra.misdineros.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parra.misdineros.R
import com.parra.misdineros.core.money.MoneyFormatter
import com.parra.misdineros.designsystem.component.AppCard
import com.parra.misdineros.designsystem.component.AppLargeTopBar
import com.parra.misdineros.designsystem.component.ProportionBar
import com.parra.misdineros.designsystem.component.ServiceIcon
import com.parra.misdineros.designsystem.component.StatusPill
import com.parra.misdineros.designsystem.component.URGENT_RENEWAL_DAYS
import com.parra.misdineros.designsystem.component.daysUntilLabel
import com.parra.misdineros.designsystem.theme.MisDinerosTheme
import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.usecase.RankedSubscription
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = { AppLargeTopBar(stringResource(R.string.app_name), scrollBehavior) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            (state.activeCount == 0 && state.pausedCount == 0) -> {
                EmptyHomeState(
                    onGoToSubscriptions = onNavigateToSubscriptions,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
            }

            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    // Además del ancho mínimo se exige que la ventana sea apaisada: en una
                    // tablet en vertical las dos columnas quedan estrechas y dejan media
                    // pantalla vacía (mismo criterio que StatsScreen).
                    val isWide = maxWidth >= 600.dp && maxWidth >= maxHeight
                    if (isWide) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(28.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MonthLabel()
                                    SummaryCard(state = state)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(28.dp),
                            ) {
                                if (state.upcomingRenewals.isNotEmpty()) {
                                    UpcomingSection(
                                        renewals = state.upcomingRenewals,
                                        onSeeAll = onNavigateToSubscriptions,
                                        onTap = onNavigateToDetail,
                                    )
                                }
                                if (state.top5.isNotEmpty()) {
                                    Top5Section(
                                        ranked = state.top5,
                                        globalCurrency = state.globalCurrency,
                                        onTap = onNavigateToDetail,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(28.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                MonthLabel()
                                SummaryCard(state = state)
                            }

                            if (state.upcomingRenewals.isNotEmpty()) {
                                UpcomingSection(
                                    renewals = state.upcomingRenewals,
                                    onSeeAll = onNavigateToSubscriptions,
                                    onTap = onNavigateToDetail,
                                )
                            }

                            if (state.top5.isNotEmpty()) {
                                Top5Section(
                                    ranked = state.top5,
                                    globalCurrency = state.globalCurrency,
                                    onTap = onNavigateToDetail,
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Month ────────────────────────────────────────────────────────────────────

/** Mes en curso sobre la tarjeta principal; se va con el scroll, el título queda en la barra. */
@Composable
private fun MonthLabel() {
    val month = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("LLLL yyyy")).uppercase()
    }
    Text(
        text = month,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
    )
}

// ─── Summary card ─────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(state: HomeUiState) {
    val colors = MisDinerosTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.hero,
        contentColor = colors.onHero,
    ) {
        Column(
            modifier = Modifier
                .drawBehind {
                    // Dos anillos translúcidos en la esquina superior derecha: textura sin degradados.
                    val center = Offset(size.width - 40.dp.toPx(), 40.dp.toPx())
                    val ring = Color.White.copy(alpha = 0.08f)
                    val stroke = Stroke(width = 18.dp.toPx())
                    drawCircle(ring, radius = 100.dp.toPx(), center = center, style = stroke)
                    drawCircle(ring, radius = 58.dp.toPx(), center = center, style = stroke)
                }
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.home_monthly_spend),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onHeroMuted,
                )
                Text(
                    text = MoneyFormatter.format(state.monthlyTotalMinor, state.globalCurrency),
                    style = MaterialTheme.typography.displayMedium,
                    maxLines = 1,
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .drawBehind { drawRect(Color.White.copy(alpha = 0.14f)) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                Text(
                    text = stringResource(
                        R.string.home_per_year,
                        MoneyFormatter.format(state.annualEquivalentMinor, state.globalCurrency),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            if (state.excludedCurrencies.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.spend_missing_rates, state.excludedCurrencies.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFC9BE),
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.16f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatItem(
                    value = state.activeCount.toString(),
                    label = if (state.activeCount == 1) "activa" else "activas",
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    value = state.pausedCount.toString(),
                    label = if (state.pausedCount == 1) "pausada" else "pausadas",
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    value = state.upcomingRenewals.firstOrNull()?.let { daysUntilLabel(it.daysUntil) } ?: "—",
                    label = stringResource(R.string.home_next_charge),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MisDinerosTheme.colors.onHeroMuted,
        )
    }
}

// ─── Upcoming renewals ────────────────────────────────────────────────────────

@Composable
private fun UpcomingSection(
    renewals: List<UpcomingRenewal>,
    onSeeAll: () -> Unit,
    onTap: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_upcoming_renewals),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            TextButton(onClick = onSeeAll) {
                Text(stringResource(R.string.home_see_all), color = MaterialTheme.colorScheme.secondary)
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp),
        ) {
            items(renewals, key = { it.subscription.id }) { renewal ->
                UpcomingRenewalCard(
                    renewal = renewal,
                    onClick = { onTap(renewal.subscription.id) },
                )
            }
        }
    }
}

private val UpcomingDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")

@Composable
private fun UpcomingRenewalCard(
    renewal: UpcomingRenewal,
    onClick: () -> Unit,
) {
    val colors = MisDinerosTheme.colors
    val urgent = renewal.daysUntil <= URGENT_RENEWAL_DAYS
    val subscription = renewal.subscription

    AppCard(
        modifier = Modifier.width(152.dp),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ServiceIcon(
                    iconRef = subscription.iconRef,
                    fallbackName = subscription.name,
                    size = 40.dp,
                )
                StatusPill(
                    text = daysUntilLabel(renewal.daysUntil),
                    containerColor = if (urgent) colors.urgentContainer else colors.soonContainer,
                    contentColor = if (urgent) colors.onUrgentContainer else colors.onSoonContainer,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subscription.nextRenewalDate.format(UpcomingDateFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = MoneyFormatter.format(subscription.amountMinor, subscription.currencyCode),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
        }
    }
}

// ─── Top 5 ────────────────────────────────────────────────────────────────────

@Composable
private fun Top5Section(
    ranked: List<RankedSubscription>,
    globalCurrency: String,
    onTap: (String) -> Unit,
) {
    val max = ranked.maxOf { it.monthlyAmountInTarget }.coerceAtLeast(1L)
    val hasAnnual = ranked.any { it.subscription.billingCycle == BillingCycle.ANNUAL }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.home_top_expensive),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                ranked.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MisDinerosTheme.colors.cardDivider,
                        )
                    }
                    Top5Item(
                        item = item,
                        fraction = item.monthlyAmountInTarget.toFloat() / max,
                        globalCurrency = globalCurrency,
                        onClick = { onTap(item.subscription.id) },
                    )
                }
            }
        }
        if (hasAnnual) {
            Text(
                text = stringResource(R.string.home_annual_note),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun Top5Item(
    item: RankedSubscription,
    fraction: Float,
    globalCurrency: String,
    onClick: () -> Unit,
) {
    val subscription = item.subscription
    AppCard(
        onClick = onClick,
        color = Color.Transparent,
        border = null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServiceIcon(
                iconRef = subscription.iconRef,
                fallbackName = subscription.name,
                size = 40.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = subscription.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (subscription.billingCycle == BillingCycle.ANNUAL) {
                            Text(
                                text = "· " + stringResource(
                                    R.string.amount_per_year_inline,
                                    MoneyFormatter.format(subscription.amountMinor, subscription.currencyCode),
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        text = MoneyFormatter.format(item.monthlyAmountInTarget, globalCurrency),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                ProportionBar(fraction = fraction)
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyHomeState(onGoToSubscriptions: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Aún no tienes suscripciones",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Añade tu primera suscripción para ver aquí el resumen de gastos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGoToSubscriptions) {
            Text(stringResource(R.string.home_add_subscription))
            Spacer(Modifier.size(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
