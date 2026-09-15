package com.parra.misdineros.data.fx

import com.parra.misdineros.domain.model.FxRate

/** Triangulación vía EUR, la misma que usa [BundledFxRates] al sembrar. Puro, sin Android. */
object FxCrossRates {

    const val PIVOT = "EUR"

    /**
     * Pares que hay que reescribir cuando el usuario fija EUR→[quote] = [rate]: el directo, el
     * inverso y todos los cruces [quote]↔Y para cada Y con tasa EUR→Y en [eurRates]. Sin esto,
     * editar EUR→USD dejaba USD→GBP calculado con el USD antiguo.
     */
    fun derive(quote: String, rate: Double, eurRates: Map<String, Double>, now: Long): List<FxRate> {
        require(rate > 0.0) { "La tasa debe ser positiva" }
        require(quote != PIVOT) { "La tasa EUR→EUR es fija" }
        val result = mutableListOf(
            FxRate(PIVOT, quote, rate, now),
            FxRate(quote, PIVOT, 1.0 / rate, now),
        )
        eurRates.forEach { (other, eurToOther) ->
            if (other == PIVOT || other == quote || eurToOther <= 0.0) return@forEach
            result += FxRate(quote, other, eurToOther / rate, now)   // quote→EUR→other
            result += FxRate(other, quote, rate / eurToOther, now)   // other→EUR→quote
        }
        return result
    }
}
