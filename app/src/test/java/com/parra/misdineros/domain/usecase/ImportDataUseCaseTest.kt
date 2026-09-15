package com.parra.misdineros.domain.usecase

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.parra.misdineros.data.backup.BackupCrypto
import com.parra.misdineros.data.backup.BackupJson
import com.parra.misdineros.data.backup.SettingsDto
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream

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

    private fun backup(settings: SettingsDto) = Json.encodeToString(
        BackupJson(
            exportedAt = "2026-09-15T10:00:00Z",
            subscriptions = emptyList(),
            categories = emptyList(),
            fxRates = emptyList(),
            settings = settings,
        )
    )

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
}
