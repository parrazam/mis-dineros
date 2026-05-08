package com.parra.misdineros.domain.usecase

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.parra.misdineros.data.backup.BackupJson
import com.parra.misdineros.data.backup.toDto
import com.parra.misdineros.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
) {
    suspend fun exportToBytes(): Result<ByteArray> = runCatching {
        buildJson().toByteArray(Charsets.UTF_8)
    }

    // Sobrecarga legacy usada mientras coexista el flujo SAF antiguo.
    suspend operator fun invoke(uri: Uri): Result<Unit> = runCatching {
        val bytes = buildJson().toByteArray(Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri)?.buffered()?.use { it.write(bytes) }
            ?: error("No se pudo abrir el archivo para escritura")
    }

    private suspend fun buildJson(): String {
        val snapshot = backupRepository.snapshot()

        val assets = mutableMapOf<String, String>()
        val processedSubs = snapshot.subscriptions.map { sub ->
            if (sub.iconRef.startsWith("file:")) {
                val file = File(sub.iconRef.removePrefix("file:"))
                if (file.exists()) {
                    assets[file.name] = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                    sub.copy(iconRef = "asset:${file.name}")
                } else {
                    sub.copy(iconRef = "initial")
                }
            } else sub
        }

        return Json.encodeToString(
            BackupJson(
                exportedAt = Instant.now().toString(),
                subscriptions = processedSubs.map { it.toDto() },
                categories = snapshot.categories.map { it.toDto() },
                fxRates = snapshot.fxRates.map { it.toDto() },
                settings = snapshot.settings.toDto(),
                assets = assets,
            )
        )
    }
}
