package com.parra.misdineros.domain.usecase

import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.CategoryRepository
import com.parra.misdineros.domain.repository.IconStore
import com.parra.misdineros.domain.repository.SubscriptionRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class PruneOrphanIconsUseCaseTest {

    private fun sub(id: String, iconRef: String) = Subscription(
        id = id, name = id, iconRef = iconRef, amountMinor = 1L, currencyCode = "EUR",
        billingCycle = BillingCycle.MONTHLY, nextRenewalDate = LocalDate.of(2030, 1, 1), billingAnchorDay = 1,
        categoryId = "c", isPaused = false, notifyDaysBefore = null, notes = null, createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `pasa al almacen todas las referencias de suscripciones y categorias`() = runTest {
        val subs = mockk<SubscriptionRepository> {
            every { observeAll() } returns flowOf(listOf(sub("a", "file:/i/a.jpg"), sub("b", "bundled:netflix")))
        }
        val cats = mockk<CategoryRepository> {
            every { observeAll() } returns flowOf(listOf(Category("c", "C", "file:/c/c.jpg", 0, false, 0), Category("d", "D", "emoji:x", 0, false, 1)))
        }
        val store = mockk<IconStore>(relaxed = true)

        PruneOrphanIconsUseCase(subs, cats, store)()

        coVerify(exactly = 1) { store.pruneOrphans(setOf("file:/i/a.jpg", "bundled:netflix", "file:/c/c.jpg", "emoji:x")) }
    }
}
