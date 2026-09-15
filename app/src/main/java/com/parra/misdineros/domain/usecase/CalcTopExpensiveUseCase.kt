package com.parra.misdineros.domain.usecase

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
        // Una suscripción sin tipo de cambio no se puede ordenar frente a las demás: se omite.
        return subscriptions
            .filter { !it.isPaused }
            .mapNotNull { sub ->
                val converted = fxRepo.convert(sub.monthlyAmountMinor, sub.currencyCode, targetCurrency)
                    ?: return@mapNotNull null
                RankedSubscription(subscription = sub, monthlyAmountInTarget = converted)
            }
            .sortedByDescending { it.monthlyAmountInTarget }
            .take(limit)
    }
}
