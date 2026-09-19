package com.parra.misdineros.data.repository

import com.parra.misdineros.data.db.dao.SubscriptionDao
import com.parra.misdineros.data.db.entity.SubscriptionEntity
import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.IconStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class SubscriptionRepositoryImplTest {

    private lateinit var dao: SubscriptionDao
    private lateinit var iconStore: IconStore
    private lateinit var repo: SubscriptionRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        iconStore = mockk(relaxed = true)
        repo = SubscriptionRepositoryImpl(dao, iconStore)
    }

    private fun entity(iconRef: String) = SubscriptionEntity(
        id = "s1", name = "Sub", iconRef = iconRef, amountMinor = 100L, currencyCode = "EUR",
        billingCycle = "MONTHLY", nextRenewalDate = "2030-01-15", billingAnchorDay = 15,
        categoryId = "builtin_otros", isPaused = false, notifyDaysBefore = null, notes = null,
        createdAt = 0L, updatedAt = 0L,
    )

    private fun domain(iconRef: String) = Subscription(
        id = "s1", name = "Sub", iconRef = iconRef, amountMinor = 100L, currencyCode = "EUR",
        billingCycle = BillingCycle.MONTHLY, nextRenewalDate = LocalDate.of(2030, 1, 15), billingAnchorDay = 15,
        categoryId = "builtin_otros", isPaused = false, notifyDaysBefore = null, notes = null,
        createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `borrar una suscripcion borra su icono`() = runTest {
        coEvery { dao.getById("s1") } returns entity("file:/data/icons/a.jpg")

        repo.delete("s1")

        coVerify(exactly = 1) { dao.deleteById("s1") }
        coVerify(exactly = 1) { iconStore.delete("file:/data/icons/a.jpg") }
    }

    @Test
    fun `sustituir el icono al editar borra el anterior`() = runTest {
        coEvery { dao.getById("s1") } returns entity("file:/data/icons/old.jpg")

        repo.upsert(domain("file:/data/icons/new.jpg"))

        coVerify(exactly = 1) { iconStore.delete("file:/data/icons/old.jpg") }
    }

    @Test
    fun `editar sin cambiar el icono no borra nada`() = runTest {
        coEvery { dao.getById("s1") } returns entity("file:/data/icons/same.jpg")

        repo.upsert(domain("file:/data/icons/same.jpg"))

        coVerify(exactly = 0) { iconStore.delete(any()) }
    }

    @Test
    fun `crear una suscripcion nueva no borra nada`() = runTest {
        coEvery { dao.getById("s1") } returns null

        repo.upsert(domain("file:/data/icons/new.jpg"))

        coVerify(exactly = 0) { iconStore.delete(any()) }
    }
}
