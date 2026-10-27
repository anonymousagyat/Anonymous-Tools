package com.anonymoustools.app.core.crypto

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.log2
import kotlin.math.pow

object CryptoManager {

    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    private val secureRandom = SecureRandom()

    fun encrypt(plaintext: String, passphrase: CharArray): String {
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)

        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, ITERATION_COUNT, KEY_LENGTH)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Output format: salt + iv + ciphertext (Base64 encoded)
        val combined = ByteArray(salt.size + iv.size + ciphertext.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(ciphertext, 0, combined, salt.size + iv.size, ciphertext.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedBase64: String, passphrase: CharArray): Result<String> {
        return runCatching {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < SALT_LENGTH + GCM_IV_LENGTH) {
                throw IllegalArgumentException("Invalid payload size")
            }

            val salt = ByteArray(SALT_LENGTH)
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH)

            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, SALT_LENGTH, iv, 0, GCM_IV_LENGTH)

            val ciphertextLength = combined.size - SALT_LENGTH - GCM_IV_LENGTH
            val ciphertext = ByteArray(ciphertextLength)
            System.arraycopy(combined, SALT_LENGTH + GCM_IV_LENGTH, ciphertext, 0, ciphertextLength)

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(passphrase, salt, ITERATION_COUNT, KEY_LENGTH)
            val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        }
    }

    fun hashText(text: String, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        val hash = digest.digest(text.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun generatePassword(
        length: Int,
        includeUppercase: Boolean,
        includeLowercase: Boolean,
        includeDigits: Boolean,
        includeSymbols: Boolean,
        excludeAmbiguous: Boolean
    ): String {
        val upperChars = if (excludeAmbiguous) "ABCDEFGHJKLMNPQRSTUVWXYZ" else "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowerChars = if (excludeAmbiguous) "abcdefghijkmnopqrstuvwxyz" else "abcdefghijklmnopqrstuvwxyz"
        val digitChars = if (excludeAmbiguous) "23456789" else "0123456789"
        val symbolChars = "!@#$%^&*()-_=+[]{}|;:,.<>?"

        val charPool = buildString {
            if (includeUppercase) append(upperChars)
            if (includeLowercase) append(lowerChars)
            if (includeDigits) append(digitChars)
            if (includeSymbols) append(symbolChars)
        }

        if (charPool.isEmpty()) return ""

        return (1..length)
            .map { charPool[secureRandom.nextInt(charPool.length)] }
            .joinToString("")
    }

    fun calculateEntropy(password: String): Double {
        if (password.isEmpty()) return 0.0
        var poolSize = 0
        if (password.any { it.isUpperCase() }) poolSize += 26
        if (password.any { it.isLowerCase() }) poolSize += 26
        if (password.any { it.isDigit() }) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32

        if (poolSize == 0) return 0.0
        return password.length * log2(poolSize.toDouble())
    }
}
