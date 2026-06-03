package com.parra.misdineros.domain.model

import java.time.LocalDate
import java.time.YearMonth

enum class BillingCycle {
    MONTHLY,
    ANNUAL;

    fun toMonthlyFactor(): Double = when (this) {
        MONTHLY -> 1.0
        ANNUAL -> 1.0 / 12.0
    }

    /**
     * Calcula la siguiente fecha de renovación a partir de [from], avanzando un ciclo.
     *
     * Reancla el día de facturación a [anchorDay] (día del mes original) aplicando
     * `min(anchorDay, díasDelMes)`, de modo que un cobro el día 31 hace
     * 31 ene → 28/29 feb → 31 mar en lugar de quedarse clavado en el día 28.
     */
    fun nextRenewal(from: LocalDate, anchorDay: Int): LocalDate {
        val base = when (this) {
            MONTHLY -> from.plusMonths(1)
            ANNUAL -> from.plusYears(1)
        }
        val ym = YearMonth.from(base)
        return ym.atDay(minOf(anchorDay, ym.lengthOfMonth()))
    }
}
