package com.parra.misdineros.data.mapper

import com.parra.misdineros.data.db.entity.CategoryEntity
import com.parra.misdineros.domain.model.Category

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    isBuiltIn = isBuiltIn,
    sortOrder = sortOrder,
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    isBuiltIn = isBuiltIn,
    sortOrder = sortOrder,
)
