package com.parra.misdineros.presentation.home

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parra.misdineros.R
import com.parra.misdineros.core.money.MoneyFormatter
import com.parra.misdineros.designsystem.component.ServiceIcon
import com.parra.misdineros.domain.usecase.RankedSubscription
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.activeCount == 0 && state.pausedCount == 0 -> {
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
                    val isWide = maxWidth >= 600.dp
                    if (isWide) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                            ) {
                                SummaryCard(state = state)
                                Spacer(Modifier.height(8.dp))
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                            ) {
                                if (state.upcomingRenewals.isNotEmpty()) {
                                    UpcomingSection(
                                        renewals = state.upcomingRenewals,
                                        categories = state.categories,
                                        onTap = onNavigateToDetail,
                                    )
                                }
                                if (state.top5.isNotEmpty()) {
                                    Top5Section(
                                        ranked = state.top5,
                                        categories = state.categories,
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
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            SummaryCard(state = state)

                            if (state.upcomingRenewals.isNotEmpty()) {
                                UpcomingSection(
                                    renewals = state.upcomingRenewals,
                                    categories = state.categories,
                                    onTap = onNavigateToDetail,
                                )
                            }

                            if (state.top5.isNotEmpty()) {
                                Top5Section(
                                    ranked = state.top5,
                                    categories = state.categories,
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

// ─── Summary card ─────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(state: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.home_monthly_spend),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = MoneyFormatter.format(state.monthlyTotalMinor, state.globalCurrency),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.home_annual_equivalent)}: ${
                    MoneyFormatter.format(state.annualEquivalentMinor, state.globalCurrency)
                }",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(
                    value = state.activeCount.toString(),
                    label = if (state.activeCount == 1) "activa" else "activas",
                )
                StatItem(
                    value = state.pausedCount.toString(),
                    label = if (state.pausedCount == 1) "pausada" else "pausadas",
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}

// ─── Upcoming renewals ────────────────────────────────────────────────────────

@Composable
private fun UpcomingSection(
    renewals: List<UpcomingRenewal>,
    categories: Map<String, com.parra.misdineros.domain.model.Category>,
    onTap: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.home_upcoming_renewals),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 8.dp),
        ) {
            items(renewals, key = { it.subscription.id }) { renewal ->
                UpcomingRenewalCard(
                    renewal = renewal,
                    categoryName = categories[renewal.subscription.categoryId]?.name,
                    onClick = { onTap(renewal.subscription.id) },
                )
            }
        }
    }
}

@Composable
private fun UpcomingRenewalCard(
    renewal: UpcomingRenewal,
    categoryName: String?,
    onClick: () -> Unit,
) {
    val (chipBg, chipFg) = when {
        renewal.daysUntil <= 1L -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        renewal.daysUntil <= 3L -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        else -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }

    OutlinedCard(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ServiceIcon(
                    iconRef = renewal.subscription.iconRef,
                    fallbackName = renewal.subscription.name,
                    size = 36.dp,
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = chipBg,
                ) {
                    Text(
                        text = daysLabel(renewal.daysUntil),
                        style = MaterialTheme.typography.labelSmall,
                        color = chipFg,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Text(
                text = renewal.subscription.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (categoryName != null) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Text(
                text = renewal.subscription.nextRenewalDate.format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun daysLabel(daysUntil: Long): String = when (daysUntil) {
    0L -> "Hoy"
    1L -> "Mañana"
    else -> "En $daysUntil días"
}

// ─── Top 5 ────────────────────────────────────────────────────────────────────

@Composable
private fun Top5Section(
    ranked: List<RankedSubscription>,
    categories: Map<String, com.parra.misdineros.domain.model.Category>,
    globalCurrency: String,
    onTap: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.home_top_expensive),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                ranked.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    Top5Item(
                        rank = index + 1,
                        item = item,
                        categoryName = categories[item.subscription.categoryId]?.name,
                        globalCurrency = globalCurrency,
                        onClick = { onTap(item.subscription.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Top5Item(
    rank: Int,
    item: RankedSubscription,
    categoryName: String?,
    globalCurrency: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center,
        )

        ServiceIcon(
            iconRef = item.subscription.iconRef,
            fallbackName = item.subscription.name,
            size = 40.dp,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.subscription.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (categoryName != null) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = MoneyFormatter.format(item.monthlyAmountInTarget, globalCurrency),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "/ mes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            Text("Añadir suscripción")
            Spacer(Modifier.size(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
