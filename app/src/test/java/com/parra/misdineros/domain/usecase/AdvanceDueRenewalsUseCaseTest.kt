package com.parra.misdineros.domain.usecase

import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class AdvanceDueRenewalsUseCaseTest {

    private lateinit var repo: SubscriptionRepository
    private lateinit var useCase: AdvanceDueRenewalsUseCase

    private val today = LocalDate.of(2026, 6, 3)

    @Before
    fun setUp() {
        repo = mockk(relaxed = true)
        useCase = AdvanceDueRenewalsUseCase(repo)
    }

    private fun sub(
        id: String = "1",
        cycle: BillingCycle = BillingCycle.MONTHLY,
        nextRenewalDate: LocalDate,
        anchorDay: Int = nextRenewalDate.dayOfMonth,
        paused: Boolean = false,
    ) = Subscription(
        id = id, name = "Sub $id", iconRef = "initial",
        amountMinor = 1000L, currencyCode = "EUR",
        billingCycle = cycle,
        nextRenewalDate = nextRenewalDate,
        billingAnchorDay = anchorDay,
        categoryId = "cat1", isPaused = paused, notifyDaysBefore = null, notes = null,
        createdAt = 0L, updatedAt = 0L,
    )

    private fun givenSubs(vararg subs: Subscription) {
        coEvery { repo.observeAll() } returns flowOf(subs.toList())
    }

    @Test
    fun `mensual vencida un ciclo avanza al mes siguiente`() = runTest {
        givenSubs(sub(nextRenewalDate = LocalDate.of(2026, 5, 15)))
        useCase(today)
        coVerify(exactly = 1) { repo.updateRenewalDate("1", "2026-06-15", any()) }
    }

    @Test
    fun `mensual vencida varios ciclos avanza hasta el futuro`() = runTest {
        // Vencida desde febrero; debe saltar mar, abr, may hasta junio (>= hoy)
        givenSubs(sub(nextRenewalDate = LocalDate.of(2026, 2, 10)))
        useCase(today)
        coVerify(exactly = 1) { repo.updateRenewalDate("1", "2026-06-10", any()) }
    }

    @Test
    fun `anual vencida avanza un anyo`() = runTest {
        // 1 jul 2025 → 1 jul 2026 (>= hoy con un solo avance)
        givenSubs(sub(cycle = BillingCycle.ANNUAL, nextRenewalDate = LocalDate.of(2025, 7, 1)))
        useCase(today)
        coVerify(exactly = 1) { repo.updateRenewalDate("1", "2026-07-01", any()) }
    }

    @Test
    fun `dia 31 preserva el ancla al avanzar`() = runTest {
        // 31 ene vencida → debe llegar a 30 jun (junio tiene 30 días) reanclando desde 31
        givenSubs(sub(nextRenewalDate = LocalDate.of(2026, 1, 31), anchorDay = 31))
        useCase(today)
        coVerify(exactly = 1) { repo.updateRenewalDate("1", "2026-06-30", any()) }
    }

    @Test
    fun `renovacion hoy no se avanza`() = runTest {
        givenSubs(sub(nextRenewalDate = today))
        useCase(today)
        coVerify(exactly = 0) { repo.updateRenewalDate(any(), any(), any()) }
    }

    @Test
    fun `fecha futura no se toca`() = runTest {
        givenSubs(sub(nextRenewalDate = LocalDate.of(2026, 8, 1)))
        useCase(today)
        coVerify(exactly = 0) { repo.updateRenewalDate(any(), any(), any()) }
    }

    @Test
    fun `suscripcion pausada no se avanza`() = runTest {
        givenSubs(sub(nextRenewalDate = LocalDate.of(2026, 5, 1), paused = true))
        useCase(today)
        coVerify(exactly = 0) { repo.updateRenewalDate(any(), any(), any()) }
    }
}
