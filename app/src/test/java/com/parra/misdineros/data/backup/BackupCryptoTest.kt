package com.parra.misdineros.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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

    // ─── Fase 6: cabecera autenticada ──────────────────────────────────────────

    /**
     * Fichero generado con el formato inicial (flags 0x01, sin AAD) por una implementación
     * independiente (python `cryptography`, PBKDF2-HMAC-SHA256 200k, AES-256-GCM).
     * Contraseña: "contraseña-legacy". Debe seguir descifrándose para siempre.
     */
    private val legacyEncryptedBase64 = "TURCMQFHE+hCVJVGf44Ai7oxR5psUq1IxBeIoEmwGy60cMKlZNqRxHd4vmLBY9tcYiVaWzYYMcKBFyi1PD2gt1J0rJ7d89+ZzYM1UqRYJAc5J7oMT55qnrYZM5rdonWLR6cOZD7+HVscRuFKi9/0OFarYPo0x3J8mcI0ReKP2Y6sYhpiMdGj6lmC9xPUWe0jNtWWvw5TDK2Q9DnqD6ptqnOR+jmdkyWphnKs2U7NrWgFOFwDMhDoPwLuAxncLAojSW4T0pjW7pupwBoKLBO7wTF0K9qfM009ua0m3L+2+KCBRHxQCaJk7m9ri5IoK2vzDKsN3z4KEmWYebHNwoV/gBFyz2v4lToI4uYhSAwfv7pKVZmzhJIGE2OZIYwPVax/u016iISeOTiij0/Cd1FpXweNDEY79Hm8"
    private val legacyJson = """{"version":1,"exportedAt":"2026-05-08T10:00:00Z","subscriptions":[],"categories":[],"fxRates":[],"settings":{"globalCurrencyCode":"EUR","notificationsEnabled":true,"notificationHour":9,"defaultNotifyDaysBefore":3,"monthlySummaryEnabled":true,"appTheme":"SYSTEM"}}"""

    @Test
    fun `los ficheros nuevos llevan flags 0x02`() {
        val encrypted = BackupCrypto.encrypt(sampleJson, "12345678".toCharArray())
        assertEquals(0x02.toByte(), encrypted[4])
        assertEquals(BackupFormat.Encrypted, BackupCrypto.detectFormat(encrypted))
    }

    @Test
    fun `un fichero cifrado con el formato inicial se sigue descifrando`() {
        val legacy = java.util.Base64.getDecoder().decode(legacyEncryptedBase64)
        assertEquals(0x01.toByte(), legacy[4])
        assertEquals(BackupFormat.Encrypted, BackupCrypto.detectFormat(legacy))
        assertEquals(legacyJson, BackupCrypto.decrypt(legacy, "contraseña-legacy".toCharArray()))
    }

    @Test
    fun `degradar el byte de flags de un fichero nuevo invalida el tag`() {
        val encrypted = BackupCrypto.encrypt(sampleJson, "12345678".toCharArray())
        val downgraded = encrypted.copyOf().also { it[4] = 0x01 }
        assertThrows(WrongPasswordException::class.java) { BackupCrypto.decrypt(downgraded, "12345678".toCharArray()) }
    }

    @Test
    fun `manipular salt, IV o ciphertext invalida el tag`() {
        val encrypted = BackupCrypto.encrypt(sampleJson, "12345678".toCharArray())
        listOf(7, 25, encrypted.size - 1).forEach { index ->
            val tampered = encrypted.copyOf().also { it[index] = (it[index].toInt() xor 0x01).toByte() }
            assertThrows("byte $index", WrongPasswordException::class.java) {
                BackupCrypto.decrypt(tampered, "12345678".toCharArray())
            }
        }
    }

    @Test
    fun `un blob cifrado truncado por debajo de la cabecera no lanza fuera de rango`() {
        assertThrows(WrongPasswordException::class.java) {
            BackupCrypto.decrypt("MDB1".toByteArray() + byteArrayOf(0x02, 1, 2, 3), "x".toCharArray())
        }
    }
}
