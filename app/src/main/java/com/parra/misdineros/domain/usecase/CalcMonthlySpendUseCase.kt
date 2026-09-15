package com.parra.misdineros.domain.usecase

import com.parra.misdineros.domain.model.SpendTotal
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.FxRepository
import javax.inject.Inject

class CalcMonthlySpendUseCase @Inject constructor(
    private val fxRepo: FxRepository,
) {
    suspend operator fun invoke(
        subscriptions: List<Subscription>,
        targetCurrency: String,
    ): SpendTotal = sumConverted(subscriptions, targetCurrency, fxRepo) { it.monthlyAmountMinor }
}

/**
 * Suma los importes activos convertidos a [targetCurrency]. Una suscripción sin tipo de cambio
 * no se suma y su divisa queda en [SpendTotal.excludedCurrencies].
 */
internal suspend fun sumConverted(
    subscriptions: List<Subscription>,
    targetCurrency: String,
    fxRepo: FxRepository,
    amountOf: (Subscription) -> Long,
): SpendTotal {
    var total = 0L
    val excluded = linkedSetOf<String>()
    subscriptions.filter { !it.isPaused }.forEach { sub ->
        val converted = fxRepo.convert(amountOf(sub), sub.currencyCode, targetCurrency)
        if (converted == null) excluded += sub.currencyCode else total += converted
    }
    return SpendTotal(total, excluded)
}
