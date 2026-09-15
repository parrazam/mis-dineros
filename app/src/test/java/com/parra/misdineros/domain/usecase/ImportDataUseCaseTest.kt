package com.parra.misdineros.domain.usecase

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.parra.misdineros.data.backup.BackupCrypto
import com.parra.misdineros.data.backup.BackupAssets
import com.parra.misdineros.data.backup.BackupJson
import com.parra.misdineros.data.backup.SettingsDto
import com.parra.misdineros.data.backup.SubscriptionDto
import com.parra.misdineros.domain.repository.BackupRepository
import com.parra.misdineros.domain.repository.BackupSnapshot
import com.parra.misdineros.notifications.NotificationScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.Base64

class ImportDataUseCaseTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var resolver: ContentResolver
    private lateinit var backupRepository: BackupRepository
    private lateinit var advanceDueRenewals: AdvanceDueRenewalsUseCase
    private lateinit var scheduler: NotificationScheduler
    private lateinit var useCase: ImportDataUseCase
    private val uri: Uri = mockk()

    @Before
    fun setUp() {
        resolver = mockk()
        context = mockk {
            every { contentResolver } returns resolver
            every { filesDir } returns tmp.root
        }
        backupRepository = mockk(relaxed = true)
        advanceDueRenewals = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        useCase = ImportDataUseCase(context, backupRepository, advanceDueRenewals, scheduler)
    }

    private fun givenFile(bytes: ByteArray) {
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(bytes)
    }

    private fun backup(
        settings: SettingsDto = importedSettings,
        subscriptions: List<SubscriptionDto> = emptyList(),
        assets: Map<String, String> = emptyMap(),
    ) = Json.encodeToString(
        BackupJson(
            exportedAt = "2026-09-15T10:00:00Z",
            subscriptions = subscriptions,
            categories = emptyList(),
            fxRates = emptyList(),
            settings = settings,
            assets = assets,
        )
    )

    private fun sub(iconRef: String) = SubscriptionDto(
        id = "s-$iconRef", name = "Sub", iconRef = iconRef, amountMinor = 100L, currencyCode = "EUR",
        billingCycle = "MONTHLY", nextRenewalDate = "2030-01-15", categoryId = "builtin_otros",
        isPaused = false, createdAt = 0L, updatedAt = 0L,
    )

    private val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3)
    private fun b64(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
    private val iconsDir get() = File(tmp.root, "icons")

    private suspend fun importAndCaptureSubs(json: String): List<com.parra.misdineros.domain.model.Subscription> {
        givenFile(BackupCrypto.wrapPlain(json))
        val snapshot = slot<BackupSnapshot>()
        val result = useCase(uri)
        assertTrue(result.exceptionOrNull()?.toString() ?: "", result.isSuccess)
        coVerify { backupRepository.restore(capture(snapshot)) }
        return snapshot.captured.subscriptions
    }

    private val importedSettings = SettingsDto(
        globalCurrencyCode = "USD",
        notificationsEnabled = true,
        notificationHour = 20,
        notificationMinute = 45,
        defaultNotifyDaysBefore = 2,
        monthlySummaryEnabled = false,
        appTheme = "DARK",
    )

    // ─── Regresión: importar no reprogramaba las notificaciones ─────────────

    @Test
    fun `importar reprograma las notificaciones con los ajustes importados`() = runTest {
        givenFile(BackupCrypto.wrapPlain(backup(importedSettings)))

        val result = useCase(uri)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { scheduler.schedule(hour = 20, minute = 45, enabled = true) }
    }

    @Test
    fun `importar con notificaciones desactivadas las cancela`() = runTest {
        givenFile(BackupCrypto.wrapPlain(backup(importedSettings.copy(notificationsEnabled = false))))

        useCase(uri)

        verify(exactly = 1) { scheduler.schedule(hour = 20, minute = 45, enabled = false) }
    }

    @Test
    fun `restaura, avanza renovaciones y reprograma en ese orden`() = runTest {
        givenFile(backup(importedSettings).toByteArray())  // JSON legacy sin cabecera
        val snapshot = slot<BackupSnapshot>()

        useCase(uri)

        coVerify(exactly = 1) { backupRepository.restore(capture(snapshot)) }
        coVerify(exactly = 1) { advanceDueRenewals(any()) }
        assertEquals("USD", snapshot.captured.settings.globalCurrencyCode)
        assertEquals(20, snapshot.captured.settings.notificationHour)
    }

    @Test
    fun `si la restauracion falla no se reprograma nada`() = runTest {
        givenFile(BackupCrypto.wrapPlain(backup(importedSettings)))
        coEvery { backupRepository.restore(any()) } throws IllegalStateException("boom")

        val result = useCase(uri)

        assertTrue(result.isFailure)
        verify(exactly = 0) { scheduler.schedule(any(), any(), any()) }
    }

    @Test
    fun `fichero cifrado sin contrasena falla sin tocar los datos`() = runTest {
        givenFile(BackupCrypto.encrypt(backup(importedSettings), "secreta".toCharArray()))

        val result = useCase(uri)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { backupRepository.restore(any()) }
        verify(exactly = 0) { scheduler.schedule(any(), any(), any()) }
    }

    // ─── Fase 3: saneado de assets e iconRef ───────────────────────────────────

    @Test
    fun `un nombre de asset con path traversal no escribe fuera de icons`() = runTest {
        val hostile = "../../databases/mis_dineros.db"
        val subs = importAndCaptureSubs(
            backup(subscriptions = listOf(sub("asset:$hostile")), assets = mapOf(hostile to b64(jpegBytes)))
        )

        assertFalse(File(tmp.root, "databases/mis_dineros.db").exists())
        val written = iconsDir.listFiles()!!.toList()
        assertEquals(1, written.size)
        assertTrue(written.single().name, written.single().name.matches(Regex("[0-9a-f-]{36}[.]jpg")))
        assertEquals("file:${written.single().absolutePath}", subs.single().iconRef)
    }

    @Test
    fun `la extension sale de la firma binaria y no del nombre`() = runTest {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0)
        importAndCaptureSubs(backup(assets = mapOf("foto.jpg" to b64(png))))

        assertTrue(iconsDir.listFiles()!!.single().name.endsWith(".png"))
    }

    @Test
    fun `un asset que no es imagen se omite y el icono cae a la inicial`() = runTest {
        val subs = importAndCaptureSubs(
            backup(subscriptions = listOf(sub("asset:evil")), assets = mapOf("evil" to b64("SQLite format 3".toByteArray())))
        )

        assertEquals(0, iconsDir.listFiles()!!.size)
        assertEquals("initial", subs.single().iconRef)
    }

    @Test
    fun `un asset con base64 invalido se omite sin abortar`() = runTest {
        val subs = importAndCaptureSubs(
            backup(subscriptions = listOf(sub("asset:x")), assets = mapOf("x" to "esto no es base64 !!!"))
        )
        assertEquals("initial", subs.single().iconRef)
    }

    @Test
    fun `iconRef file arbitrario se descarta y bundled se conserva`() = runTest {
        val subs = importAndCaptureSubs(
            backup(subscriptions = listOf(sub("file:/data/data/com.parra.misdineros/databases/mis_dineros.db"), sub("bundled:netflix"), sub("initial")))
        )
        assertEquals(listOf("initial", "bundled:netflix", "initial"), subs.map { it.iconRef })
    }

    @Test
    fun `asset por encima del limite aborta la importacion sin restaurar`() = runTest {
        val huge = "A".repeat(BackupAssets.MAX_ASSET_BASE64_CHARS + 4)
        givenFile(BackupCrypto.wrapPlain(backup(assets = mapOf("big" to huge))))

        val result = useCase(uri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("8 MB"))
        coVerify(exactly = 0) { backupRepository.restore(any()) }
    }

    @Test
    fun `fichero por encima del limite falla sin leerlo entero`() = runTest {
        var served = 0L
        val endless = object : InputStream() {
            override fun read(): Int { served++; return 'x'.code }
            override fun read(b: ByteArray, off: Int, len: Int): Int { served += len; return len }
        }
        every { resolver.openInputStream(uri) } returns endless

        val result = useCase(uri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("32 MB"))
        assertTrue("leidos $served bytes", served <= BackupAssets.MAX_FILE_BYTES + 64 * 1024)
        coVerify(exactly = 0) { backupRepository.restore(any()) }
    }

    @Test
    fun `un backup exportado por la app con assets se importa con normalidad`() = runTest {
        val subs = importAndCaptureSubs(
            backup(subscriptions = listOf(sub("asset:abc.jpg")), assets = mapOf("abc.jpg" to b64(jpegBytes)))
        )
        val file = File(subs.single().iconRef.removePrefix("file:"))
        assertTrue(file.exists())
        assertTrue(file.readBytes().contentEquals(jpegBytes))
    }
}
