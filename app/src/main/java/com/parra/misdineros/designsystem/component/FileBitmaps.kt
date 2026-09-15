package com.parra.misdineros.designsystem.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.parra.misdineros.core.image.ImageSizing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Carga un fichero de imagen fuera del hilo principal, muestreado al tamaño en el que se va a
 * pintar. Devuelve `null` mientras carga o si el fichero no es decodificable; el llamador
 * muestra su fallback en ambos casos. Antes se hacía `BitmapFactory.decodeFile` a resolución
 * completa dentro de `remember`, en el hilo de UI, por cada elemento de las listas.
 */
@Composable
fun rememberFileBitmap(path: String, size: Dp): ImageBitmap? {
    val targetPx = with(LocalDensity.current) { size.roundToPx() }
    val state = produceState<ImageBitmap?>(initialValue = null, path, targetPx) {
        value = withContext(Dispatchers.IO) { decodeSampledFile(path, targetPx)?.asImageBitmap() }
    }
    return state.value
}

/** Decodifica [path] con el `inSampleSize` que deja ambos lados ≥ [targetPx]. */
fun decodeSampledFile(path: String, targetPx: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val opts = BitmapFactory.Options().apply {
        inSampleSize = ImageSizing.inSampleSize(bounds.outWidth, bounds.outHeight, targetPx)
    }
    BitmapFactory.decodeFile(path, opts)
}.getOrNull()
