package pl.cukrzycowy.ollee.glycemia

data class GlycemiaReading(
    val bg: String,
    val trend: String?,
    val delta: Double?,
    val timestamp: Long = System.currentTimeMillis()
)
