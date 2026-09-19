package com.parra.misdineros.core.image

/** Cálculos puros de tamaño de imagen, compartidos por el almacenamiento y la carga en la UI. */
object ImageSizing {

    /** Lado mayor con el que se guardan los iconos subidos por el usuario. */
    const val STORED_MAX_SIDE = 512

    /**
     * Mayor potencia de 2 tal que la imagen decodificada siga teniendo ambos lados ≥ [target].
     * Es el `inSampleSize` que `BitmapFactory`/`ImageDecoder` aplican al decodificar, y evita
     * cargar en memoria una foto de 12 MP para pintar un círculo de 48 dp.
     */
    fun inSampleSize(width: Int, height: Int, target: Int): Int {
        if (width <= 0 || height <= 0 || target <= 0) return 1
        var sample = 1
        while (width / (sample * 2) >= target && height / (sample * 2) >= target) sample *= 2
        return sample
    }

    /** Dimensiones finales para que el lado mayor no supere [maxSide], conservando la proporción. */
    fun fitWithin(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
        val longest = maxOf(width, height)
        if (longest <= maxSide) return width to height
        val scale = maxSide.toDouble() / longest
        return maxOf(1, Math.round(width * scale).toInt()) to maxOf(1, Math.round(height * scale).toInt())
    }
}
