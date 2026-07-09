package com.arthur.bgollee.synthetic

class HermiteInterpolator {

    fun interpolate(
        anchors: List<NormalizedAnchor>,
        config: SgvGenerationConfig
    ): List<InterpolatedPoint> {
        val stepMillis = config.sampleIntervalMinutes * 60_000L
        val points = mutableListOf<InterpolatedPoint>()
        val firstTimestamp = anchors.first().timestampMs

        anchors.zipWithNext().forEachIndexed { segmentIndex, (start, end) ->
            val duration = end.stepIndex - start.stepIndex
            require(duration > 0) { "Segment duration must be positive" }

            val rangeStart = if (segmentIndex == 0) start.stepIndex else start.stepIndex + 1
            for (stepIndex in rangeStart..end.stepIndex) {
                val t = (stepIndex - start.stepIndex) / duration.toDouble()
                val rawValue = hermiteValue(
                    t = t,
                    yA = start.valueMgDl,
                    yB = end.valueMgDl,
                    mA = start.deltaMgDlPer5Min ?: 0.0,
                    mB = end.deltaMgDlPer5Min ?: 0.0,
                    duration = duration.toDouble()
                )

                points += InterpolatedPoint(
                    timestampMs = firstTimestamp + stepIndex * stepMillis,
                    stepIndex = stepIndex,
                    rawValueMgDl = rawValue,
                    isAnchor = stepIndex == start.stepIndex || stepIndex == end.stepIndex
                )
            }
        }

        return points
    }

    private fun hermiteValue(
        t: Double,
        yA: Double,
        yB: Double,
        mA: Double,
        mB: Double,
        duration: Double
    ): Double {
        val h00 = 2 * t * t * t - 3 * t * t + 1
        val h10 = t * t * t - 2 * t * t + t
        val h01 = -2 * t * t * t + 3 * t * t
        val h11 = t * t * t - t * t

        return h00 * yA + h10 * duration * mA + h01 * yB + h11 * duration * mB
    }
}