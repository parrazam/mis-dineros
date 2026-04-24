package com.parra.misdineros.data.db.entity

import androidx.room.Entity

@Entity(tableName = "fx_rates", primaryKeys = ["base", "quote"])
data class FxRateEntity(
    val base: String,
    val quote: String,
    val rate: Double,
    val updatedAt: Long,
)
