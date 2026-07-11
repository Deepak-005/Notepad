package com.example.util

import android.util.Base64
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ITERATIONS = 1000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12 // GCM recommended IV size is 12 bytes
    private const val ALGORITHM = "AES/GCM/NoPadding"

    // Derive a 256-bit AES key from password and salt using PBKDF2
    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    // Encrypts text using a password. Returns a base64 encoded string containing [Salt (16 bytes) + IV (12 bytes) + Ciphertext]
    fun encrypt(plainText: String, password: String): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)

        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)

        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Combine salt, iv, and ciphertext into a single array
        val combined = ByteArray(salt.size + iv.size + cipherText.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(cipherText, 0, combined, salt.size + iv.size, cipherText.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    // Decrypts text using a password. Expects base64 encoded string containing [Salt (16 bytes) + IV (12 bytes) + Ciphertext]
    fun decrypt(encryptedText: String, password: String): String {
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        if (combined.size < SALT_LENGTH + IV_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted backup data")
        }

        val salt = ByteArray(SALT_LENGTH)
        val iv = ByteArray(IV_LENGTH)
        val cipherText = ByteArray(combined.size - SALT_LENGTH - IV_LENGTH)

        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH)
        System.arraycopy(combined, SALT_LENGTH, iv, 0, IV_LENGTH)
        System.arraycopy(combined, SALT_LENGTH + IV_LENGTH, cipherText, 0, cipherText.size)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)

        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
