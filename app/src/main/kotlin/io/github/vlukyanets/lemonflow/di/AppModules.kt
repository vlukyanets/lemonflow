package io.github.vlukyanets.lemonflow.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vlukyanets.lemonflow.data.crypto.AndroidKeyStoreGateway
import io.github.vlukyanets.lemonflow.data.crypto.DatabaseKeyManager
import io.github.vlukyanets.lemonflow.data.db.LemonFlowDatabase
import io.github.vlukyanets.lemonflow.data.repo.DatabaseHolder
import io.github.vlukyanets.lemonflow.data.repo.DatabaseProvider
import io.github.vlukyanets.lemonflow.domain.port.StorageGate
import java.io.File
import java.security.SecureRandom
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

/** The only source of "now" for the app (constitution IV). */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun clock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    private const val KEY_FILE = "db_key.bin"

    @Provides
    @Singleton
    fun databaseHolder(@ApplicationContext context: Context): DatabaseHolder {
        val dbFile = context.getDatabasePath(LemonFlowDatabase.FILE_NAME)
        val keyManager = DatabaseKeyManager(
            gateway = AndroidKeyStoreGateway(),
            keyFile = File(context.noBackupFilesDir, KEY_FILE),
            databaseFiles = {
                listOf("", "-wal", "-shm", "-journal").map { File(dbFile.path + it) }
            },
            random = SecureRandom()
        )
        return DatabaseHolder(context, keyManager, Dispatchers.IO)
    }

    @Provides
    fun storageGate(holder: DatabaseHolder): StorageGate = holder

    @Provides
    fun databaseProvider(holder: DatabaseHolder): DatabaseProvider = holder
}
