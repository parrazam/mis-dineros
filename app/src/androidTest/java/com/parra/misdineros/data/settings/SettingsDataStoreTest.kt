package com.parra.misdineros.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parra.misdineros.designsystem.theme.AppTheme
import com.parra.misdineros.domain.model.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Regresión: un fichero de ajustes corrupto hacía que `DataStore.data` lanzara
 * `CorruptionException` en cada lectura y la app cerrara en cada arranque.
 */
@RunWith(AndroidJUnit4::class)
class SettingsDataStoreTest {

    private lateinit var context: Context
    private lateinit var dir: File
    private lateinit var file: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dir = File(context.cacheDir, "settings-test-${System.nanoTime()}").also { it.mkdirs() }
        file = File(dir, "settings.preferences_pb")
    }

    @After
    fun tearDown() { dir.deleteRecursively() }

    private fun repo() = SettingsDataStore(context, SettingsDataStore.create(file))

    @Test
    fun ficheroCorruptoDevuelveValoresPorDefectoEnVezDeLanzar() = runTest {
        file.writeBytes(byteArrayOf(0x7f, 0x00, 0x13, 0x37, 0x42, 0x42, 0x42, 0x42, 0x42))

        val settings = repo().observe().first()

        assertEquals(AppSettings(), settings)
    }

    @Test
    fun trasRecuperarseDeCorrupcionSePuedeEscribirYLeer() = runTest {
        file.writeBytes(ByteArray(64) { 0x42 })
        val repo = repo()
        repo.observe().first()

        repo.update(AppSettings(globalCurrencyCode = "USD", notificationHour = 21, appTheme = AppTheme.DARK))

        val reloaded = repo.observe().first()
        assertEquals("USD", reloaded.globalCurrencyCode)
        assertEquals(21, reloaded.notificationHour)
        assertEquals(AppTheme.DARK, reloaded.appTheme)
    }

    @Test
    fun ficheroValidoSePersisteEnDisco() = runTest {
        // DataStore prohíbe dos instancias sobre el mismo fichero, así que la lectura desde
        // disco se comprueba por el contenido del fichero, no con una segunda instancia.
        val repo = repo()
        repo.update(AppSettings(globalCurrencyCode = "GBP", notificationsEnabled = false))

        val settings = repo.observe().first()

        assertEquals("GBP", settings.globalCurrencyCode)
        assertEquals(false, settings.notificationsEnabled)
        assertTrue(file.exists() && file.length() > 0)
    }
}
