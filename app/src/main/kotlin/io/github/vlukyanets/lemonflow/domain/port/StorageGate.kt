package io.github.vlukyanets.lemonflow.domain.port

enum class StorageState { Ready, Unavailable }

/** Opens the encrypted store (research R2). Nothing is erased without [startFresh]. */
interface StorageGate {
    suspend fun open(): StorageState

    /** Only after the user confirmed (FR-015): erases the unreadable data and the old key. */
    suspend fun startFresh(): StorageState
}
