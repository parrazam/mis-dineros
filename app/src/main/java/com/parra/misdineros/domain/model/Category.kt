package com.parra.misdineros.domain.model

data class Category(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorArgb: Int,
    val isBuiltIn: Boolean,
    val sortOrder: Int,
)
