package com.parra.misdineros.data.icons

import android.content.Context
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class IconStorageTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var storage: IconStorage
    private lateinit var icons: File
    private lateinit var categoryIcons: File

    @Before
    fun setUp() {
        // android.util.Log es un stub en JVM: sin esto cada Log.i lanza "Method i not mocked".
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        val context = mockk<Context> { every { filesDir } returns tmp.root }
        storage = IconStorage(context)
        icons = File(tmp.root, "icons").also { it.mkdirs() }
        categoryIcons = File(tmp.root, "category_icons").also { it.mkdirs() }
    }

    @After
    fun tearDown() = unmockkAll()

    private fun file(dir: File, name: String) = File(dir, name).also { it.writeBytes(byteArrayOf(1, 2, 3)) }

    // ─── delete ────────────────────────────────────────────────────────────────

    @Test
    fun `borra un fichero de icons y de category_icons`() = runTest {
        val a = file(icons, "a.jpg")
        val b = file(categoryIcons, "b.jpg")

        storage.delete("file:${a.absolutePath}")
        storage.delete("file:${b.absolutePath}")

        assertFalse(a.exists())
        assertFalse(b.exists())
    }

    @Test
    fun `no borra nada fuera de los directorios gestionados`() = runTest {
        val db = File(tmp.root, "databases").also { it.mkdirs() }.let { file(it, "mis_dineros.db") }
        val outside = tmp.newFile("outside.jpg")
        val nested = File(icons, "sub").also { it.mkdirs() }.let { file(it, "n.jpg") }

        storage.delete("file:${db.absolutePath}")
        storage.delete("file:${outside.absolutePath}")
        storage.delete("file:${nested.absolutePath}")
        storage.delete("file:${File(icons, "../databases/mis_dineros.db").path}")

        assertTrue(db.exists())
        assertTrue(outside.exists())
        assertTrue(nested.exists())
    }

    @Test
    fun `ignora referencias que no son file`() = runTest {
        storage.delete(null)
        storage.delete("initial")
        storage.delete("bundled:netflix")
        storage.delete("emoji:🎬")
        storage.delete("file:")
        storage.delete("file:${File(icons, "no-existe.jpg").absolutePath}")
    }

    // ─── pruneOrphans ──────────────────────────────────────────────────────────

    @Test
    fun `poda lo no referenciado y conserva lo referenciado en ambos directorios`() = runTest {
        val keepSub = file(icons, "keep.jpg")
        val dropSub = file(icons, "drop.jpg")
        val keepCat = file(categoryIcons, "keep.jpg")
        val dropCat = file(categoryIcons, "drop.jpg")

        storage.pruneOrphans(
            setOf("file:${keepSub.absolutePath}", "file:${keepCat.absolutePath}", "bundled:netflix", "initial", "emoji:x")
        )

        assertTrue(keepSub.exists())
        assertTrue(keepCat.exists())
        assertFalse(dropSub.exists())
        assertFalse(dropCat.exists())
    }

    @Test
    fun `podar con directorios inexistentes no falla`() = runTest {
        icons.deleteRecursively()
        categoryIcons.deleteRecursively()
        storage.pruneOrphans(emptySet())
    }

    @Test
    fun `podar no toca ficheros fuera de los directorios gestionados`() = runTest {
        val outside = tmp.newFile("settings.preferences_pb")
        storage.pruneOrphans(emptySet())
        assertTrue(outside.exists())
    }
}
