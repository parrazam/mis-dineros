package com.parra.misdineros.domain.usecase

import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllSubscriptionsUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    operator fun invoke(): Flow<List<Subscription>> = repository.observeAll()
}
