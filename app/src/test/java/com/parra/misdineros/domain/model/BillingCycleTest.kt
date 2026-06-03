package com.parra.misdineros.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BillingCycleTest {

    @Test
    fun `mensual avanza un mes conservando el dia`() {
        val result = BillingCycle.MONTHLY.nextRenewal(LocalDate.of(2026, 6, 15), anchorDay = 15)
        assertEquals(LocalDate.of(2026, 7, 15), result)
    }

    @Test
    fun `mensual con dia 31 se ajusta en febrero pero se reancla en marzo`() {
        // 31 ene → 28 feb (febrero no bisiesto solo tiene 28 días)
        val feb = BillingCycle.MONTHLY.nextRenewal(LocalDate.of(2026, 1, 31), anchorDay = 31)
        assertEquals(LocalDate.of(2026, 2, 28), feb)
        // 28 feb → 31 mar (se reancla al día original 31, no se queda en 28)
        val mar = BillingCycle.MONTHLY.nextRenewal(feb, anchorDay = 31)
        assertEquals(LocalDate.of(2026, 3, 31), mar)
    }

    @Test
    fun `mensual con dia 31 en febrero bisiesto usa 29`() {
        val feb = BillingCycle.MONTHLY.nextRenewal(LocalDate.of(2028, 1, 31), anchorDay = 31)
        assertEquals(LocalDate.of(2028, 2, 29), feb)
    }

    @Test
    fun `anual avanza un anyo conservando mes y dia`() {
        val result = BillingCycle.ANNUAL.nextRenewal(LocalDate.of(2026, 3, 10), anchorDay = 10)
        assertEquals(LocalDate.of(2027, 3, 10), result)
    }

    @Test
    fun `anual con ancla 29 de febrero se expande en anyo bisiesto`() {
        // 29 feb 2028 (bisiesto) → 28 feb 2029 (no bisiesto, ajustado)
        val y2029 = BillingCycle.ANNUAL.nextRenewal(LocalDate.of(2028, 2, 29), anchorDay = 29)
        assertEquals(LocalDate.of(2029, 2, 28), y2029)
        // ... y al volver a un bisiesto (2032) recupera el día 29 partiendo de 28 feb 2031
        val y2032 = BillingCycle.ANNUAL.nextRenewal(LocalDate.of(2031, 2, 28), anchorDay = 29)
        assertEquals(LocalDate.of(2032, 2, 29), y2032)
    }
}
