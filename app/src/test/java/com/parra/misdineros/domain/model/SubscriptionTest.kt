package com.parra.misdineros.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SubscriptionTest {

    private fun sub(amount: Long, cycle: BillingCycle, sharedWith: Int = 1) = Subscription(
        id = "s", name = "S", iconRef = "initial", amountMinor = amount, currencyCode = "EUR",
        billingCycle = cycle, nextRenewalDate = LocalDate.of(2030, 1, 1), billingAnchorDay = 1,
        categoryId = "c", isPaused = false, notifyDaysBefore = null, notes = null, createdAt = 0L, updatedAt = 0L,
        sharedWith = sharedWith,
    )

    @Test
    fun `monthlyAmountMinor redondea la anual`() {
        assertEquals(833L, sub(10000L, BillingCycle.ANNUAL).monthlyAmountMinor)   // 833,33
        assertEquals(834L, sub(10007L, BillingCycle.ANNUAL).monthlyAmountMinor)   // 833,92
        assertEquals(1L, sub(6L, BillingCycle.ANNUAL).monthlyAmountMinor)          // 0,5 → 1 (HALF_UP)
        assertEquals(999L, sub(999L, BillingCycle.MONTHLY).monthlyAmountMinor)
    }

    @Test
    fun `annualAmountMinor es exacto`() {
        assertEquals(10000L, sub(10000L, BillingCycle.ANNUAL).annualAmountMinor)
        assertEquals(11988L, sub(999L, BillingCycle.MONTHLY).annualAmountMinor)
    }

    @Test
    fun `sin compartir la parte es el importe completo`() {
        val monthly = sub(1799L, BillingCycle.MONTHLY)
        assertEquals(false, monthly.isShared)
        assertEquals(monthly.monthlyAmountMinor, monthly.myMonthlyShareMinor)
        assertEquals(monthly.annualAmountMinor, monthly.myAnnualShareMinor)
        val annual = sub(10000L, BillingCycle.ANNUAL)
        assertEquals(annual.monthlyAmountMinor, annual.myMonthlyShareMinor)
        assertEquals(annual.annualAmountMinor, annual.myAnnualShareMinor)
    }

    @Test
    fun `mensual compartida redondea HALF_UP`() {
        val s = sub(1799L, BillingCycle.MONTHLY, sharedWith = 4)
        assertEquals(true, s.isShared)
        assertEquals(450L, s.myMonthlyShareMinor)   // 4,4975
        assertEquals(5397L, s.myAnnualShareMinor)   // 215,88 / 4 = 53,97
        assertEquals(333L, sub(1000L, BillingCycle.MONTHLY, sharedWith = 3).myMonthlyShareMinor)
        assertEquals(501L, sub(1001L, BillingCycle.MONTHLY, sharedWith = 2).myMonthlyShareMinor) // 5,005
    }

    @Test
    fun `anual compartida no reconstruye el anual desde el mensual`() {
        val s = sub(10000L, BillingCycle.ANNUAL, sharedWith = 3)
        assertEquals(3333L, s.myAnnualShareMinor)   // 33,33, no 2,78 × 12 = 33,36
        assertEquals(278L, s.myMonthlyShareMinor)   // 100 / 36 = 2,777…
    }
}
