package com.parra.misdineros.data.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import com.parra.misdineros.core.image.ImageSizing
import com.parra.misdineros.domain.repository.IconStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Almacén de iconos subidos por el usuario.
 *
 * Al importar desde la galería la imagen se decodifica con muestreo, se reduce a
 * [ImageSizing.STORED_MAX_SIDE] px de lado mayor y se guarda como JPEG: una foto de cámara
 * copiada tal cual pesaba varios MB, se decodificaba entera en cada lista y, al estar
 * `files/icons/` dentro de las reglas de Auto Backup, unas pocas agotaban la cuota de 25 MB
 * y la copia dejaba de subirse sin aviso.
 */
@Singleton
class IconStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) : IconStore {

    enum class Kind(val dirName: String) {
        SUBSCRIPTION("icons"),
        CATEGORY("category_icons"),
    }

    private fun dir(kind: Kind) = File(context.filesDir, kind.dirName)
    private val managedDirs get() = Kind.entries.map { dir(it).canonicalFile }

    /**
     * Copia la imagen de [uri] reducida y re-codificada a JPEG en el directorio de [kind].
     * Devuelve el `file:<ruta>` resultante, o `null` si el contenido no es una imagen decodificable.
     */
    suspend fun importFromUri(uri: Uri, kind: Kind): String? = withContext(Dispatchers.IO) {
        val bitmap = runCatching { decodeDownsampled(uri) }
            .onFailure { Log.w(TAG, "No se pudo decodificar la imagen elegida", it) }
            .getOrNull() ?: return@withContext null
        val (w, h) = ImageSizing.fitWithin(bitmap.width, bitmap.height, ImageSizing.STORED_MAX_SIDE)
        val scaled = if (w != bitmap.width || h != bitmap.height) Bitmap.createScaledBitmap(bitmap, w, h, true) else bitmap
        val target = File(dir(kind).also { it.mkdirs() }, "${UUID.randomUUID()}.jpg")
        runCatching {
            target.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        }.onFailure {
            Log.w(TAG, "No se pudo guardar el icono", it)
            target.delete()
        }.getOrNull() ?: return@withContext null
        "file:${target.absolutePath}"
    }

    private fun decodeDownsampled(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder aplica la orientación EXIF, así una foto vertical no se guarda tumbada.
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.setTargetSampleSize(
                    ImageSizing.inSampleSize(info.size.width, info.size.height, ImageSizing.STORED_MAX_SIDE)
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = ImageSizing.inSampleSize(bounds.outWidth, bounds.outHeight, ImageSizing.STORED_MAX_SIDE)
        }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    override suspend fun delete(ref: String?) = withContext(Dispatchers.IO) {
        val file = managedFileOf(ref) ?: return@withContext
        if (!file.delete() && file.exists()) Log.w(TAG, "No se pudo borrar el icono ${file.name}")
    }

    override suspend fun pruneOrphans(referenced: Set<String>) = withContext(Dispatchers.IO) {
        val keep = referenced.mapNotNull { managedFileOf(it) }.toSet()
        Kind.entries.forEach { kind ->
            dir(kind).listFiles()?.forEach { file ->
                if (file.isFile && file.canonicalFile !in keep) {
                    Log.i(TAG, "Icono huérfano eliminado: ${kind.dirName}/${file.name}")
                    file.delete()
                }
            }
        }
    }

    /** Fichero canónico al que apunta un `file:` si, y solo si, está directamente en un directorio gestionado. */
    private fun managedFileOf(ref: String?): File? {
        if (ref == null || !ref.startsWith(PREFIX)) return null
        val file = runCatching { File(ref.removePrefix(PREFIX)).canonicalFile }.getOrNull() ?: return null
        return file.takeIf { it.parentFile in managedDirs }
    }

    private companion object {
        const val TAG = "IconStorage"
        const val PREFIX = "file:"
        const val JPEG_QUALITY = 85
    }
}
