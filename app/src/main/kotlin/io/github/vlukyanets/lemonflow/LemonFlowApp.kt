package io.github.vlukyanets.lemonflow

import android.app.Application
import io.github.vlukyanets.lemonflow.data.db.DatabaseFactory
import io.github.vlukyanets.lemonflow.data.db.LemonFlowDatabase

class LemonFlowApp : Application() {
    /** Opened on first use; touching it does Keystore and disk work, so never on the main thread. */
    val database: LemonFlowDatabase by lazy { DatabaseFactory.create(this) }
}
