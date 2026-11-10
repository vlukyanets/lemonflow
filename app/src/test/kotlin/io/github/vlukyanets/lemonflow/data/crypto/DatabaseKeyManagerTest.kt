package io.github.vlukyanets.lemonflow.data.crypto

import java.io.File
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DatabaseKeyManagerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val gateway = FakeKeyStoreGateway()

    private fun manager(): DatabaseKeyManager = DatabaseKeyManager(
        gateway = gateway,
        keyFile = File(tmp.root, "db_key.bin"),
        databaseFiles = {
            listOf(File(tmp.root, "lemonflow.db"), File(tmp.root, "lemonflow.db-wal"))
        },
        random = SecureRandom()
    )

    @Test
    fun firstRunGeneratesWrappedKey() {
        val result = manager().obtainKey()

        val passphrase = (result as KeyResult.Ready).passphrase
        assertEquals(32, passphrase.size)
        val stored = File(tmp.root, "db_key.bin").readBytes()
        assertFalse("raw passphrase must not be stored", stored.containsSequence(passphrase))
        assertTrue(gateway.hasKey())
    }

    @Test
    fun secondRunUnwrapsSamePassphrase() {
        val first = (manager().obtainKey() as KeyResult.Ready).passphrase.copyOf()
        val second = (manager().obtainKey() as KeyResult.Ready).passphrase

        assertArrayEquals(first, second)
    }

    @Test
    fun missingKeystoreEntryIsUnavailable() {
        manager().obtainKey()
        gateway.deleteKey()

        assertEquals(KeyResult.Unavailable, manager().obtainKey())
    }

    @Test
    fun tamperedKeyFileIsUnavailable() {
        manager().obtainKey()
        val file = File(tmp.root, "db_key.bin")
        val bytes = file.readBytes()
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0x01).toByte()
        file.writeBytes(bytes)

        assertEquals(KeyResult.Unavailable, manager().obtainKey())
    }

    @Test
    fun databaseWithoutKeyFileIsUnavailable() {
        File(tmp.root, "lemonflow.db").writeText("encrypted bytes")

        assertEquals(KeyResult.Unavailable, manager().obtainKey())
    }

    @Test
    fun wipeRemovesEverythingAndNextOpenCreatesNewKey() {
        val first = (manager().obtainKey() as KeyResult.Ready).passphrase.copyOf()
        File(tmp.root, "lemonflow.db").writeText("db")
        File(tmp.root, "lemonflow.db-wal").writeText("wal")

        manager().wipe()

        assertFalse(File(tmp.root, "db_key.bin").exists())
        assertFalse(File(tmp.root, "lemonflow.db").exists())
        assertFalse(File(tmp.root, "lemonflow.db-wal").exists())
        assertFalse(gateway.hasKey())
        val second = (manager().obtainKey() as KeyResult.Ready).passphrase
        assertFalse(first.contentEquals(second))
    }

    private fun ByteArray.containsSequence(needle: ByteArray): Boolean =
        (0..size - needle.size).any { start ->
            needle.indices.all { this[start + it] == needle[it] }
        }
}

/** In-memory stand-in for AndroidKeyStore using a real AES-GCM key, so tampering is detected. */
private class FakeKeyStoreGateway : KeyStoreGateway {
    private var key: SecretKey? = null

    override fun hasKey(): Boolean = key != null

    override fun createKey() {
        key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    }

    override fun deleteKey() {
        key = null
    }

    override fun wrap(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key ?: throw GeneralSecurityException("no key"))
        return cipher.iv + cipher.doFinal(plain)
    }

    override fun unwrap(blob: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            key ?: throw GeneralSecurityException("no key"),
            GCMParameterSpec(128, blob, 0, 12)
        )
        return cipher.doFinal(blob, 12, blob.size - 12)
    }
}
