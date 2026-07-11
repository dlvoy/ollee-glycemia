package pl.cukrzycowy.ollee.glycemia

enum class WatchActivityState { ACTIVE, PAUSED, STOPPED }

data class PairedWatch(
    val address: String,
    val name: String,
    val isCustomName: Boolean = false,
    val lastSuccessfulSyncTimeMs: Long = 0L,
    val activityState: WatchActivityState = WatchActivityState.ACTIVE,
    val activityLabelSent: Boolean = false
)
