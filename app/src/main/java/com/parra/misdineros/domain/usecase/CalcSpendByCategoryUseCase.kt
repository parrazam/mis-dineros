package com.parra.misdineros.domain.usecase

import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.FxRepository
import javax.inject.Inject

data class CategorySpend(
    val categoryId: String,
    val monthlyAmountMinor: Long,
)

class CalcSpendByCategoryUseCase @Inject constructor(
    private val fxRepo: FxRepository,
) {
    suspend operator fun invoke(
        subscriptions: List<Subscription>,
        targetCurrency: String,
    ): List<CategorySpend> {
        val grouped = subscriptions
            .filter { !it.isPaused }
            .groupBy { it.categoryId }

        return grouped.map { (categoryId, subs) ->
            val total = subs.sumOf { sub ->
                val monthly = when (sub.billingCycle) {
                    BillingCycle.MONTHLY -> sub.amountMinor
                    BillingCycle.ANNUAL -> sub.amountMinor / 12
                }
                fxRepo.convert(monthly, sub.currencyCode, targetCurrency)
            }
            CategorySpend(categoryId, total)
        }.sortedByDescending { it.monthlyAmountMinor }
    }
}
