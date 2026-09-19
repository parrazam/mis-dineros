package com.parra.misdineros.data.repository

import androidx.room.withTransaction
import com.parra.misdineros.data.db.MisDinerosDatabase
import com.parra.misdineros.data.db.dao.CategoryDao
import com.parra.misdineros.data.db.dao.SubscriptionDao
import com.parra.misdineros.data.fx.BuiltInCategories
import com.parra.misdineros.data.mapper.toDomain
import com.parra.misdineros.data.mapper.toEntity
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.repository.CategoryRepository
import com.parra.misdineros.domain.repository.IconStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val db: MisDinerosDatabase,
    private val dao: CategoryDao,
    private val subscriptionDao: SubscriptionDao,
    private val iconStore: IconStore,
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> =
        dao.observeAll()
            .onStart { seedIfEmpty() }
            .map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Category?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getById(id: String): Category? =
        dao.getById(id)?.toDomain()

    override suspend fun upsert(category: Category) {
        val previousIcon = dao.getById(category.id)?.iconKey
        dao.upsert(category.toEntity())
        if (previousIcon != null && previousIcon != category.iconKey) iconStore.delete(previousIcon)
    }

    /**
     * Borra una categoría personalizada reasignando antes sus suscripciones a
     * [Category.FALLBACK_ID]. La FK es `ON DELETE SET DEFAULT` sobre una columna `NOT NULL`
     * sin valor por defecto, así que sin esta reasignación SQLite lanzaría
     * `NOT NULL constraint failed` al borrar una categoría en uso.
     */
    override suspend fun delete(id: String) {
        if (id == Category.FALLBACK_ID) return
        var deletedIcon: String? = null
        db.withTransaction {
            val target = dao.getById(id) ?: return@withTransaction
            if (target.isBuiltIn) return@withTransaction
            // Un backup importado podría no traer la categoría de reserva; la FK exige que exista.
            if (dao.getById(Category.FALLBACK_ID) == null) {
                dao.upsert(BuiltInCategories.entries.first { it.id == Category.FALLBACK_ID })
            }
            subscriptionDao.reassignCategory(from = id, to = Category.FALLBACK_ID, now = System.currentTimeMillis())
            dao.deleteById(id)
            deletedIcon = target.iconKey
        }
        iconStore.delete(deletedIcon)
    }

    private suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.upsertAll(BuiltInCategories.entries)
        }
    }
}
