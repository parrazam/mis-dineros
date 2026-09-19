package com.parra.misdineros.domain.usecase

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

        // Sin tipo de cambio la suscripción no cuenta; el aviso lo da CalcMonthlySpendUseCase.
        return grouped.map { (categoryId, subs) ->
            val total = subs.sumOf { sub ->
                fxRepo.convert(sub.monthlyAmountMinor, sub.currencyCode, targetCurrency) ?: 0L
            }
            CategorySpend(categoryId, total)
        }.sortedByDescending { it.monthlyAmountMinor }
    }
}
