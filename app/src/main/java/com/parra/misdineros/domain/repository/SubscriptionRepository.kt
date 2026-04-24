package com.parra.misdineros.domain.repository

import com.parra.misdineros.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeAll(): Flow<List<Subscription>>
    fun observeActive(): Flow<List<Subscription>>
    suspend fun getById(id: String): Subscription?
    suspend fun upsert(subscription: Subscription)
    suspend fun delete(id: String)
    suspend fun togglePause(id: String)
}
