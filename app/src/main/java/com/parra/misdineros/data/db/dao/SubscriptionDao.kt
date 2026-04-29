package com.parra.misdineros.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.parra.misdineros.data.db.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY name ASC")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE isPaused = 0 ORDER BY name ASC")
    fun observeActive(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SubscriptionEntity?

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<SubscriptionEntity?>

    @Upsert
    suspend fun upsert(entity: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE subscriptions SET isPaused = NOT isPaused, updatedAt = :now WHERE id = :id")
    suspend fun togglePause(id: String, now: Long)

    @Query("SELECT * FROM subscriptions WHERE isPaused = 0")
    suspend fun getAllActive(): List<SubscriptionEntity>

    @Query("UPDATE subscriptions SET nextRenewalDate = :newDate, updatedAt = :now WHERE id = :id")
    suspend fun updateRenewalDate(id: String, newDate: String, now: Long)

    @Query("SELECT * FROM subscriptions ORDER BY name ASC")
    suspend fun getAll(): List<SubscriptionEntity>

    @Upsert
    suspend fun upsertAll(entities: List<SubscriptionEntity>)

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAll()
}
