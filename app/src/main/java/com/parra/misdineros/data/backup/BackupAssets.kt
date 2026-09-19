package com.parra.misdineros.data.backup

/**
 * Reglas de saneado de los datos que vienen de un backup, la única entrada de datos no
 * confiables de la app. Objeto puro (sin dependencias Android) para probarlo en JVM.
 */
object BackupAssets {

    /** Tamaño máximo del fichero de backup completo; por encima no se lee siquiera a memoria. */
    const val MAX_FILE_BYTES = 32L * 1024 * 1024

    /** Tamaño máximo de una imagen embebida, ya decodificada. */
    const val MAX_ASSET_BYTES = 8 * 1024 * 1024

    /** Longitud máxima del base64 que puede producir [MAX_ASSET_BYTES]; se comprueba antes de decodificar. */
    const val MAX_ASSET_BASE64_CHARS = (MAX_ASSET_BYTES / 3 + 1) * 4

    private val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    /**
     * Extensión de fichero según la firma binaria, o `null` si el contenido no es una imagen
     * de un formato que `BitmapFactory` sepa decodificar. El nombre que traiga el backup no se
     * usa nunca: ni para la ruta (path traversal) ni para la extensión.
     */
    fun imageExtension(bytes: ByteArray): String? = when {
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "jpg"
        bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(PNG) -> "png"
        bytes.size >= 12 && ascii(bytes, 0, 4) == "RIFF" && ascii(bytes, 8, 4) == "WEBP" -> "webp"
        bytes.size >= 6 && ascii(bytes, 0, 6).let { it == "GIF87a" || it == "GIF89a" } -> "gif"
        bytes.size >= 12 && ascii(bytes, 4, 4) == "ftyp" -> "heic"
        else -> null
    }

    /**
     * Devuelve el `iconRef` que se persistirá para una suscripción importada:
     *  - `asset:<nombre>` → la ruta `file:` del asset ya escrito, o `initial` si no venía o era inválido.
     *  - `bundled:<clave>` → tal cual (una clave desconocida ya cae a la inicial en la UI).
     *  - cualquier otra cosa, incluido `file:<ruta arbitraria>` → `initial`.
     */
    fun resolveIconRef(iconRef: String, writtenAssets: Map<String, String>): String = when {
        iconRef.startsWith("asset:") -> writtenAssets[iconRef] ?: "initial"
        iconRef.startsWith("bundled:") -> iconRef
        else -> "initial"
    }

    /** Clave de icono de categoría por defecto cuando la importada no es utilizable. */
    const val DEFAULT_CATEGORY_ICON = "category"

    /**
     * Las categorías no exportan su imagen (solo la clave), así que un `file:` importado
     * apuntaría a una ruta de otro dispositivo o, peor, a un fichero arbitrario de este.
     */
    fun resolveCategoryIconKey(iconKey: String): String =
        if (iconKey.startsWith("file:")) DEFAULT_CATEGORY_ICON else iconKey

    private fun ascii(bytes: ByteArray, offset: Int, len: Int) =
        String(bytes, offset, len, Charsets.US_ASCII)
}
