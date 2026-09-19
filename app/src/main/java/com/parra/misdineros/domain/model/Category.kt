package com.parra.misdineros.domain.model

data class Category(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorArgb: Int,
    val isBuiltIn: Boolean,
    val sortOrder: Int,
) {
    companion object {
        /**
         * Categoría predefinida que recibe las suscripciones huérfanas cuando se borra la suya.
         * La FK de `subscriptions.categoryId` es `NOT NULL`, así que no puede quedar sin valor.
         */
        const val FALLBACK_ID = "builtin_otros"
    }
}
