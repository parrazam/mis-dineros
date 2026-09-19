package com.parra.misdineros.domain.usecase

import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.FxRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CalcAnnualEquivalentUseCaseTest {

    private lateinit var fxRepo: FxRepository
    private lateinit var useCase: CalcAnnualEquivalentUseCase

    @Before
    fun setUp() {
        fxRepo = mockk()
        coEvery { fxRepo.convert(any(), any(), any()) } answers { firstArg<Long>() }
        useCase = CalcAnnualEquivalentUseCase(fxRepo)
    }

    private fun sub(id: String, amount: Long, cycle: BillingCycle, currency: String = "EUR", paused: Boolean = false) = Subscription(
        id = id, name = "Sub $id", iconRef = "initial", amountMinor = amount, currencyCode = currency,
        billingCycle = cycle, nextRenewalDate = LocalDate.of(2030, 1, 1), billingAnchorDay = 1,
        categoryId = "cat1", isPaused = paused, notifyDaysBefore = null, notes = null, createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `una anual de 100 euros suma 100 euros al ano, no 99,96`() = runTest {
        val result = useCase(listOf(sub("1", 10000L, BillingCycle.ANNUAL)), "EUR")
        assertEquals(10000L, result.totalMinor)
    }

    @Test
    fun `las mensuales se multiplican por 12`() = runTest {
        val result = useCase(listOf(sub("1", 999L, BillingCycle.MONTHLY), sub("2", 5000L, BillingCycle.ANNUAL)), "EUR")
        assertEquals(999L * 12 + 5000L, result.totalMinor)
    }

    @Test
    fun `excluye pausadas y divisas sin tipo`() = runTest {
        coEvery { fxRepo.convert(any(), "XXX", "EUR") } returns null
        val result = useCase(
            listOf(sub("1", 100L, BillingCycle.MONTHLY), sub("2", 100L, BillingCycle.MONTHLY, paused = true), sub("3", 100L, BillingCycle.ANNUAL, currency = "XXX")),
            "EUR",
        )
        assertEquals(1200L, result.totalMinor)
        assertEquals(setOf("XXX"), result.excludedCurrencies)
    }
}
