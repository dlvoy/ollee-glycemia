package com.arthur.bgollee.synthetic

import kotlin.random.Random

class SgvDeltaEstimator {

    fun estimate(
        anchors: List<NormalizedAnchor>,
        config: SgvGenerationConfig,
        random: Random
    ): List<NormalizedAnchor> {
        require(anchors.size >= 2) { "At least two anchors are required" }

        val segmentSlopes = anchors.zipWithNext { start, end ->
            (end.valueMgDl - start.valueMgDl) / (end.stepIndex - start.stepIndex).toDouble()
        }

        return anchors.mapIndexed { index, anchor ->
            val estimatedDelta = anchor.deltaMgDlPer5Min ?: estimateMissingDelta(index, segmentSlopes)
            val randomizedDelta = if (anchor.deltaMgDlPer5Min == null && config.randomizeEstimatedDeltas) {
                estimatedDelta * random.nextDouble(
                    from = config.deltaRandomizationMinFactor,
                    until = config.deltaRandomizationMaxFactor
                )
            } else {
                estimatedDelta
            }

            anchor.copy(
                deltaMgDlPer5Min = randomizedDelta.coerceIn(
                    -config.maxFallMgDlPer5Min,
                    config.maxRiseMgDlPer5Min
                )
            )
        }
    }

    private fun estimateMissingDelta(index: Int, segmentSlopes: List<Double>): Double {
        if (index == 0) return segmentSlopes.first()
        if (index == segmentSlopes.size) return segmentSlopes.last()

        val previousSlope = segmentSlopes[index - 1]
        val nextSlope = segmentSlopes[index]

        return if (previousSlope == 0.0 || nextSlope == 0.0 || previousSlope * nextSlope < 0.0) {
            0.0
        } else {
            (previousSlope + nextSlope) / 2.0
        }
    }
}