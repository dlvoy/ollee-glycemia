package pl.cukrzycowy.ollee.glycemia.synthetic

import kotlin.math.roundToInt

class SgvConstraintProcessor {

    fun apply(
        points: List<InterpolatedPoint>,
        noises: DoubleArray,
        config: SgvGenerationConfig
    ): List<SgvSample> {
        require(points.size == noises.size) { "Points and noise arrays must have the same size" }

        val samples = mutableListOf<SgvSample>()
        var previousValue: Double? = null
        var previousDelta: Double? = null

        points.forEachIndexed { index, point ->
            var candidate = (point.rawValueMgDl + noises[index]).coerceIn(config.minSgvMgDl, config.maxSgvMgDl)

            if (config.preserveAnchors && point.isAnchor) {
                candidate = point.rawValueMgDl.coerceIn(config.minSgvMgDl, config.maxSgvMgDl)
            }

            val delta = if (previousValue == null) {
                0.0
            } else {
                var boundedDelta = (candidate - previousValue!!).coerceIn(
                    -config.maxFallMgDlPer5Min,
                    config.maxRiseMgDlPer5Min
                )

                if (previousDelta != null) {
                    boundedDelta = boundedDelta.coerceIn(
                        previousDelta!! - config.maxDeltaChangePerStep,
                        previousDelta!! + config.maxDeltaChangePerStep
                    )
                }

                if (config.preserveAnchors && point.isAnchor) {
                    candidate = point.rawValueMgDl.coerceIn(config.minSgvMgDl, config.maxSgvMgDl)
                    candidate - previousValue!!
                } else {
                    candidate = previousValue!! + boundedDelta
                    boundedDelta
                }
            }

            val clampedCandidate = candidate.coerceIn(config.minSgvMgDl, config.maxSgvMgDl)
            samples += SgvSample(
                timestampMs = point.timestampMs,
                valueMgDl = clampedCandidate.roundToInt(),
                rawValueMgDl = clampedCandidate,
                deltaMgDlPer5Min = delta,
                isAnchor = point.isAnchor
            )

            previousValue = clampedCandidate
            previousDelta = delta
        }

        return samples
    }
}