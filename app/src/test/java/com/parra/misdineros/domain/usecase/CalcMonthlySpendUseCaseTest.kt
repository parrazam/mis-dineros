package com.parra.misdineros.domain.usecase

import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.FxRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CalcMonthlySpendUseCaseTest {

    private lateinit var fxRepo: FxRepository
    private lateinit var useCase: CalcMonthlySpendUseCase

    @Before
    fun setUp() {
        fxRepo = mockk()
        useCase = CalcMonthlySpendUseCase(fxRepo)
        // Mismo código → tasa 1.0
        coEvery { fxRepo.convert(any(), any(), any()) } answers {
            firstArg<Long>()
        }
    }

    private fun sub(
        id: String,
        amountMinor: Long,
        currency: String = "EUR",
        cycle: BillingCycle = BillingCycle.MONTHLY,
        paused: Boolean = false,
    ) = Subscription(
        id = id, name = "Sub $id", iconRef = "initial",
        amountMinor = amountMinor, currencyCode = currency,
        billingCycle = cycle,
        nextRenewalDate = LocalDate.now().plusMonths(1),
        billingAnchorDay = LocalDate.now().plusMonths(1).dayOfMonth,
        categoryId = "cat1", isPaused = paused, notifyDaysBefore = null, notes = null,
        createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `suma correcta de suscripciones mensuales activas`() = runTest {
        val subs = listOf(
            sub("1", 1000L),
            sub("2", 2000L),
        )
        val result = useCase(subs, "EUR")
        assertEquals(3000L, result.totalMinor)
    }

    @Test
    fun `excluye suscripciones pausadas`() = runTest {
        val subs = listOf(
            sub("1", 1000L),
            sub("2", 2000L, paused = true),
        )
        val result = useCase(subs, "EUR")
        assertEquals(1000L, result.totalMinor)
    }

    @Test
    fun `normaliza ciclo anual dividiendo entre 12`() = runTest {
        val subs = listOf(
            sub("1", 1200L, cycle = BillingCycle.ANNUAL),
        )
        val result = useCase(subs, "EUR")
        assertEquals(100L, result.totalMinor) // 1200 / 12 = 100
    }

    @Test
    fun `lista vacia devuelve cero`() = runTest {
        val result = useCase(emptyList(), "EUR")
        assertEquals(0L, result.totalMinor)
    }

    @Test
    fun `aplica conversion de divisa`() = runTest {
        // USD → EUR: 1000 USD → 930 EUR (tasa 0.93)
        coEvery { fxRepo.convert(1000L, "USD", "EUR") } returns 930L
        val subs = listOf(sub("1", 1000L, currency = "USD"))
        val result = useCase(subs, "EUR")
        assertEquals(930L, result.totalMinor)
    }

    @Test
    fun `anual no divisible entre 12 se redondea en vez de truncar`() = runTest {
        // 10000 / 12 = 833,33 → 833 ; 10007 / 12 = 833,9 → 834
        assertEquals(833L, useCase(listOf(sub("1", 10000L, cycle = BillingCycle.ANNUAL)), "EUR").totalMinor)
        assertEquals(834L, useCase(listOf(sub("1", 10007L, cycle = BillingCycle.ANNUAL)), "EUR").totalMinor)
    }

    @Test
    fun `sin tipo de cambio la suscripcion se excluye y se senala su divisa`() = runTest {
        coEvery { fxRepo.convert(any(), "XXX", "EUR") } returns null
        val subs = listOf(sub("1", 1000L), sub("2", 5000L, currency = "XXX"), sub("3", 700L, currency = "XXX"))

        val result = useCase(subs, "EUR")

        assertEquals(1000L, result.totalMinor)
        assertEquals(setOf("XXX"), result.excludedCurrencies)
        assertTrue(result.hasExclusions)
    }

    @Test
    fun `una pausada sin tipo de cambio no genera aviso`() = runTest {
        coEvery { fxRepo.convert(any(), "XXX", "EUR") } returns null
        val result = useCase(listOf(sub("1", 1000L), sub("2", 5000L, currency = "XXX", paused = true)), "EUR")
        assertEquals(emptySet<String>(), result.excludedCurrencies)
    }
}
