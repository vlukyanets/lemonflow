package io.github.vlukyanets.lemonflow.data.crypto

import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeyStoreGatewayTest {
    @Test
    fun wrapKeyIsNotBoundToUserAuthentication() {
        val spec = AndroidKeyStoreGateway.wrapKeySpec()

        // FR-013a: a lock-screen change must never invalidate the key.
        assertFalse(spec.isUserAuthenticationRequired)
        assertEquals("lemonflow_db_wrap", spec.keystoreAlias)
        assertEquals(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT, spec.purposes)
        assertArrayEquals(arrayOf(KeyProperties.BLOCK_MODE_GCM), spec.blockModes)
        assertArrayEquals(arrayOf(KeyProperties.ENCRYPTION_PADDING_NONE), spec.encryptionPaddings)
        assertEquals(256, spec.keySize)
    }
}
