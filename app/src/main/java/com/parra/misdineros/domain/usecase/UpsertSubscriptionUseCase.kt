package com.parra.misdineros.domain.usecase

import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.SubscriptionRepository
import javax.inject.Inject

class UpsertSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(subscription: Subscription) = repository.upsert(subscription)
}
