package pl.cukrzycowy.ollee.glycemia

data class GlycemiaHistoryEntry(
    val timestampMs: Long,
    val valueMgDl: Int,
    val delta: Double
)