package io.github.vlukyanets.lemonflow.data.crypto

import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.SecureRandom

sealed interface KeyResult {
    class Ready(val passphrase: ByteArray) : KeyResult

    data object Unavailable : KeyResult
}

/**
 * Owns the SQLCipher passphrase: 32 random bytes generated on the device, stored only wrapped
 * by the Keystore key in [keyFile] (research R2). Never logs.
 */
class DatabaseKeyManager(
    private val gateway: KeyStoreGateway,
    private val keyFile: File,
    private val databaseFiles: () -> List<File>,
    private val random: SecureRandom
) {
    fun obtainKey(): KeyResult = when {
        keyFile.exists() -> unwrapExisting()

        // A database without its key can never be opened again; never overwrite it silently.
        databaseFiles().any { it.exists() } -> KeyResult.Unavailable

        else -> createNew()
    }

    /** Erases the database files, the wrapped key and the Keystore entry (FR-015). */
    fun wipe() {
        databaseFiles().forEach { it.delete() }
        keyFile.delete()
        gateway.deleteKey()
    }

    private fun unwrapExisting(): KeyResult {
        if (!gateway.hasKey()) return KeyResult.Unavailable
        return try {
            KeyResult.Ready(gateway.unwrap(keyFile.readBytes()))
        } catch (ignored: GeneralSecurityException) {
            KeyResult.Unavailable
        } catch (ignored: IOException) {
            KeyResult.Unavailable
        }
    }

    private fun createNew(): KeyResult {
        gateway.deleteKey()
        gateway.createKey()
        val passphrase = ByteArray(PASSPHRASE_BYTES).also(random::nextBytes)
        val temp = File(keyFile.parentFile, keyFile.name + ".tmp")
        temp.writeBytes(gateway.wrap(passphrase))
        if (!temp.renameTo(keyFile)) throw IOException("Could not store the wrapped database key")
        return KeyResult.Ready(passphrase)
    }

    private companion object {
        const val PASSPHRASE_BYTES = 32
    }
}
