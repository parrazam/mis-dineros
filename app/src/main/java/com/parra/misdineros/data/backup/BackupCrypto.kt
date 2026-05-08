package com.parra.misdineros.data.backup

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Formato de fichero (cabecera 5 bytes):
 *   [4] "MDB1"  magic
 *   [1] flags   0x00=plano, 0x01=cifrado AES-256-GCM
 * Si cifrado:
 *   [16] salt  PBKDF2WithHmacSHA256
 *   [12] IV    GCM nonce
 *   [N]  ciphertext + tag GCM (16 bytes)
 * Sin cabecera MDB1 → JSON legacy (compatibilidad con exports anteriores).
 */
object BackupCrypto {

    private val MAGIC = "MDB1".toByteArray(Charsets.US_ASCII)
    private const val FLAG_PLAIN: Byte = 0x00
    private const val FLAG_ENCRYPTED: Byte = 0x01
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val PBKDF2_ITERATIONS = 200_000
    private const val KEY_BITS = 256

    fun wrapPlain(json: String): ByteArray =
        MAGIC + byteArrayOf(FLAG_PLAIN) + json.toByteArray(Charsets.UTF_8)

    fun encrypt(json: String, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        return MAGIC + byteArrayOf(FLAG_ENCRYPTED) + salt + iv + ciphertext
    }

    fun decrypt(blob: ByteArray, password: CharArray): String {
        val offset = MAGIC.size + 1  // 4 magic + 1 flags = 5
        val salt = blob.copyOfRange(offset, offset + SALT_LEN)
        val iv = blob.copyOfRange(offset + SALT_LEN, offset + SALT_LEN + IV_LEN)
        val ciphertext = blob.copyOfRange(offset + SALT_LEN + IV_LEN, blob.size)
        val key = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        } catch (_: AEADBadTagException) {
            throw WrongPasswordException()
        }
    }

    fun detectFormat(blob: ByteArray): BackupFormat {
        if (blob.size < 5) return BackupFormat.LegacyJson
        if (!blob.copyOfRange(0, 4).contentEquals(MAGIC)) return BackupFormat.LegacyJson
        return when (blob[4]) {
            FLAG_PLAIN -> BackupFormat.Plain
            FLAG_ENCRYPTED -> BackupFormat.Encrypted
            else -> BackupFormat.LegacyJson
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        val tmp = factory.generateSecret(spec)
        spec.clearPassword()
        password.fill(' ')
        return SecretKeySpec(tmp.encoded, "AES")
    }
}

sealed class BackupFormat {
    data object Plain : BackupFormat()
    data object Encrypted : BackupFormat()
    data object LegacyJson : BackupFormat()
}

class WrongPasswordException : Exception("Contraseña incorrecta")
class PasswordRequiredException : Exception("Este archivo está cifrado. Introduce la contraseña para importarlo.")
