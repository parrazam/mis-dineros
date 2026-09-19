package com.parra.misdineros.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupAssetsTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }
    private fun ascii(s: String) = s.toByteArray(Charsets.US_ASCII)

    @Test
    fun `detecta jpeg png webp gif y heic por su firma`() {
        assertEquals("jpg", BackupAssets.imageExtension(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 0)))
        assertEquals("png", BackupAssets.imageExtension(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0)))
        assertEquals("webp", BackupAssets.imageExtension(ascii("RIFF") + bytes(0, 0, 0, 0) + ascii("WEBPVP8 ")))
        assertEquals("gif", BackupAssets.imageExtension(ascii("GIF89a......")))
        assertEquals("heic", BackupAssets.imageExtension(bytes(0, 0, 0, 0x18) + ascii("ftypheic....")))
    }

    @Test
    fun `contenido que no es imagen no tiene extension`() {
        assertNull(BackupAssets.imageExtension(ByteArray(0)))
        assertNull(BackupAssets.imageExtension(ascii("SQLite format 3") + bytes(0)))
        assertNull(BackupAssets.imageExtension(ascii("{\"json\": true}")))
        assertNull(BackupAssets.imageExtension(bytes(0xFF, 0xD8)))  // JPEG truncado
    }

    @Test
    fun `asset resuelto usa la ruta escrita`() {
        val written = mapOf("asset:a.jpg" to "file:/data/icons/uuid.jpg")
        assertEquals("file:/data/icons/uuid.jpg", BackupAssets.resolveIconRef("asset:a.jpg", written))
    }

    @Test
    fun `asset ausente cae a la inicial`() {
        assertEquals("initial", BackupAssets.resolveIconRef("asset:missing.jpg", emptyMap()))
    }

    @Test
    fun `bundled se conserva`() {
        assertEquals("bundled:netflix", BackupAssets.resolveIconRef("bundled:netflix", emptyMap()))
    }

    @Test
    fun `file y cualquier otra cosa caen a la inicial`() {
        assertEquals("initial", BackupAssets.resolveIconRef("file:/data/data/com.parra.misdineros/databases/mis_dineros.db", emptyMap()))
        assertEquals("initial", BackupAssets.resolveIconRef("file:/etc/hosts", emptyMap()))
        assertEquals("initial", BackupAssets.resolveIconRef("initial", emptyMap()))
        assertEquals("initial", BackupAssets.resolveIconRef("", emptyMap()))
        assertEquals("initial", BackupAssets.resolveIconRef("content://media/external/images/1", emptyMap()))
    }
}
