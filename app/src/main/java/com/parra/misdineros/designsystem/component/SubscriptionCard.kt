package com.parra.misdineros.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.parra.misdineros.R
import com.parra.misdineros.core.money.MoneyFormatter
import com.parra.misdineros.designsystem.theme.MisDinerosTheme
import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.Subscription
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val ShortDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

/**
 * Fila de la lista de suscripciones. Toque: detalle; pulsación larga: menú con editar y
 * pausar/reactivar (también accesibles desde el detalle y con deslizamiento lateral).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubscriptionCard(
    subscription: Subscription,
    category: Category?,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val colors = MisDinerosTheme.colors
    val shape = MaterialTheme.shapes.large
    val paused = subscription.isPaused
    val daysUntil = remember(subscription.nextRenewalDate) {
        ChronoUnit.DAYS.between(LocalDate.now(), subscription.nextRenewalDate)
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (paused) {
                        Modifier.dashedBorder(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), 22.dp)
                    } else {
                        Modifier
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, shape)
                    },
                )
                .combinedClickable(
                    onClick = onTap,
                    onLongClick = { showMenu = true },
                    onLongClickLabel = stringResource(R.string.content_desc_subscription_options, subscription.name),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ServiceIcon(
                iconRef = subscription.iconRef,
                fallbackName = subscription.name,
                size = 48.dp,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (paused) {
                    StatusPill(
                        text = stringResource(R.string.subscription_paused),
                        containerColor = colors.segmentTrack,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        leadingIcon = Icons.Default.Pause,
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (category != null) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(category.colorArgb)),
                            )
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (daysUntil in 0..URGENT_RENEWAL_DAYS) {
                            Text(
                                text = daysUntilLabel(daysUntil),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onUrgentContainer,
                                maxLines = 1,
                            )
                        } else {
                            Text(
                                text = subscription.nextRenewalDate.format(ShortDateFormat),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = MoneyFormatter.format(subscription.amountMinor, subscription.currencyCode),
                    style = MaterialTheme.typography.titleLarge.copy(
                        textDecoration = if (paused) TextDecoration.LineThrough else null,
                    ),
                    color = if (paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = when (subscription.billingCycle) {
                        BillingCycle.MONTHLY -> stringResource(R.string.amount_per_month_suffix)
                        BillingCycle.ANNUAL -> stringResource(
                            R.string.amount_per_year_and_month,
                            MoneyFormatter.format(subscription.monthlyAmountMinor, subscription.currencyCode),
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_subscription)) },
                onClick = { showMenu = false; onEdit() },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (paused) stringResource(R.string.action_resume)
                        else stringResource(R.string.action_pause)
                    )
                },
                leadingIcon = {
                    Icon(
                        if (paused) Icons.Default.PlayCircle
                        else Icons.Default.PauseCircle,
                        contentDescription = null,
                    )
                },
                onClick = { showMenu = false; onTogglePause() },
            )
        }
    }
}
