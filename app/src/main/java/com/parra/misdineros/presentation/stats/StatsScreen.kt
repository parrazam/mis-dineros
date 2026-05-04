package com.parra.misdineros.presentation.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.parra.misdineros.R
import com.parra.misdineros.core.money.MoneyFormatter
import com.parra.misdineros.designsystem.component.ServiceIcon
import com.parra.misdineros.domain.usecase.RankedSubscription
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.stats_by_category),
        stringResource(R.string.stats_global),
        stringResource(R.string.stats_top5),
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_stats)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            when (selectedTab) {
                0 -> PorCategoriaTab(state)
                1 -> GlobalTab(state)
                2 -> Top5Tab(state)
            }
        }
    }
}

// ─── Tab 1: Por categoría ─────────────────────────────────────────────────────

@Composable
private fun PorCategoriaTab(state: StatsUiState) {
    if (state.categoryItems.isEmpty()) {
        EmptyStats()
        return
    }

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        DonutChart(
            items = state.categoryItems,
            globalCurrency = state.globalCurrency,
            totalMinor = state.monthlyTotalMinor,
            selectedCategoryId = selectedCategoryId,
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .aspectRatio(1f)
                .align(Alignment.CenterHorizontally),
        )

        DonutLegend(
            items = state.categoryItems,
            globalCurrency = state.globalCurrency,
            selectedCategoryId = selectedCategoryId,
            onCategoryClick = { categoryId ->
                selectedCategoryId = if (selectedCategoryId == categoryId) null else categoryId
            },
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DonutChart(
    items: List<CategorySpendItem>,
    globalCurrency: String,
    totalMinor: Long,
    selectedCategoryId: String?,
    modifier: Modifier = Modifier,
) {
    val selectedItem = items.firstOrNull { it.category.id == selectedCategoryId }
    val hasSelection = selectedCategoryId != null

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.16f
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            val total = items.sumOf { it.monthlyAmountMinor }.toFloat()
            if (total <= 0f) return@Canvas

            val gap = 3f
            val explodeDistance = size.minDimension * 0.06f
            var startAngle = -90f

            items.forEach { item ->
                val sweep = (item.monthlyAmountMinor / total) * 360f
                val isSelected = item.category.id == selectedCategoryId
                val alpha = if (hasSelection && !isSelected) 0.28f else 1f

                if (isSelected) {
                    val midAngle = startAngle + sweep / 2f
                    val radians = Math.toRadians(midAngle.toDouble())
                    val dx = (cos(radians) * explodeDistance).toFloat()
                    val dy = (sin(radians) * explodeDistance).toFloat()
                    withTransform({ translate(dx, dy) }) {
                        drawArc(
                            color = Color(item.category.colorArgb),
                            startAngle = startAngle + gap / 2f,
                            sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                            useCenter = false,
                            style = Stroke(width = strokeWidth * 1.18f, cap = StrokeCap.Butt),
                            topLeft = topLeft,
                            size = arcSize,
                        )
                    }
                } else {
                    drawArc(
                        color = Color(item.category.colorArgb).copy(alpha = alpha),
                        startAngle = startAngle + gap / 2f,
                        sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        topLeft = topLeft,
                        size = arcSize,
                    )
                }
                startAngle += sweep
            }
        }

        if (selectedItem != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selectedItem.category.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = MoneyFormatter.format(selectedItem.monthlyAmountMinor, globalCurrency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "${selectedItem.percentage.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = MoneyFormatter.format(totalMinor, globalCurrency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "/ mes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DonutLegend(
    items: List<CategorySpendItem>,
    globalCurrency: String,
    selectedCategoryId: String?,
    onCategoryClick: (String) -> Unit,
) {
    val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            items.forEachIndexed { index, item ->
                val isSelected = item.category.id == selectedCategoryId
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryClick(item.category.id) }
                        .background(if (isSelected) highlightColor else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = Color(item.category.colorArgb))
                    }
                    Text(
                        text = item.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = MoneyFormatter.format(item.monthlyAmountMinor, globalCurrency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                    Text(
                        text = "${item.percentage.roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

// ─── Tab 2: Global ────────────────────────────────────────────────────────────

@Composable
private fun GlobalTab(state: StatsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.monthlyTotalMinor == 0L) {
            EmptyStats()
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlobalStatCard(
                label = stringResource(R.string.stats_monthly),
                amount = MoneyFormatter.format(state.monthlyTotalMinor, state.globalCurrency),
                modifier = Modifier.weight(1f),
            )
            GlobalStatCard(
                label = stringResource(R.string.stats_annual),
                amount = MoneyFormatter.format(state.annualEquivalentMinor, state.globalCurrency),
                modifier = Modifier.weight(1f),
            )
        }

        ColumnBarChart(
            monthlyMinor = state.monthlyTotalMinor,
            annualMinor = state.annualEquivalentMinor,
            currency = state.globalCurrency,
        )
    }
}

@Composable
private fun GlobalStatCard(label: String, amount: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ColumnBarChart(monthlyMinor: Long, annualMinor: Long, currency: String) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(monthlyMinor, annualMinor) {
        modelProducer.runTransaction {
            columnSeries { series(monthlyMinor.toFloat(), annualMinor.toFloat()) }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = { _, value, _ ->
                        MoneyFormatter.format(value.toLong(), currency)
                    },
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = { _, value, _ ->
                        if (value.toInt() == 0) "Mensual" else "Anual"
                    },
                ),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(8.dp),
        )
    }
}

// ─── Tab 3: Top 5 ─────────────────────────────────────────────────────────────

@Composable
private fun Top5Tab(state: StatsUiState) {
    if (state.top5.isEmpty()) {
        EmptyStats()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                state.top5.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Top5ListItem(rank = index + 1, item = item, currency = state.globalCurrency)
                }
            }
        }
    }
}

@Composable
private fun Top5ListItem(rank: Int, item: RankedSubscription, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        Text(
            text = item.subscription.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = MoneyFormatter.format(item.monthlyAmountInTarget, currency),
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

// ─── Shared empty state ───────────────────────────────────────────────────────

@Composable
private fun EmptyStats() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Sin datos.\nAñade suscripciones activas para ver estadísticas.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}
