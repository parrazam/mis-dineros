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
 *   [1] flags   0x00=plano, 0x01=cifrado (formato inicial), 0x02=cifrado con cabecera autenticada
 * Si cifrado:
 *   [16] salt  PBKDF2WithHmacSHA256
 *   [12] IV    GCM nonce
 *   [N]  ciphertext + tag GCM (16 bytes)
 * Sin cabecera MDB1 → JSON legacy (compatibilidad con exports anteriores).
 *
 * Con flags 0x02 los 33 bytes de cabecera (magic, flags, salt, IV) entran como AAD en GCM, de
 * modo que cualquier cambio en ellos (por ejemplo, degradar el byte de flags o los parámetros
 * del KDF que se añadan en el futuro) invalida el tag. Los ficheros 0x01 ya exportados se
 * siguen descifrando sin AAD; solo se escriben ficheros 0x02.
 *
 * `encrypt`/`decrypt` borran el `CharArray` de la contraseña tras derivar la clave.
 */
object BackupCrypto {

    private val MAGIC = "MDB1".toByteArray(Charsets.US_ASCII)
    private const val FLAG_PLAIN: Byte = 0x00
    private const val FLAG_ENCRYPTED_V1: Byte = 0x01
    private const val FLAG_ENCRYPTED_V2: Byte = 0x02
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val HEADER_LEN = 4 + 1 + SALT_LEN + IV_LEN
    private const val PBKDF2_ITERATIONS = 200_000
    private const val KEY_BITS = 256

    /** Longitud mínima de contraseña exigida por la UI de exportación. */
    const val MIN_PASSWORD_LENGTH = 8

    fun wrapPlain(json: String): ByteArray =
        MAGIC + byteArrayOf(FLAG_PLAIN) + json.toByteArray(Charsets.UTF_8)

    fun encrypt(json: String, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val header = MAGIC + byteArrayOf(FLAG_ENCRYPTED_V2) + salt + iv
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(json.toByteArray(Charsets.UTF_8))
    }

    fun decrypt(blob: ByteArray, password: CharArray): String {
        if (blob.size < HEADER_LEN) throw WrongPasswordException()
        val flags = blob[4]
        val header = blob.copyOfRange(0, HEADER_LEN)
        val salt = blob.copyOfRange(5, 5 + SALT_LEN)
        val iv = blob.copyOfRange(5 + SALT_LEN, HEADER_LEN)
        val ciphertext = blob.copyOfRange(HEADER_LEN, blob.size)
        val key = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            if (flags == FLAG_ENCRYPTED_V2) cipher.updateAAD(header)
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
            FLAG_ENCRYPTED_V1, FLAG_ENCRYPTED_V2 -> BackupFormat.Encrypted
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

/** GCM no distingue contraseña incorrecta de fichero manipulado: el tag falla en ambos casos. */
class WrongPasswordException : Exception("Contraseña incorrecta o archivo dañado")
class PasswordRequiredException : Exception("Este archivo está cifrado. Introduce la contraseña para importarlo.")
