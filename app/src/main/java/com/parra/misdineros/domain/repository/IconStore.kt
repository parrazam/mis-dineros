package com.parra.misdineros.domain.repository

/**
 * Ficheros de icono subidos por el usuario (`file:<ruta>` en `Subscription.iconRef` y
 * `Category.iconKey`). Solo gestiona ficheros dentro de sus propios directorios.
 */
interface IconStore {
    /** Borra el fichero al que apunta [ref] si es `file:` y está en un directorio gestionado. */
    suspend fun delete(ref: String?)

    /** Borra todo fichero gestionado cuyo `file:<ruta>` no esté en [referenced]. */
    suspend fun pruneOrphans(referenced: Set<String>)
}
