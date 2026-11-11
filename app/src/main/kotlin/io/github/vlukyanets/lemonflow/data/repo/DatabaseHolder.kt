package io.github.vlukyanets.lemonflow.data.repo

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import io.github.vlukyanets.lemonflow.data.crypto.DatabaseKeyManager
import io.github.vlukyanets.lemonflow.data.crypto.KeyResult
import io.github.vlukyanets.lemonflow.data.db.LemonFlowDatabase
import io.github.vlukyanets.lemonflow.domain.port.StorageGate
import io.github.vlukyanets.lemonflow.domain.port.StorageState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/** Gives repositories the open database; tests supply an in-memory one. */
fun interface DatabaseProvider {
    fun database(): LemonFlowDatabase
}

/**
 * Opens the SQLCipher-encrypted Room database (research R1, R2). Any key or decryption
 * failure becomes [StorageState.Unavailable]; nothing is deleted before [startFresh].
 */
class DatabaseHolder(
    private val context: Context,
    private val keyManager: DatabaseKeyManager,
    private val ioDispatcher: CoroutineDispatcher
) : StorageGate,
    DatabaseProvider {
    private val mutex = Mutex()

    @Volatile
    private var db: LemonFlowDatabase? = null

    override fun database(): LemonFlowDatabase = db ?: error("Storage is not open")

    override suspend fun open(): StorageState = mutex.withLock {
        withContext(ioDispatcher) { openLocked() }
    }

    override suspend fun startFresh(): StorageState = mutex.withLock {
        withContext(ioDispatcher) {
            db?.close()
            db = null
            keyManager.wipe()
            openLocked()
        }
    }

    private fun openLocked(): StorageState {
        if (db != null) return StorageState.Ready
        loadNativeLibrary()
        return when (val key = keyManager.obtainKey()) {
            is KeyResult.Ready -> openWith(key.passphrase)
            KeyResult.Unavailable -> StorageState.Unavailable
        }
    }

    private fun openWith(passphrase: ByteArray): StorageState {
        // SQLCipher keeps the passphrase for its connection pool, so the array is handed over
        // to the factory instead of being zeroed here.
        val database = Room.databaseBuilder(
            context,
            LemonFlowDatabase::class.java,
            LemonFlowDatabase.FILE_NAME
        )
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
        return try {
            // Opening is lazy; a wrong key only shows up on the first real access.
            database.openHelper.writableDatabase.query("SELECT count(*) FROM sqlite_master").close()
            db = database
            StorageState.Ready
        } catch (ignored: SQLiteException) {
            database.close()
            StorageState.Unavailable
        }
    }

    private companion object {
        @Volatile
        var nativeLoaded = false

        fun loadNativeLibrary() {
            if (!nativeLoaded) {
                System.loadLibrary("sqlcipher")
                nativeLoaded = true
            }
        }
    }
}
