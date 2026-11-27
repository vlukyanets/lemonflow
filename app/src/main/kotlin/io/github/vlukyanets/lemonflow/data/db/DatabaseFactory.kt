package io.github.vlukyanets.lemonflow.data.db

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseFactory {
    private const val DATABASE_NAME = "lemonflow.db"

    /** Opens the SQLCipher-encrypted database. Does Keystore and disk work: call off the main thread. */
    fun create(context: Context): LemonFlowDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = DatabaseKeyStore(context).passphrase()
        return Room.databaseBuilder(context, LemonFlowDatabase::class.java, DATABASE_NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
    }
}
