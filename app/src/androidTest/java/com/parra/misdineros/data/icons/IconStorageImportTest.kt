package com.parra.misdineros.data.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parra.misdineros.core.image.ImageSizing
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Las imágenes de la galería se guardan reducidas y como JPEG, sean cuales sean su tamaño y formato. */
@RunWith(AndroidJUnit4::class)
class IconStorageImportTest {

    private lateinit var context: Context
    private lateinit var storage: IconStorage
    private lateinit var srcDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = IconStorage(context)
        srcDir = File(context.cacheDir, "icon-src-${System.nanoTime()}").also { it.mkdirs() }
    }

    @After
    fun tearDown() {
        srcDir.deleteRecursively()
        File(context.filesDir, "icons").deleteRecursively()
        File(context.filesDir, "category_icons").deleteRecursively()
    }

    private fun bigPng(width: Int, height: Int): Uri {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val file = File(srcDir, "big.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(file)
    }

    private fun decode(ref: String) = BitmapFactory.decodeFile(ref.removePrefix("file:"))!!

    @Test
    fun unaFotoGrandeSeGuardaReducidaComoJpegEnIcons() = runTest {
        val ref = storage.importFromUri(bigPng(3000, 1500), IconStorage.Kind.SUBSCRIPTION)!!

        val file = File(ref.removePrefix("file:"))
        assertEquals(File(context.filesDir, "icons"), file.parentFile)
        assertTrue(file.name.endsWith(".jpg"))
        val head = file.readBytes().copyOfRange(0, 3)
        assertTrue(head.contentEquals(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())))
        val bmp = decode(ref)
        assertEquals(ImageSizing.STORED_MAX_SIDE, maxOf(bmp.width, bmp.height))
        assertEquals(bmp.width / 2, bmp.height)
        assertTrue("${file.length()} bytes", file.length() < 200_000)
    }

    @Test
    fun unaImagenPequenaNoSeAmplia() = runTest {
        val ref = storage.importFromUri(bigPng(100, 60), IconStorage.Kind.CATEGORY)!!

        assertEquals(File(context.filesDir, "category_icons"), File(ref.removePrefix("file:")).parentFile)
        val bmp = decode(ref)
        assertEquals(100, bmp.width)
        assertEquals(60, bmp.height)
    }

    @Test
    fun unFicheroQueNoEsImagenDevuelveNullYNoDejaRestos() = runTest {
        val file = File(srcDir, "not-an-image.jpg").apply { writeText("hola") }

        val ref = storage.importFromUri(Uri.fromFile(file), IconStorage.Kind.SUBSCRIPTION)

        assertNull(ref)
        assertEquals(0, File(context.filesDir, "icons").listFiles()?.size ?: 0)
    }
}
