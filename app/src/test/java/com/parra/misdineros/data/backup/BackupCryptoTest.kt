package com.parra.misdineros.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    private val sampleJson = """{"version":1,"exportedAt":"2026-05-08T10:00:00Z","subscriptions":[]}"""

    @Test
    fun `round-trip cifrado devuelve el json original`() {
        val encrypted = BackupCrypto.encrypt(sampleJson, "contraseña123".toCharArray())
        val decrypted = BackupCrypto.decrypt(encrypted, "contraseña123".toCharArray())
        assertEquals(sampleJson, decrypted)
    }

    @Test(expected = WrongPasswordException::class)
    fun `contrasena incorrecta lanza WrongPasswordException`() {
        val encrypted = BackupCrypto.encrypt(sampleJson, "correcta".toCharArray())
        BackupCrypto.decrypt(encrypted, "incorrecta".toCharArray())
    }

    @Test
    fun `detectFormat identifica fichero cifrado`() {
        val encrypted = BackupCrypto.encrypt(sampleJson, "pwd123".toCharArray())
        assertEquals(BackupFormat.Encrypted, BackupCrypto.detectFormat(encrypted))
    }

    @Test
    fun `detectFormat identifica fichero plano con cabecera`() {
        val plain = BackupCrypto.wrapPlain(sampleJson)
        assertEquals(BackupFormat.Plain, BackupCrypto.detectFormat(plain))
    }

    @Test
    fun `detectFormat identifica json legacy sin cabecera`() {
        val legacy = sampleJson.toByteArray(Charsets.UTF_8)
        assertEquals(BackupFormat.LegacyJson, BackupCrypto.detectFormat(legacy))
    }

    @Test
    fun `wrapPlain permite extraer el json original`() {
        val wrapped = BackupCrypto.wrapPlain(sampleJson)
        val extracted = wrapped.copyOfRange(5, wrapped.size).toString(Charsets.UTF_8)
        assertEquals(sampleJson, extracted)
    }

    @Test
    fun `ficheros cifrados distintos con la misma contrasena por salt aleatorio`() {
        val a = BackupCrypto.encrypt(sampleJson, "misma".toCharArray())
        val b = BackupCrypto.encrypt(sampleJson, "misma".toCharArray())
        assertTrue("Los ciphertexts deben diferir por el salt/IV aleatorio", !a.contentEquals(b))
    }

    @Test
    fun `blob demasiado corto detectado como legacy`() {
        assertEquals(BackupFormat.LegacyJson, BackupCrypto.detectFormat(byteArrayOf(1, 2, 3)))
    }
}
