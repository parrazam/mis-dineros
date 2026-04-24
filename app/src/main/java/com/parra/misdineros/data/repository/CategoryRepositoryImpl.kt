package com.parra.misdineros.data.repository

import com.parra.misdineros.data.db.dao.CategoryDao
import com.parra.misdineros.data.fx.BuiltInCategories
import com.parra.misdineros.data.mapper.toDomain
import com.parra.misdineros.data.mapper.toEntity
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao,
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> =
        dao.observeAll()
            .onStart { seedIfEmpty() }
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Category? =
        dao.getById(id)?.toDomain()

    override suspend fun upsert(category: Category) {
        dao.upsert(category.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    private suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.upsertAll(BuiltInCategories.entries)
        }
    }
}
