package com.parra.misdineros.domain.usecase

import javax.inject.Inject

class CalcAnnualEquivalentUseCase @Inject constructor(
    private val calcMonthly: CalcMonthlySpendUseCase,
) {
    suspend operator fun invoke(
        subscriptions: List<com.parra.misdineros.domain.model.Subscription>,
        targetCurrency: String,
    ): Long = calcMonthly(subscriptions, targetCurrency) * 12
}
