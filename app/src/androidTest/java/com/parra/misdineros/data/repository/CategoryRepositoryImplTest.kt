package com.parra.misdineros.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parra.misdineros.data.db.MisDinerosDatabase
import com.parra.misdineros.data.db.entity.CategoryEntity
import com.parra.misdineros.data.db.entity.SubscriptionEntity
import com.parra.misdineros.data.icons.IconStorage
import com.parra.misdineros.domain.model.Category
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regresión: borrar una categoría en uso lanzaba `NOT NULL constraint failed` porque la FK
 * es `ON DELETE SET DEFAULT` sobre una columna sin valor por defecto.
 */
@RunWith(AndroidJUnit4::class)
class CategoryRepositoryImplTest {

    private lateinit var db: MisDinerosDatabase
    private lateinit var repo: CategoryRepositoryImpl

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, MisDinerosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = CategoryRepositoryImpl(db, db.categoryDao(), db.subscriptionDao(), IconStorage(ctx))
    }

    @After
    fun tearDown() { db.close() }

    private fun category(id: String, builtIn: Boolean = false) = CategoryEntity(
        id = id, name = id, iconKey = "category", colorArgb = 0, isBuiltIn = builtIn, sortOrder = 0,
    )

    private fun subscription(id: String, categoryId: String) = SubscriptionEntity(
        id = id, name = id, iconRef = "initial", amountMinor = 1000L, currencyCode = "EUR",
        billingCycle = "MONTHLY", nextRenewalDate = "2030-01-15", billingAnchorDay = 15,
        categoryId = categoryId, isPaused = false, notifyDaysBefore = null, notes = null,
        createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun borrarCategoriaEnUsoReasignaSusSuscripcionesAOtros() = runTest {
        db.categoryDao().upsertAll(listOf(category(Category.FALLBACK_ID, builtIn = true), category("custom")))
        db.subscriptionDao().upsertAll(listOf(subscription("s1", "custom"), subscription("s2", "custom")))

        repo.delete("custom")

        assertNull(db.categoryDao().getById("custom"))
        val subs = db.subscriptionDao().getAll()
        assertEquals(2, subs.size)
        subs.forEach { assertEquals(Category.FALLBACK_ID, it.categoryId) }
    }

    @Test
    fun borrarCategoriaNoTocaSuscripcionesDeOtrasCategorias() = runTest {
        db.categoryDao().upsertAll(listOf(category(Category.FALLBACK_ID, builtIn = true), category("a"), category("b")))
        db.subscriptionDao().upsertAll(listOf(subscription("s1", "a"), subscription("s2", "b")))

        repo.delete("a")

        assertEquals(Category.FALLBACK_ID, db.subscriptionDao().getById("s1")!!.categoryId)
        assertEquals("b", db.subscriptionDao().getById("s2")!!.categoryId)
    }

    @Test
    fun borrarCreaLaCategoriaDeReservaSiNoExiste() = runTest {
        db.categoryDao().upsertAll(listOf(category("custom")))
        db.subscriptionDao().upsert(subscription("s1", "custom"))

        repo.delete("custom")

        assertNotNull(db.categoryDao().getById(Category.FALLBACK_ID))
        assertEquals(Category.FALLBACK_ID, db.subscriptionDao().getById("s1")!!.categoryId)
    }

    @Test
    fun borrarCategoriaPredefinidaNoHaceNada() = runTest {
        db.categoryDao().upsertAll(listOf(category(Category.FALLBACK_ID, builtIn = true), category("builtin_x", builtIn = true)))
        db.subscriptionDao().upsert(subscription("s1", "builtin_x"))

        repo.delete("builtin_x")
        repo.delete(Category.FALLBACK_ID)

        assertNotNull(db.categoryDao().getById("builtin_x"))
        assertNotNull(db.categoryDao().getById(Category.FALLBACK_ID))
        assertEquals("builtin_x", db.subscriptionDao().getById("s1")!!.categoryId)
    }
}
