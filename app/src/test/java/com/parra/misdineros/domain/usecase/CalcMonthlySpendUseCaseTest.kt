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

class CalcMonthlySpendUseCaseTest {

    private lateinit var fxRepo: FxRepository
    private lateinit var subscriptionRepo: com.parra.misdineros.domain.repository.SubscriptionRepository
    private lateinit var useCase: CalcMonthlySpendUseCase

    @Before
    fun setUp() {
        fxRepo = mockk()
        subscriptionRepo = mockk()
        useCase = CalcMonthlySpendUseCase(subscriptionRepo, fxRepo)
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
        assertEquals(3000L, result)
    }

    @Test
    fun `excluye suscripciones pausadas`() = runTest {
        val subs = listOf(
            sub("1", 1000L),
            sub("2", 2000L, paused = true),
        )
        val result = useCase(subs, "EUR")
        assertEquals(1000L, result)
    }

    @Test
    fun `normaliza ciclo anual dividiendo entre 12`() = runTest {
        val subs = listOf(
            sub("1", 1200L, cycle = BillingCycle.ANNUAL),
        )
        val result = useCase(subs, "EUR")
        assertEquals(100L, result) // 1200 / 12 = 100
    }

    @Test
    fun `lista vacia devuelve cero`() = runTest {
        val result = useCase(emptyList(), "EUR")
        assertEquals(0L, result)
    }

    @Test
    fun `aplica conversion de divisa`() = runTest {
        // USD → EUR: 1000 USD → 930 EUR (tasa 0.93)
        coEvery { fxRepo.convert(1000L, "USD", "EUR") } returns 930L
        val subs = listOf(sub("1", 1000L, currency = "USD"))
        val result = useCase(subs, "EUR")
        assertEquals(930L, result)
    }
}
