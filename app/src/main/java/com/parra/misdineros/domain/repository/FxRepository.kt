package com.parra.misdineros.domain.repository

import com.parra.misdineros.domain.model.FxRate
import kotlinx.coroutines.flow.Flow

interface FxRepository {
    fun observeAll(): Flow<List<FxRate>>
    suspend fun getRate(base: String, quote: String): Double?
    suspend fun upsert(rate: FxRate)
    suspend fun upsertAll(rates: List<FxRate>)

    suspend fun convert(amountMinor: Long, from: String, to: String): Long
}
