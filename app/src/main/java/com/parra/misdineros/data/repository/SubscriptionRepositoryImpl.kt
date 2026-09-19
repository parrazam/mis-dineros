package com.parra.misdineros.data.repository

import com.parra.misdineros.data.db.dao.SubscriptionDao
import com.parra.misdineros.data.mapper.toDomain
import com.parra.misdineros.data.mapper.toEntity
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.IconStore
import com.parra.misdineros.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SubscriptionRepositoryImpl @Inject constructor(
    private val dao: SubscriptionDao,
    private val iconStore: IconStore,
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
        val previousIcon = dao.getById(subscription.id)?.iconRef
        dao.upsert(subscription.toEntity())
        // El icono anterior deja de estar referenciado: se borra para no acumular ficheros.
        if (previousIcon != null && previousIcon != subscription.iconRef) iconStore.delete(previousIcon)
    }

    override suspend fun delete(id: String) {
        val icon = dao.getById(id)?.iconRef
        dao.deleteById(id)
        iconStore.delete(icon)
    }

    override suspend fun togglePause(id: String) {
        dao.togglePause(id, System.currentTimeMillis())
    }

    override suspend fun updateRenewalDate(id: String, newDate: String, now: Long) {
        dao.updateRenewalDate(id, newDate, now)
    }
}
