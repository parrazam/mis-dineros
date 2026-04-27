package com.parra.misdineros.domain.usecase

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.parra.misdineros.data.backup.BackupJson
import com.parra.misdineros.data.backup.toDomain
import com.parra.misdineros.domain.repository.BackupRepository
import com.parra.misdineros.domain.repository.BackupSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

class ImportDataUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
) {
    private val lenientJson = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(uri: Uri): Result<Unit> = runCatching {
        val jsonStr = context.contentResolver.openInputStream(uri)?.buffered()?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: error("No se pudo leer el archivo")

        val backupJson = lenientJson.decodeFromString<BackupJson>(jsonStr)
        require(backupJson.version <= CURRENT_VERSION) {
            "Versión de backup no soportada: ${backupJson.version}"
        }

        val iconsDir = File(context.filesDir, "icons").also { it.mkdirs() }
        val resolvedIconRefs = mutableMapOf<String, String>()
        backupJson.assets.forEach { (filename, base64) ->
            val outFile = File(iconsDir, filename)
            outFile.writeBytes(Base64.decode(base64, Base64.NO_WRAP))
            resolvedIconRefs["asset:$filename"] = "file:${outFile.absolutePath}"
        }

        val snapshot = BackupSnapshot(
            subscriptions = backupJson.subscriptions.map { dto ->
                dto.toDomain(resolvedIconRefs[dto.iconRef] ?: dto.iconRef)
            },
            categories = backupJson.categories.map { it.toDomain() },
            fxRates = backupJson.fxRates.map { it.toDomain() },
            settings = backupJson.settings.toDomain(),
        )
        backupRepository.restore(snapshot)
    }

    companion object {
        private const val CURRENT_VERSION = 1
    }
}
