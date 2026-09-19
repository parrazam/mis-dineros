package com.parra.misdineros.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SubscriptionTest {

    private fun sub(amount: Long, cycle: BillingCycle) = Subscription(
        id = "s", name = "S", iconRef = "initial", amountMinor = amount, currencyCode = "EUR",
        billingCycle = cycle, nextRenewalDate = LocalDate.of(2030, 1, 1), billingAnchorDay = 1,
        categoryId = "c", isPaused = false, notifyDaysBefore = null, notes = null, createdAt = 0L, updatedAt = 0L,
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
}
