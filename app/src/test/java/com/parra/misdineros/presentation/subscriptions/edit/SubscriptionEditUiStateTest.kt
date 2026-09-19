package com.parra.misdineros.presentation.subscriptions.edit

import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Subscription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SubscriptionEditUiStateTest {

    private val now = 1_700_000_000_000L

    private val existing = Subscription(
        id = "sub-1", name = "Netflix", iconRef = "bundled:netflix",
        amountMinor = 1299L, currencyCode = "EUR",
        billingCycle = BillingCycle.MONTHLY,
        // Anclada al 31; hoy muestra el 28 de febrero porque el mes es más corto.
        nextRenewalDate = LocalDate.of(2027, 2, 28),
        billingAnchorDay = 31,
        categoryId = "builtin_streaming", isPaused = true, notifyDaysBefore = 5, notes = "nota",
        createdAt = 1_600_000_000_000L, updatedAt = 1_650_000_000_000L,
    )

    private fun stateFrom(sub: Subscription) = SubscriptionEditUiState(
        isEditing = true,
        name = sub.name,
        iconRef = sub.iconRef,
        amountText = "12,99",
        currencyCode = sub.currencyCode,
        billingCycle = sub.billingCycle,
        nextRenewalDate = sub.nextRenewalDate,
        categoryId = sub.categoryId,
        notifyDaysBefore = sub.notifyDaysBefore,
        notes = sub.notes ?: "",
        original = sub,
    )

    // ─── Regresión: editar reactivaba una suscripción pausada ─────────────────

    @Test
    fun `editar conserva isPaused`() {
        val result = stateFrom(existing).copy(name = "Netflix 4K").toSubscription("ignored", 1299L, now)
        assertTrue(result.isPaused)
    }

    @Test
    fun `una suscripcion nueva se crea activa`() {
        val result = SubscriptionEditUiState(name = "Nueva", nextRenewalDate = LocalDate.of(2027, 3, 10))
            .toSubscription("new-id", 500L, now)
        assertFalse(result.isPaused)
        assertEquals("new-id", result.id)
        assertEquals(now, result.createdAt)
    }

    // ─── Regresión: editar sin tocar la fecha perdía el día de anclaje ───────

    @Test
    fun `editar sin cambiar la fecha conserva billingAnchorDay`() {
        val result = stateFrom(existing).copy(name = "Netflix 4K").toSubscription("ignored", 1299L, now)
        assertEquals(31, result.billingAnchorDay)
        assertEquals(LocalDate.of(2027, 2, 28), result.nextRenewalDate)
    }

    @Test
    fun `cambiar la fecha recalcula billingAnchorDay`() {
        val result = stateFrom(existing).copy(nextRenewalDate = LocalDate.of(2027, 3, 15))
            .toSubscription("ignored", 1299L, now)
        assertEquals(15, result.billingAnchorDay)
    }

    @Test
    fun `una suscripcion nueva ancla al dia de la fecha elegida`() {
        val result = SubscriptionEditUiState(name = "Nueva", nextRenewalDate = LocalDate.of(2027, 1, 31))
            .toSubscription("new-id", 500L, now)
        assertEquals(31, result.billingAnchorDay)
    }

    // ─── Resto de campos ───────────────────────────────────────────────────────

    @Test
    fun `editar conserva id y createdAt y actualiza updatedAt`() {
        val result = stateFrom(existing).toSubscription("ignored", 1299L, now)
        assertEquals("sub-1", result.id)
        assertEquals(existing.createdAt, result.createdAt)
        assertEquals(now, result.updatedAt)
    }

    @Test
    fun `nombre recortado y notas vacias como null`() {
        val result = stateFrom(existing).copy(name = "  Netflix  ", notes = "   ").toSubscription("ignored", 1299L, now)
        assertEquals("Netflix", result.name)
        assertNull(result.notes)
    }
}
