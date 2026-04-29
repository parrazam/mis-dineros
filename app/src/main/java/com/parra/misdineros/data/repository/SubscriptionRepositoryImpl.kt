package com.parra.misdineros.data.repository

import com.parra.misdineros.data.db.dao.SubscriptionDao
import com.parra.misdineros.data.mapper.toDomain
import com.parra.misdineros.data.mapper.toEntity
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SubscriptionRepositoryImpl @Inject constructor(
    private val dao: SubscriptionDao,
) : SubscriptionRepository {

    override fun observeAll(): Flow<List<Subscription>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActive(): Flow<List<Subscription>> =
        dao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Subscription?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getById(id: String): Subscription? =
        dao.getById(id)?.toDomain()

    override suspend fun upsert(subscription: Subscription) {
        dao.upsert(subscription.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun togglePause(id: String) {
        dao.togglePause(id, System.currentTimeMillis())
    }
}
