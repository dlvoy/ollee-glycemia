package com.arthur.bgollee

data class GlycemiaHistoryEntry(
    val timestampMs: Long,
    val valueMgDl: Int,
    val delta: Double
)