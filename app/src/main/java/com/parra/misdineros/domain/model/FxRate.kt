package com.parra.misdineros.domain.model

data class FxRate(
    val base: String,
    val quote: String,
    val rate: Double,
    val updatedAt: Long,
)
