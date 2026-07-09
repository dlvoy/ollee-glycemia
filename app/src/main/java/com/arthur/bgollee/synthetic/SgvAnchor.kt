package com.arthur.bgollee.synthetic

data class SgvAnchor(
    val timestampMs: Long,
    val valueMgDl: Double,
    val deltaMgDlPer5Min: Double? = null
)