package com.parra.misdineros.domain.usecase

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.parra.misdineros.domain.model.AppSettings
import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.FxRate
import com.parra.misdineros.domain.model.Subscription
import com.parra.misdineros.domain.repository.BackupRepository
import com.parra.misdineros.domain.repository.BackupSnapshot
import com.parra.misdineros.notifications.NotificationScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate

/**
 * Lo que exporta la app debe importarse íntegro, incluidas las imágenes embebidas, con y sin
 * cifrado. Cubre el cambio de android.util.Base64 a java.util.Base64.
 */
class ExportImportRoundTripTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(300) { it.toByte() }

    private fun roundTrip(password: CharArray?): BackupSnapshot {
        val iconFile = File(tmp.newFolder("src-icons"), "orig.jpg").apply { writeBytes(jpeg) }
        val original = BackupSnapshot(
            subscriptions = listOf(
                Subscription(
                    id = "s1", name = "Netflix", iconRef = "file:${iconFile.absolutePath}", amountMinor = 1299L,
                    currencyCode = "EUR", billingCycle = BillingCycle.MONTHLY,
                    nextRenewalDate = LocalDate.of(2030, 1, 31), billingAnchorDay = 31,
                    categoryId = "builtin_streaming", isPaused = true, notifyDaysBefore = 2, notes = "ñ € 日本",
                    createdAt = 1L, updatedAt = 2L,
                ),
                Subscription(
                    id = "s2", name = "Spotify", iconRef = "bundled:spotify", amountMinor = 999L,
                    currencyCode = "USD", billingCycle = BillingCycle.ANNUAL,
                    nextRenewalDate = LocalDate.of(2030, 6, 1), billingAnchorDay = 1,
                    categoryId = "builtin_musica", isPaused = false, notifyDaysBefore = null, notes = null,
                    createdAt = 3L, updatedAt = 4L,
                ),
            ),
            categories = listOf(Category("builtin_streaming", "Streaming", "tv", 0x11, true, 0), Category("builtin_musica", "Música", "music", 0x22, true, 1)),
            fxRates = listOf(FxRate("EUR", "USD", 1.1, 5L)),
            settings = AppSettings(globalCurrencyCode = "USD", notificationHour = 7, notificationMinute = 30),
        )

        val exported = ByteArrayOutputStream()
        val uri: Uri = mockk()
        val resolver = mockk<ContentResolver> {
            every { openOutputStream(uri) } returns exported
            every { openInputStream(uri) } answers { ByteArrayInputStream(exported.toByteArray()) }
        }
        val context = mockk<Context> {
            every { contentResolver } returns resolver
            every { filesDir } returns tmp.root
        }
        val repo = mockk<BackupRepository>(relaxed = true) { coEvery { snapshot() } returns original }
        val captured = slot<BackupSnapshot>()

        runTest {
            // BackupCrypto borra el CharArray tras derivar la clave: cada llamada recibe su copia,
            // igual que en la app (cada diálogo crea su propio toCharArray()).
            assertTrue(ExportDataUseCase(context, repo)(uri, password?.copyOf()).isSuccess)
            val result = ImportDataUseCase(context, repo, mockk(relaxed = true), mockk<NotificationScheduler>(relaxed = true))(uri, password?.copyOf())
            assertTrue(result.exceptionOrNull()?.toString() ?: "", result.isSuccess)
            coVerify { repo.restore(capture(captured)) }
        }
        return captured.captured
    }

    private fun check(restored: BackupSnapshot) {
        val s1 = restored.subscriptions.first { it.id == "s1" }
        val s2 = restored.subscriptions.first { it.id == "s2" }
        // La imagen se reescribe en files/icons con nombre nuevo y contenido idéntico.
        assertTrue(s1.iconRef.startsWith("file:${File(tmp.root, "icons").absolutePath}"))
        assertTrue(File(s1.iconRef.removePrefix("file:")).readBytes().contentEquals(jpeg))
        assertEquals("bundled:spotify", s2.iconRef)
        assertEquals("ñ € 日本", s1.notes)
        assertEquals(true, s1.isPaused)
        assertEquals(31, s1.billingAnchorDay)
        assertEquals(2, restored.categories.size)
        assertEquals(1.1, restored.fxRates.single().rate, 0.0)
        assertEquals("USD", restored.settings.globalCurrencyCode)
        assertEquals(7, restored.settings.notificationHour)
        assertEquals(30, restored.settings.notificationMinute)
    }

    @Test
    fun `exportar e importar en claro conserva todos los datos`() = check(roundTrip(password = null))

    @Test
    fun `exportar e importar cifrado conserva todos los datos`() = check(roundTrip(password = "contraseña-1".toCharArray()))
}
