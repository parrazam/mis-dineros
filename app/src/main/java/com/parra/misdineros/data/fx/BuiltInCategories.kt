package com.parra.misdineros.data.fx

import android.graphics.Color
import com.parra.misdineros.data.db.entity.CategoryEntity

object BuiltInCategories {

    val entries = listOf(
        CategoryEntity(
            id = "builtin_streaming",
            name = "Streaming",
            iconKey = "live_tv",
            colorArgb = Color.parseColor("#E53935"),
            isBuiltIn = true,
            sortOrder = 0,
        ),
        CategoryEntity(
            id = "builtin_musica",
            name = "Música",
            iconKey = "music_note",
            colorArgb = Color.parseColor("#8E24AA"),
            isBuiltIn = true,
            sortOrder = 1,
        ),
        CategoryEntity(
            id = "builtin_productividad",
            name = "Productividad",
            iconKey = "work",
            colorArgb = Color.parseColor("#1E88E5"),
            isBuiltIn = true,
            sortOrder = 2,
        ),
        CategoryEntity(
            id = "builtin_nube",
            name = "Nube",
            iconKey = "cloud",
            colorArgb = Color.parseColor("#00ACC1"),
            isBuiltIn = true,
            sortOrder = 3,
        ),
        CategoryEntity(
            id = "builtin_gaming",
            name = "Gaming",
            iconKey = "sports_esports",
            colorArgb = Color.parseColor("#43A047"),
            isBuiltIn = true,
            sortOrder = 4,
        ),
        CategoryEntity(
            id = "builtin_noticias",
            name = "Noticias",
            iconKey = "newspaper",
            colorArgb = Color.parseColor("#F4511E"),
            isBuiltIn = true,
            sortOrder = 5,
        ),
        CategoryEntity(
            id = "builtin_fitness",
            name = "Fitness",
            iconKey = "fitness_center",
            colorArgb = Color.parseColor("#E91E63"),
            isBuiltIn = true,
            sortOrder = 6,
        ),
        CategoryEntity(
            id = "builtin_ia",
            name = "IA",
            iconKey = "smart_toy",
            colorArgb = Color.parseColor("#00897B"),
            isBuiltIn = true,
            sortOrder = 7,
        ),
        CategoryEntity(
            id = "builtin_otros",
            name = "Otros",
            iconKey = "category",
            colorArgb = Color.parseColor("#546E7A"),
            isBuiltIn = true,
            sortOrder = 8,
        ),
    )
}
