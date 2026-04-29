package com.parra.misdineros.domain.repository

import com.parra.misdineros.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    fun observeById(id: String): Flow<Category?>
    suspend fun getById(id: String): Category?
    suspend fun upsert(category: Category)
    suspend fun delete(id: String)
}
