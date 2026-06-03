package com.parra.misdineros.domain.usecase

import com.parra.misdineros.core.time.DateUtils.toIsoString
import com.parra.misdineros.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Avanza la fecha de renovación de las suscripciones activas cuyo [Subscription.nextRenewalDate]
 * ha quedado en el pasado, recalculándola al siguiente ciclo (mensual/anual) hasta situarla en
 * hoy o en el futuro. Persiste solo la fecha (no toca el día de anclaje).
 *
 * Las suscripciones pausadas se excluyen, igual que en [RenewalReminderWorker].
 */
class AdvanceDueRenewalsUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()) {
        repository.observeAll().first()
            .filter { !it.isPaused && it.nextRenewalDate.isBefore(today) }
            .forEach { sub ->
                var date = sub.nextRenewalDate
                while (date.isBefore(today)) {
                    date = sub.billingCycle.nextRenewal(date, sub.billingAnchorDay)
                }
                repository.updateRenewalDate(sub.id, date.toIsoString(), System.currentTimeMillis())
            }
    }
}
