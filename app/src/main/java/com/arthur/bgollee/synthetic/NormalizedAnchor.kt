package com.arthur.bgollee.synthetic

data class NormalizedAnchor(
    val timestampMs: Long,
    val stepIndex: Int,
    val valueMgDl: Double,
    val deltaMgDlPer5Min: Double?
)