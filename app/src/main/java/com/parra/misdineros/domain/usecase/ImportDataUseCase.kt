package com.parra.misdineros.domain.usecase

import android.content.Context
import android.net.Uri
import com.parra.misdineros.data.backup.BackupAssets
import com.parra.misdineros.data.backup.BackupCrypto
import com.parra.misdineros.data.backup.BackupFormat
import com.parra.misdineros.data.backup.BackupJson
import com.parra.misdineros.data.backup.PasswordRequiredException
import com.parra.misdineros.data.backup.toDomain
import com.parra.misdineros.domain.repository.BackupRepository
import com.parra.misdineros.domain.repository.BackupSnapshot
import com.parra.misdineros.notifications.NotificationScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.Base64
import java.util.UUID
import javax.inject.Inject

class ImportDataUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val advanceDueRenewals: AdvanceDueRenewalsUseCase,
    private val notificationScheduler: NotificationScheduler,
) {
    private val lenientJson = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(uri: Uri, password: CharArray? = null): Result<Unit> = runCatching {
        val blob = context.contentResolver.openInputStream(uri)?.use { it.readAtMost(BackupAssets.MAX_FILE_BYTES) }
            ?: error("No se pudo leer el archivo")

        val jsonStr = when (BackupCrypto.detectFormat(blob)) {
            BackupFormat.LegacyJson -> blob.toString(Charsets.UTF_8)
            BackupFormat.Plain -> blob.copyOfRange(5, blob.size).toString(Charsets.UTF_8)
            BackupFormat.Encrypted -> {
                val pwd = password ?: throw PasswordRequiredException()
                withContext(Dispatchers.Default) { BackupCrypto.decrypt(blob, pwd) }
            }
        }

        val backupJson = lenientJson.decodeFromString<BackupJson>(jsonStr)
        require(backupJson.version <= CURRENT_VERSION) {
            "Versión de backup no soportada: ${backupJson.version}"
        }

        val iconsDir = File(context.filesDir, "icons").also { it.mkdirs() }
        val writtenAssets = writeAssets(backupJson.assets, iconsDir)

        val snapshot = BackupSnapshot(
            subscriptions = backupJson.subscriptions.map { dto ->
                dto.toDomain(BackupAssets.resolveIconRef(dto.iconRef, writtenAssets))
            },
            categories = backupJson.categories.map { it.toDomain() },
            fxRates = backupJson.fxRates.map { it.toDomain() },
            settings = backupJson.settings.toDomain(),
        )
        backupRepository.restore(snapshot)
        // Tras restaurar, avanza las renovaciones que vengan vencidas en el backup importado,
        // sin esperar a un reinicio de la app.
        advanceDueRenewals()
        // Los ajustes importados traen hora y estado de las notificaciones; sin esto la cadena
        // diaria seguiría con la programación anterior hasta el siguiente arranque.
        with(snapshot.settings) {
            notificationScheduler.schedule(notificationHour, notificationMinute, notificationsEnabled)
        }
    }

    /**
     * Escribe las imágenes embebidas en [iconsDir] y devuelve `asset:<nombre>` → `file:<ruta>`.
     * El nombre del backup nunca se usa como ruta (un `../../databases/x.db` sobrescribiría
     * ficheros de la app): el fichero recibe un UUID y la extensión sale de la firma binaria.
     * Un asset que no sea imagen o no se pueda decodificar se omite y su icono cae a la inicial;
     * uno que supere [BackupAssets.MAX_ASSET_BYTES] aborta la importación con un error claro.
     */
    private fun writeAssets(assets: Map<String, String>, iconsDir: File): Map<String, String> {
        val written = mutableMapOf<String, String>()
        assets.forEach { (name, base64) ->
            require(base64.length <= BackupAssets.MAX_ASSET_BASE64_CHARS) {
                "Una imagen del backup supera el tamaño máximo (8 MB)"
            }
            val bytes = runCatching { Base64.getDecoder().decode(base64) }.getOrNull() ?: return@forEach
            require(bytes.size <= BackupAssets.MAX_ASSET_BYTES) {
                "Una imagen del backup supera el tamaño máximo (8 MB)"
            }
            val ext = BackupAssets.imageExtension(bytes) ?: return@forEach
            val outFile = File(iconsDir, "${UUID.randomUUID()}.$ext")
            outFile.writeBytes(bytes)
            written["asset:$name"] = "file:${outFile.absolutePath}"
        }
        return written
    }

    /** Lee el stream completo pero falla en cuanto supera [max] bytes, sin cargar el resto. */
    private fun InputStream.readAtMost(max: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = read(buf)
            if (n < 0) break
            total += n
            require(total <= max) { "El archivo supera el tamaño máximo de un backup (32 MB)" }
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    companion object {
        private const val CURRENT_VERSION = 1
    }
}
