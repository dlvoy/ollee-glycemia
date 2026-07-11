package pl.cukrzycowy.ollee.glycemia

data class PairedWatch(
    val address: String,
    val name: String,
    val isCustomName: Boolean = false,
    val lastSuccessfulSyncTimeMs: Long = 0L
)
