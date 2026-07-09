package com.arthur.bgollee.synthetic

data class InterpolatedPoint(
    val timestampMs: Long,
    val stepIndex: Int,
    val rawValueMgDl: Double,
    val isAnchor: Boolean
)