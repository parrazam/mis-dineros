package com.parra.misdineros.data.repository

import com.parra.misdineros.data.db.dao.FxRateDao
import com.parra.misdineros.data.fx.BundledFxRates
import com.parra.misdineros.data.mapper.toDomain
import com.parra.misdineros.data.mapper.toEntity
import com.parra.misdineros.domain.model.FxRate
import com.parra.misdineros.domain.repository.FxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import kotlin.math.roundToLong

class FxRepositoryImpl @Inject constructor(
    private val dao: FxRateDao,
) : FxRepository {

    override fun observeAll(): Flow<List<FxRate>> =
        dao.observeAll()
            .onStart { seedIfEmpty() }
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getRate(base: String, quote: String): Double? {
        if (base == quote) return 1.0
        seedIfEmpty()
        return dao.getRate(base, quote)
    }

    override suspend fun upsert(rate: FxRate) {
        dao.upsert(rate.toEntity())
    }

    override suspend fun upsertAll(rates: List<FxRate>) {
        dao.upsertAll(rates.map { it.toEntity() })
    }

    override suspend fun convert(amountMinor: Long, from: String, to: String): Long {
        if (from == to) return amountMinor
        val rate = getRate(from, to) ?: 1.0
        return (amountMinor * rate).roundToLong()
    }

    private suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.upsertAll(BundledFxRates.generateEntities())
        }
    }
}
