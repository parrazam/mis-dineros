package com.parra.misdineros.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parra.misdineros.data.db.MisDinerosDatabase
import com.parra.misdineros.data.db.entity.CategoryEntity
import com.parra.misdineros.data.db.entity.FxRateEntity
import com.parra.misdineros.data.db.entity.SubscriptionEntity
import com.parra.misdineros.domain.model.AppSettings
import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.FxRate
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.BackupSnapshot
import com.parra.misdineros.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class BackupRestoreTest {

    private lateinit var db: MisDinerosDatabase
    private lateinit var repo: BackupRepositoryImpl

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, MisDinerosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = BackupRepositoryImpl(
            db = db,
            subscriptionDao = db.subscriptionDao(),
            categoryDao = db.categoryDao(),
            fxRateDao = db.fxRateDao(),
            settingsRepository = FakeSettingsRepository(),
        )
    }

    @After
    fun tearDown() { db.close() }

    // ─── Bug regression: FK ordering ──────────────────────────────────────────

    @Test
    fun `restore inserta categorias antes que suscripciones, sin FK violation`() = runTest {
        val snapshot = BackupSnapshot(
            subscriptions = listOf(
                Subscription(
                    id = "sub1", name = "Netflix", iconRef = "bundled:netflix",
                    amountMinor = 1299L, currencyCode = "EUR",
                    billingCycle = BillingCycle.MONTHLY,
                    nextRenewalDate = LocalDate.of(2026, 5, 27),
                    categoryId = "cat1", isPaused = false,
                    notifyDaysBefore = null, notes = null, createdAt = 0L, updatedAt = 0L,
                )
            ),
            categories = listOf(Category("cat1", "Streaming", "play_circle", 0xFFE53935.toInt(), false, 1)),
            fxRates = emptyList(),
            settings = AppSettings(),
        )

        repo.restore(snapshot) // debe completar sin SQLiteConstraintException

        assertEquals(1, db.categoryDao().getAll().size)
        assertEquals(1, db.subscriptionDao().getAll().size)
        assertEquals("cat1", db.subscriptionDao().getAll()[0].categoryId)
    }

    // ─── Round-trip ───────────────────────────────────────────────────────────

    @Test
    fun `snapshot y restore round-trip preserva suscripciones y categorias`() = runTest {
        db.categoryDao().upsert(
            CategoryEntity("cat1", "Streaming", "play_circle", 0xFFE53935.toInt(), false, 1)
        )
        db.subscriptionDao().upsert(
            SubscriptionEntity(
                id = "sub1", name = "Netflix", iconRef = "bundled:netflix",
                amountMinor = 1299L, currencyCode = "EUR", billingCycle = "MONTHLY",
                nextRenewalDate = "2026-05-27", categoryId = "cat1",
                isPaused = false, notifyDaysBefore = 3, notes = "nota",
                createdAt = 0L, updatedAt = 0L,
            )
        )

        val snapshot = repo.snapshot()
        assertEquals(1, snapshot.subscriptions.size)
        assertEquals(1, snapshot.categories.size)

        repo.restore(snapshot)

        val subs = db.subscriptionDao().getAll()
        val cats = db.categoryDao().getAll()
        assertEquals(1, subs.size)
        assertEquals("sub1", subs[0].id)
        assertEquals(1299L, subs[0].amountMinor)
        assertEquals("cat1", subs[0].categoryId)
        assertEquals(1, cats.size)
        assertEquals("cat1", cats[0].id)
    }

    @Test
    fun `snapshot y restore round-trip preserva tasas de cambio`() = runTest {
        db.fxRateDao().upsertAll(
            listOf(
                FxRateEntity("EUR", "USD", 1.08, 0L),
                FxRateEntity("EUR", "GBP", 0.85, 0L),
            )
        )

        val snapshot = repo.snapshot()
        repo.restore(snapshot)

        val rates = db.fxRateDao().getAll()
        assertEquals(2, rates.size)
        assertTrue(rates.any { it.base == "EUR" && it.quote == "USD" })
        assertTrue(rates.any { it.base == "EUR" && it.quote == "GBP" })
    }

    // ─── Sobreescritura ───────────────────────────────────────────────────────

    @Test
    fun `restore sobreescribe datos existentes completamente`() = runTest {
        db.categoryDao().upsert(
            CategoryEntity("old_cat", "Old", "settings", 0xFF000000.toInt(), false, 1)
        )

        val snapshot = BackupSnapshot(
            subscriptions = emptyList(),
            categories = listOf(Category("new_cat", "New", "home", 0xFFFFFFFF.toInt(), false, 1)),
            fxRates = listOf(FxRate("EUR", "USD", 1.08, 0L)),
            settings = AppSettings(),
        )

        repo.restore(snapshot)

        val cats = db.categoryDao().getAll()
        assertEquals(1, cats.size)
        assertEquals("new_cat", cats[0].id)
        assertEquals(1, db.fxRateDao().getAll().size)
    }

    @Test
    fun `restore con snapshot vacio deja todas las tablas vacias`() = runTest {
        db.categoryDao().upsert(
            CategoryEntity("cat1", "X", "home", 0xFF000000.toInt(), false, 1)
        )

        repo.restore(
            BackupSnapshot(emptyList(), emptyList(), emptyList(), AppSettings())
        )

        assertEquals(0, db.subscriptionDao().getAll().size)
        assertEquals(0, db.categoryDao().getAll().size)
        assertEquals(0, db.fxRateDao().getAll().size)
    }

    // ─── Multiples suscripciones y categorias ─────────────────────────────────

    @Test
    fun `restore multiples suscripciones referenciando distintas categorias`() = runTest {
        val snapshot = BackupSnapshot(
            subscriptions = listOf(
                Subscription(
                    id = "s1", name = "Netflix", iconRef = "initial",
                    amountMinor = 1299L, currencyCode = "EUR",
                    billingCycle = BillingCycle.MONTHLY,
                    nextRenewalDate = LocalDate.of(2026, 6, 1),
                    categoryId = "cat_stream", isPaused = false,
                    notifyDaysBefore = null, notes = null, createdAt = 0L, updatedAt = 0L,
                ),
                Subscription(
                    id = "s2", name = "Spotify", iconRef = "initial",
                    amountMinor = 999L, currencyCode = "EUR",
                    billingCycle = BillingCycle.MONTHLY,
                    nextRenewalDate = LocalDate.of(2026, 6, 15),
                    categoryId = "cat_music", isPaused = true,
                    notifyDaysBefore = 1, notes = null, createdAt = 0L, updatedAt = 0L,
                ),
            ),
            categories = listOf(
                Category("cat_stream", "Streaming", "play_circle", 0xFFE53935.toInt(), false, 1),
                Category("cat_music", "Música", "music_note", 0xFF8E24AA.toInt(), false, 2),
            ),
            fxRates = emptyList(),
            settings = AppSettings(),
        )

        repo.restore(snapshot)

        assertEquals(2, db.subscriptionDao().getAll().size)
        assertEquals(2, db.categoryDao().getAll().size)
    }
}

// ─── Fake ─────────────────────────────────────────────────────────────────────

private class FakeSettingsRepository : SettingsRepository {
    private var current = AppSettings()
    override fun observe(): Flow<AppSettings> = flowOf(current)
    override suspend fun update(settings: AppSettings) { current = settings }
}
