package com.parra.misdineros.data.repository

import com.parra.misdineros.data.db.dao.FxRateDao
import com.parra.misdineros.data.fx.BundledFxRates
import com.parra.misdineros.data.fx.FxCrossRates
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

    override suspend fun resetToDefaults() {
        dao.upsertAll(BundledFxRates.generateEntities())
    }

    override suspend fun setRateFromEur(quote: String, rate: Double) {
        seedIfEmpty()
        val eurRates = dao.getAll()
            .filter { it.base == FxCrossRates.PIVOT }
            .associate { it.quote to it.rate }
        dao.upsertAll(FxCrossRates.derive(quote, rate, eurRates, System.currentTimeMillis()).map { it.toEntity() })
    }

    /**
     * `null` cuando falta el par: antes se devolvía el importe sin convertir (tasa 1.0), lo que
     * sumaba dólares como euros sin que nada lo indicara.
     */
    override suspend fun convert(amountMinor: Long, from: String, to: String): Long? {
        if (from == to) return amountMinor
        val rate = getRate(from, to) ?: return null
        return (amountMinor * rate).roundToLong()
    }

    private suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            dao.upsertAll(BundledFxRates.generateEntities())
        }
    }
}
