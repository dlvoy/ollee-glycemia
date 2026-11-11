package pl.cukrzycowy.ollee.glycemia

enum class WatchActivityState { ACTIVE, PAUSED, STOPPED }

data class PairedWatch(
    val address: String,
    val name: String,
    val isCustomName: Boolean = false,
    val lastSuccessfulSyncTimeMs: Long = 0L,
    val activityState: WatchActivityState = WatchActivityState.ACTIVE,
    val activityLabelSent: Boolean = false,
    /** Last known: the watch exposed the Forge BLE service on its previous connection. */
    val forgeSupported: Boolean = false,
    /** Last known: the glycemia watchface answered the handshake (set by spec 16's handshake). */
    val faceDetected: Boolean = false
)
