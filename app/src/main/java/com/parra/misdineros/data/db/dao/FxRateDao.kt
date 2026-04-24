package com.parra.misdineros.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.parra.misdineros.data.db.entity.FxRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FxRateDao {

    @Query("SELECT * FROM fx_rates ORDER BY base ASC, quote ASC")
    fun observeAll(): Flow<List<FxRateEntity>>

    @Query("SELECT rate FROM fx_rates WHERE base = :base AND quote = :quote LIMIT 1")
    suspend fun getRate(base: String, quote: String): Double?

    @Upsert
    suspend fun upsert(entity: FxRateEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FxRateEntity>)

    @Query("SELECT COUNT(*) FROM fx_rates")
    suspend fun count(): Int
}
