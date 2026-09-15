package com.parra.misdineros.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatterTest {

    private fun eur(text: String) = MoneyFormatter.parseToMinor(text, "EUR")

    // ─── Regresión: truncamiento por aritmética en Double ─────────────────────

    @Test
    fun `importes con decimales no se truncan`() {
        assertEquals(435L, eur("4,35"))
        assertEquals(115L, eur("1.15"))
        assertEquals(29L, eur("0,29"))
        assertEquals(820L, eur("8,20"))
        assertEquals(1999L, eur("19,99"))
    }

    // ─── Formatos aceptados ────────────────────────────────────────────────────

    @Test
    fun `entero sin separador`() {
        assertEquals(1200L, eur("12"))
        assertEquals(1200L, eur("12 €"))
    }

    @Test
    fun `coma o punto como separador decimal`() {
        assertEquals(1250L, eur("12,5"))
        assertEquals(1250L, eur("12.5"))
        assertEquals(50L, eur(",5"))
        assertEquals(1200L, eur("12,"))
    }

    @Test
    fun `separador de miles con decimal distinto`() {
        assertEquals(123456L, eur("1.234,56"))
        assertEquals(123456L, eur("1,234.56"))
        assertEquals(123456789L, eur("1.234.567,89"))
    }

    @Test
    fun `separadores de miles repetidos sin decimales`() {
        assertEquals(123456700L, eur("1.234.567"))
        assertEquals(123456700L, eur("1,234,567"))
    }

    @Test
    fun `mas decimales que la divisa se redondean HALF_UP`() {
        assertEquals(436L, eur("4,355"))
        assertEquals(435L, eur("4,354"))
        assertEquals(123L, eur("1,234"))
    }

    @Test
    fun `divisa sin decimales`() {
        assertEquals(1234L, MoneyFormatter.parseToMinor("1234", "JPY"))
        assertEquals(1235L, MoneyFormatter.parseToMinor("1234,5", "JPY"))
        assertEquals(1234567L, MoneyFormatter.parseToMinor("1.234.567", "JPY"))
    }

    // ─── Entradas inválidas ────────────────────────────────────────────────────

    @Test
    fun `texto sin digitos es invalido`() {
        assertNull(eur(""))
        assertNull(eur("abc"))
        assertNull(eur(","))
        assertNull(eur("."))
    }

    @Test
    fun `grupos de miles mal formados son invalidos`() {
        assertNull(eur("12.34,5"))
        assertNull(eur("1.234.56"))
        assertNull(eur("1,2,3"))
    }

    @Test
    fun `divisa desconocida es invalida`() {
        assertNull(MoneyFormatter.parseToMinor("10", "XXX_NO"))
    }

    @Test
    fun `desbordamiento de Long es invalido`() {
        assertNull(eur("99999999999999999999"))
    }

    @Test
    fun `format y parse son inversos para el locale español`() {
        val locale = java.util.Locale("es", "ES")
        listOf(435L, 115L, 29L, 123456L, 100000000L).forEach { minor ->
            val text = MoneyFormatter.format(minor, "EUR", locale)
            assertEquals(text, minor, MoneyFormatter.parseToMinor(text, "EUR"))
        }
    }
}
