package com.arthur.bgollee.synthetic

data class SgvSample(
    val timestampMs: Long,
    val valueMgDl: Int,
    val rawValueMgDl: Double,
    val deltaMgDlPer5Min: Double,
    val isAnchor: Boolean
)