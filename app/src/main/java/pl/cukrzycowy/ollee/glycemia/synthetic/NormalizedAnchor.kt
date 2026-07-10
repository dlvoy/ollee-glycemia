package pl.cukrzycowy.ollee.glycemia.synthetic

data class NormalizedAnchor(
    val timestampMs: Long,
    val stepIndex: Int,
    val valueMgDl: Double,
    val deltaMgDlPer5Min: Double?
)