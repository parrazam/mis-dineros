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

class CalcTopExpensiveUseCaseTest {

    private lateinit var fxRepo: FxRepository
    private lateinit var useCase: CalcTopExpensiveUseCase

    @Before
    fun setUp() {
        fxRepo = mockk()
        coEvery { fxRepo.convert(any(), any(), any()) } answers { firstArg() }
        useCase = CalcTopExpensiveUseCase(fxRepo)
    }

    private fun sub(id: String, amount: Long, paused: Boolean = false) = Subscription(
        id = id, name = "Sub $id", iconRef = "initial",
        amountMinor = amount, currencyCode = "EUR",
        billingCycle = BillingCycle.MONTHLY,
        nextRenewalDate = LocalDate.now().plusMonths(1),
        categoryId = "cat1", isPaused = paused, notifyDaysBefore = null, notes = null,
        createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `devuelve top 5 ordenadas por gasto mensual descendente`() = runTest {
        val subs = (1..8).map { sub(it.toString(), it * 100L) }
        val result = useCase(subs, "EUR", limit = 5)
        assertEquals(5, result.size)
        assertEquals(800L, result[0].monthlyAmountInTarget)
        assertEquals(400L, result[4].monthlyAmountInTarget)
    }

    @Test
    fun `excluye pausadas del ranking`() = runTest {
        val subs = listOf(
            sub("1", 5000L, paused = true),
            sub("2", 200L),
            sub("3", 100L),
        )
        val result = useCase(subs, "EUR", limit = 5)
        assertEquals(2, result.size)
        assertEquals("2", result[0].subscription.id)
    }

    @Test
    fun `lista menor que limit devuelve todas`() = runTest {
        val subs = listOf(sub("1", 100L), sub("2", 200L))
        val result = useCase(subs, "EUR", limit = 5)
        assertEquals(2, result.size)
    }
}
