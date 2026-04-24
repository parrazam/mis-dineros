package com.parra.misdineros.domain.usecase

import com.parra.misdineros.domain.repository.SubscriptionRepository
import javax.inject.Inject

class ToggleSubscriptionPauseUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(id: String) = repository.togglePause(id)
}
