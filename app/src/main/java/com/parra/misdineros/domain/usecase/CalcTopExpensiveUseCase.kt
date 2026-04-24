package com.parra.misdineros.domain.usecase

import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.FxRepository
import javax.inject.Inject

data class RankedSubscription(
    val subscription: Subscription,
    val monthlyAmountInTarget: Long,
)

class CalcTopExpensiveUseCase @Inject constructor(
    private val fxRepo: FxRepository,
) {
    suspend operator fun invoke(
        subscriptions: List<Subscription>,
        targetCurrency: String,
        limit: Int = 5,
    ): List<RankedSubscription> {
        return subscriptions
            .filter { !it.isPaused }
            .map { sub ->
                val monthly = when (sub.billingCycle) {
                    BillingCycle.MONTHLY -> sub.amountMinor
                    BillingCycle.ANNUAL -> sub.amountMinor / 12
                }
                RankedSubscription(
                    subscription = sub,
                    monthlyAmountInTarget = fxRepo.convert(monthly, sub.currencyCode, targetCurrency),
                )
            }
            .sortedByDescending { it.monthlyAmountInTarget }
            .take(limit)
    }
}
