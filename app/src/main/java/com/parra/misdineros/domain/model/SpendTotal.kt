package com.parra.misdineros.domain.model

/**
 * Total de gasto en la divisa global. Las suscripciones cuya divisa no tiene tipo de cambio
 * hacia la global no se suman (antes se sumaban 1:1 sin avisar) y quedan señaladas en
 * [excludedCurrencies] para que la UI lo muestre.
 */
data class SpendTotal(
    val totalMinor: Long,
    val excludedCurrencies: Set<String> = emptySet(),
) {
    val hasExclusions: Boolean get() = excludedCurrencies.isNotEmpty()

    companion object {
        val ZERO = SpendTotal(0L)
    }
}
