package com.parra.misdineros.data.backup

import androidx.room.withTransaction
import com.parra.misdineros.data.db.MisDinerosDatabase
import com.parra.misdineros.data.db.dao.CategoryDao
import com.parra.misdineros.data.db.dao.FxRateDao
import com.parra.misdineros.data.db.dao.SubscriptionDao
import com.parra.misdineros.data.mapper.*
import com.parra.misdineros.domain.repository.BackupRepository
import com.parra.misdineros.domain.repository.BackupSnapshot
import com.parra.misdineros.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val db: MisDinerosDatabase,
    private val subscriptionDao: SubscriptionDao,
    private val categoryDao: CategoryDao,
    private val fxRateDao: FxRateDao,
    private val settingsRepository: SettingsRepository,
) : BackupRepository {

    override suspend fun snapshot() = BackupSnapshot(
        subscriptions = subscriptionDao.getAll().map { it.toDomain() },
        categories = categoryDao.getAll().map { it.toDomain() },
        fxRates = fxRateDao.getAll().map { it.toDomain() },
        settings = settingsRepository.observe().first(),
    )

    override suspend fun restore(snapshot: BackupSnapshot) {
        db.withTransaction {
            subscriptionDao.deleteAll()
            categoryDao.deleteAll()
            fxRateDao.deleteAll()
            subscriptionDao.upsertAll(snapshot.subscriptions.map { it.toEntity() })
            categoryDao.upsertAll(snapshot.categories.map { it.toEntity() })
            fxRateDao.upsertAll(snapshot.fxRates.map { it.toEntity() })
        }
        settingsRepository.update(snapshot.settings)
    }
}
