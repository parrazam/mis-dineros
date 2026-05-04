package com.parra.misdineros.presentation.subscriptions.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parra.misdineros.R
import com.parra.misdineros.designsystem.component.SubscriptionCard
import com.parra.misdineros.domain.model.BillingCycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionListScreen(
    onNavigateToEdit: (String?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onAddNew: () -> Unit,
    viewModel: SubscriptionListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.nav_subscriptions)) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_subscription))
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            !state.hasAnySubscription -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.subscriptions_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    CycleFilterRow(
                        activeFilter = state.activeFilter,
                        onFilterChange = viewModel::setFilter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    if (state.items.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(
                                    when (state.activeFilter) {
                                        BillingCycle.MONTHLY -> R.string.subscriptions_empty_filtered_monthly
                                        BillingCycle.ANNUAL -> R.string.subscriptions_empty_filtered_annual
                                        null -> R.string.subscriptions_empty
                                    }
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(32.dp),
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.items, key = { it.subscription.id }) { item ->
                                SwipeToDeleteItem(
                                    item = item,
                                    onTap = { onNavigateToDetail(item.subscription.id) },
                                    onEdit = { onNavigateToEdit(item.subscription.id) },
                                    onTogglePause = { viewModel.togglePause(item.subscription.id) },
                                    onDelete = { viewModel.delete(item.subscription.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleFilterRow(
    activeFilter: BillingCycle?,
    onFilterChange: (BillingCycle?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        null to R.string.subscriptions_filter_all,
        BillingCycle.MONTHLY to R.string.subscriptions_filter_monthly,
        BillingCycle.ANNUAL to R.string.subscriptions_filter_annual,
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, (cycle, labelRes) ->
            SegmentedButton(
                selected = activeFilter == cycle,
                onClick = { onFilterChange(cycle) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(stringResource(labelRes)) },
            )
        }
    }
}

@Composable
private fun SwipeToDeleteItem(
    item: SubscriptionListItem,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    // Locked to true once the action fires; reset in onDragStart of the next gesture.
    var actionFired by remember { mutableStateOf(false) }
    var isSwipingRight by remember { mutableStateOf<Boolean?>(null) }
    val thresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Background hint icons, only visible while the user is actively dragging.
        Box(modifier = Modifier.matchParentSize()) {
            when (isSwipingRight) {
                true -> Icon(
                    imageVector = if (item.subscription.isPaused) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp),
                )
                false -> Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                )
                null -> Unit
            }
        }

        SubscriptionCard(
            subscription = item.subscription,
            category = item.category,
            onTap = onTap,
            onEdit = onEdit,
            onTogglePause = onTogglePause,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(item.subscription.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            actionFired = false
                            isSwipingRight = null
                        },
                        onDragEnd = {
                            isSwipingRight = null
                            scope.launch { offsetX.animateTo(0f, spring()) }
                        },
                        onDragCancel = {
                            isSwipingRight = null
                            scope.launch { offsetX.animateTo(0f) }
                        },
                        onHorizontalDrag = { _, delta ->
                            val cap = thresholdPx * 1.5f
                            val newOffset = (offsetX.value + delta).coerceIn(-cap, cap)
                            scope.launch { offsetX.snapTo(newOffset) }
                            if (isSwipingRight == null) isSwipingRight = delta > 0
                            if (!actionFired) {
                                when {
                                    newOffset >= thresholdPx -> {
                                        actionFired = true
                                        onTogglePause()
                                    }
                                    newOffset <= -thresholdPx -> {
                                        actionFired = true
                                        showConfirm = true
                                    }
                                }
                            }
                        },
                    )
                },
        )
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Eliminar suscripción") },
            text = { Text("¿Seguro que quieres eliminar «${item.subscription.name}»?") },
            confirmButton = {
                TextButton(
                    onClick = { showConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
