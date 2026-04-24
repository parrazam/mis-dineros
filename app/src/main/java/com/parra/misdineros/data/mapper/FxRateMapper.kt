package com.parra.misdineros.data.mapper

import com.parra.misdineros.data.db.entity.FxRateEntity
import com.parra.misdineros.domain.model.FxRate

fun FxRateEntity.toDomain() = FxRate(
    base = base,
    quote = quote,
    rate = rate,
    updatedAt = updatedAt,
)

fun FxRate.toEntity() = FxRateEntity(
    base = base,
    quote = quote,
    rate = rate,
    updatedAt = updatedAt,
)
