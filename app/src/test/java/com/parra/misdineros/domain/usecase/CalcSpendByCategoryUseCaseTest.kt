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

class CalcSpendByCategoryUseCaseTest {

    private lateinit var fxRepo: FxRepository
    private lateinit var useCase: CalcSpendByCategoryUseCase

    @Before
    fun setUp() {
        fxRepo = mockk()
        coEvery { fxRepo.convert(any(), any(), any()) } answers { firstArg() }
        useCase = CalcSpendByCategoryUseCase(fxRepo)
    }

    private fun sub(id: String, catId: String, amount: Long, paused: Boolean = false) = Subscription(
        id = id, name = "Sub $id", iconRef = "initial",
        amountMinor = amount, currencyCode = "EUR",
        billingCycle = BillingCycle.MONTHLY,
        nextRenewalDate = LocalDate.now().plusMonths(1),
        categoryId = catId, isPaused = paused, notifyDaysBefore = null, notes = null,
        createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `agrupa y suma correctamente por categoria`() = runTest {
        val subs = listOf(
            sub("1", "streaming", 1000L),
            sub("2", "streaming", 500L),
            sub("3", "musica", 999L),
        )
        val result = useCase(subs, "EUR")
        val streaming = result.first { it.categoryId == "streaming" }
        val musica = result.first { it.categoryId == "musica" }
        assertEquals(1500L, streaming.monthlyAmountMinor)
        assertEquals(999L, musica.monthlyAmountMinor)
    }

    @Test
    fun `excluye pausadas`() = runTest {
        val subs = listOf(
            sub("1", "cat1", 1000L),
            sub("2", "cat1", 2000L, paused = true),
        )
        val result = useCase(subs, "EUR")
        assertEquals(1000L, result.first().monthlyAmountMinor)
    }

    @Test
    fun `ordena por gasto descendente`() = runTest {
        val subs = listOf(
            sub("1", "musica", 100L),
            sub("2", "streaming", 1000L),
        )
        val result = useCase(subs, "EUR")
        assertEquals("streaming", result[0].categoryId)
        assertEquals("musica", result[1].categoryId)
    }
}
