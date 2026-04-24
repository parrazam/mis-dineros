package com.parra.misdineros.data.fx

import com.parra.misdineros.data.db.entity.FxRateEntity

// Tasas de cambio aproximadas con base EUR — actualizadas 2026-04-24
// El usuario puede editarlas manualmente en Ajustes > Tasas de cambio
object BundledFxRates {

    private val ratesFromEur: Map<String, Double> = mapOf(
        "USD" to 1.07,
        "GBP" to 0.86,
        "JPY" to 163.50,
        "CHF" to 0.96,
        "CAD" to 1.46,
        "AUD" to 1.65,
        "CNY" to 7.76,
        "MXN" to 19.20,
        "BRL" to 6.25,
        "SEK" to 11.02,
        "NOK" to 11.80,
        "DKK" to 7.46,
        "PLN" to 4.26,
        "CZK" to 25.20,
        "HUF" to 400.0,
        "RON" to 4.98,
        "BGN" to 1.96,
        "HRK" to 7.53,
        "RUB" to 98.50,
        "TRY" to 35.50,
        "INR" to 90.10,
        "KRW" to 1430.0,
        "SGD" to 1.45,
        "HKD" to 8.38,
        "NZD" to 1.82,
    )

    val timestamp: Long = 1745424000000L // 2026-04-24 00:00 UTC

    fun generateEntities(): List<FxRateEntity> {
        val result = mutableListOf<FxRateEntity>()

        // EUR → X
        ratesFromEur.forEach { (quote, rate) ->
            result += FxRateEntity(base = "EUR", quote = quote, rate = rate, updatedAt = timestamp)
        }

        // X → EUR (inverso)
        ratesFromEur.forEach { (base, rate) ->
            result += FxRateEntity(base = base, quote = "EUR", rate = 1.0 / rate, updatedAt = timestamp)
        }

        // EUR → EUR (trivial)
        result += FxRateEntity(base = "EUR", quote = "EUR", rate = 1.0, updatedAt = timestamp)

        // Cross rates mediante EUR: X → Y = (X→EUR) * (EUR→Y)
        val allCodes = ratesFromEur.keys.toList()
        for (base in allCodes) {
            for (quote in allCodes) {
                if (base == quote) {
                    result += FxRateEntity(base = base, quote = quote, rate = 1.0, updatedAt = timestamp)
                } else {
                    val rateToEur = 1.0 / ratesFromEur[base]!!
                    val rateFromEur = ratesFromEur[quote]!!
                    result += FxRateEntity(
                        base = base,
                        quote = quote,
                        rate = rateToEur * rateFromEur,
                        updatedAt = timestamp,
                    )
                }
            }
        }

        return result
    }
}
