package com.readyplayerai.cassetto.data.settings

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps secrets with an AES-GCM key that lives in the Android Keystore and never
 * leaves it. Equivalent in spirit to what expo-secure-store gives the official
 * React Native app.
 *
 * Neither method is cheap — always call them off the main thread.
 */
@Singleton
class KeystoreCrypto @Inject constructor() {

    /** @return Base64 of `iv || ciphertext`, safe to persist as a string. */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + ciphertext
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    /** @return the plaintext, or null if the blob is corrupt or the key is gone. */
    fun decrypt(blob: String): String? {
        return try {
            val packed = Base64.decode(blob, Base64.NO_WRAP)
            if (packed.size <= IV_SIZE) return null
            val iv = packed.copyOfRange(0, IV_SIZE)
            val ciphertext = packed.copyOfRange(IV_SIZE, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "cassetto.session.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
