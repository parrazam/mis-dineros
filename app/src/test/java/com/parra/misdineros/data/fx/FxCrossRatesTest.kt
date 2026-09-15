package com.parra.misdineros.data.fx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FxCrossRatesTest {

    private val eurRates = mapOf("EUR" to 1.0, "USD" to 1.10, "GBP" to 0.85, "JPY" to 160.0)

    private fun rate(list: List<com.parra.misdineros.domain.model.FxRate>, base: String, quote: String) =
        list.single { it.base == base && it.quote == quote }.rate

    @Test
    fun `reescribe directo, inverso y todos los cruces de la divisa editada`() {
        val result = FxCrossRates.derive("USD", 1.25, eurRates, now = 42L)

        // directo e inverso
        assertEquals(1.25, rate(result, "EUR", "USD"), 1e-12)
        assertEquals(0.8, rate(result, "USD", "EUR"), 1e-12)
        // cruces por EUR: USD→GBP = 0.85 / 1.25 ; GBP→USD = 1.25 / 0.85
        assertEquals(0.68, rate(result, "USD", "GBP"), 1e-12)
        assertEquals(1.25 / 0.85, rate(result, "GBP", "USD"), 1e-12)
        assertEquals(160.0 / 1.25, rate(result, "USD", "JPY"), 1e-12)
        assertEquals(1.25 / 160.0, rate(result, "JPY", "USD"), 1e-12)
        // 2 + 2 por cada otra divisa distinta de EUR y USD
        assertEquals(2 + 2 * 2, result.size)
        assert(result.all { it.updatedAt == 42L })
    }

    @Test
    fun `no genera pares EUR-EUR ni de la divisa consigo misma`() {
        val result = FxCrossRates.derive("USD", 1.25, eurRates, 0L)
        assert(result.none { it.base == it.quote })
    }

    @Test
    fun `tasa no positiva o EUR como quote se rechazan`() {
        assertThrows(IllegalArgumentException::class.java) { FxCrossRates.derive("USD", 0.0, eurRates, 0L) }
        assertThrows(IllegalArgumentException::class.java) { FxCrossRates.derive("USD", -1.0, eurRates, 0L) }
        assertThrows(IllegalArgumentException::class.java) { FxCrossRates.derive("EUR", 1.0, eurRates, 0L) }
    }
}
