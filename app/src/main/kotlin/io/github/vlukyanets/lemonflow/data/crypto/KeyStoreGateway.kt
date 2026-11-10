package io.github.vlukyanets.lemonflow.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The wrapping key that protects the database passphrase. Tests use an in-memory fake. */
interface KeyStoreGateway {
    fun hasKey(): Boolean

    fun createKey()

    fun deleteKey()

    /** Returns `IV (12 bytes) + ciphertext`. */
    fun wrap(plain: ByteArray): ByteArray

    /** @throws GeneralSecurityException when the key is gone or the blob was tampered with. */
    fun unwrap(blob: ByteArray): ByteArray
}

/** AES-256-GCM key in AndroidKeyStore, deliberately not bound to user authentication (FR-013a). */
class AndroidKeyStoreGateway : KeyStoreGateway {
    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    override fun hasKey(): Boolean = keyStore().containsAlias(ALIAS)

    override fun createKey() {
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
            .apply { init(wrapKeySpec()) }
            .generateKey()
    }

    override fun deleteKey() {
        val store = keyStore()
        if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS)
    }

    override fun wrap(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.iv + cipher.doFinal(plain)
    }

    override fun unwrap(blob: ByteArray): ByteArray {
        if (blob.size <= IV_SIZE) throw GeneralSecurityException("Wrapped key is truncated")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, blob, 0, IV_SIZE))
        return cipher.doFinal(blob, IV_SIZE, blob.size - IV_SIZE)
    }

    private fun key(): SecretKey = keyStore().getKey(ALIAS, null) as? SecretKey
        ?: throw GeneralSecurityException("Wrapping key is missing")

    companion object {
        const val ALIAS = "lemonflow_db_wrap"
        private const val PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
        private const val KEY_BITS = 256

        fun wrapKeySpec(): KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
            // A lock-screen change must never make the data unreadable.
            .setUserAuthenticationRequired(false)
            .build()
    }
}
