package pl.cukrzycowy.ollee.glycemia.synthetic

data class SgvAnchor(
    val timestampMs: Long,
    val valueMgDl: Double,
    val deltaMgDlPer5Min: Double? = null
)