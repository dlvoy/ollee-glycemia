package com.arthur.bgollee

data class GlycemiaReading(
    val bg: String,
    val trend: String?,
    val delta: Double?,
    val timestamp: Long = System.currentTimeMillis()
)
