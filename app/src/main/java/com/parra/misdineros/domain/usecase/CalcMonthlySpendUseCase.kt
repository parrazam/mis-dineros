package com.parra.misdineros.domain.usecase

import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.FxRepository
import com.parra.misdineros.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CalcMonthlySpendUseCase @Inject constructor(
    private val subscriptionRepo: SubscriptionRepository,
    private val fxRepo: FxRepository,
) {
    suspend operator fun invoke(
        subscriptions: List<Subscription>,
        targetCurrency: String,
    ): Long {
        return subscriptions
            .filter { !it.isPaused }
            .sumOf { sub ->
                val normalizedMonthly = when (sub.billingCycle) {
                    BillingCycle.MONTHLY -> sub.amountMinor
                    BillingCycle.ANNUAL -> sub.amountMinor / 12
                }
                fxRepo.convert(normalizedMonthly, sub.currencyCode, targetCurrency)
            }
    }
}
