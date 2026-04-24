package com.parra.misdineros.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val colorArgb: Int,
    val isBuiltIn: Boolean,
    val sortOrder: Int,
)
