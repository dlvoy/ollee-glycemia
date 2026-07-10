package com.arthur.bgollee

enum class WatchConnState {
    CONNECTING,
    SYNCED,
    OFFLINE,
    ERROR
}

data class WatchStatus(
    val watch: PairedWatch,
    val state: WatchConnState,
    val lastSyncTimeMs: Long = 0L,
    val lastConnectionAttemptTimeMs: Long = 0L
)
