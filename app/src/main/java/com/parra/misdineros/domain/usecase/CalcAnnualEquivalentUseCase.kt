package com.parra.misdineros.domain.usecase

import com.parra.misdineros.domain.model.SpendTotal
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.FxRepository
import javax.inject.Inject

/**
 * Gasto anual: mensuales × 12 y anuales tal cual. Antes era el total mensual × 12, y como la
 * mensualización de una anual trunca (100,00/12 = 8,33), 100,00 al año volvía como 99,96.
 */
class CalcAnnualEquivalentUseCase @Inject constructor(
    private val fxRepo: FxRepository,
) {
    suspend operator fun invoke(
        subscriptions: List<Subscription>,
        targetCurrency: String,
    ): SpendTotal = sumConverted(subscriptions, targetCurrency, fxRepo) { it.annualAmountMinor }
}
