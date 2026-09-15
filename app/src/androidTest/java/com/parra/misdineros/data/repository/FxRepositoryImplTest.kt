package com.parra.misdineros.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parra.misdineros.data.db.MisDinerosDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FxRepositoryImplTest {

    private lateinit var db: MisDinerosDatabase
    private lateinit var repo: FxRepositoryImpl

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, MisDinerosDatabase::class.java).allowMainThreadQueries().build()
        repo = FxRepositoryImpl(db.fxRateDao())
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun editarEurUsdReescribeLosCrucesQuePasanPorUsd() = runTest {
        repo.resetToDefaults()
        val eurGbp = repo.getRate("EUR", "GBP")!!

        repo.setRateFromEur("USD", 2.0)

        assertEquals(2.0, repo.getRate("EUR", "USD")!!, 1e-12)
        assertEquals(0.5, repo.getRate("USD", "EUR")!!, 1e-12)
        assertEquals(eurGbp / 2.0, repo.getRate("USD", "GBP")!!, 1e-12)
        assertEquals(2.0 / eurGbp, repo.getRate("GBP", "USD")!!, 1e-12)
        // Un par ajeno a USD no cambia.
        assertEquals(eurGbp, repo.getRate("EUR", "GBP")!!, 1e-12)
    }

    @Test
    fun convertDevuelveNullSinTipoDeCambio() = runTest {
        repo.resetToDefaults()
        assertNull(repo.convert(1000L, "XXX", "EUR"))
        assertEquals(1000L, repo.convert(1000L, "EUR", "EUR"))
        repo.setRateFromEur("USD", 2.0)
        assertEquals(2000L, repo.convert(1000L, "EUR", "USD"))
    }
}
