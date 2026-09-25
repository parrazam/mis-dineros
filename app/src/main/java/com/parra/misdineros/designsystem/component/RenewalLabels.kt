package com.parra.misdineros.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.parra.misdineros.R

/** Una renovación a tres días o menos se destaca en coral; más adelante, en azul suave. */
const val URGENT_RENEWAL_DAYS = 3L

/** "Hoy", "Mañana" o "en N días" ("N días" con [short], donde no cabe la preposición). */
@Composable
fun daysUntilLabel(daysUntil: Long, short: Boolean = false): String = when (daysUntil) {
    0L -> stringResource(R.string.days_until_today)
    1L -> stringResource(R.string.days_until_tomorrow)
    else -> stringResource(if (short) R.string.days_count_n else R.string.days_until_n, daysUntil.toInt())
}
