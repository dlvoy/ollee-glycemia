package com.arthur.bgollee.synthetic

import kotlin.math.roundToInt
import kotlin.random.Random

class SgvSyntheticGenerator(
    private val normalizer: SgvAnchorNormalizer = SgvAnchorNormalizer(),
    private val deltaEstimator: SgvDeltaEstimator = SgvDeltaEstimator(),
    private val interpolator: HermiteInterpolator = HermiteInterpolator(),
    private val noiseGenerator: SgvNoiseGenerator = SgvNoiseGenerator(),
    private val constraintProcessor: SgvConstraintProcessor = SgvConstraintProcessor()
) {

    fun generate(
        anchors: List<SgvAnchor>,
        config: SgvGenerationConfig = SgvGenerationConfig()
    ): List<SgvSample> {
        val random = config.randomSeed?.let { Random(it) } ?: Random.Default
        val normalizedAnchors = normalizer.normalize(anchors, config)
        val anchorsWithDeltas = deltaEstimator.estimate(normalizedAnchors, config, random)
        val interpolatedPoints = interpolator.interpolate(anchorsWithDeltas, config)
        val noises = noiseGenerator.generate(
            points = interpolatedPoints,
            anchorSteps = anchorsWithDeltas.map { it.stepIndex }.toSet(),
            config = config,
            random = random
        )
        val constrained = constraintProcessor.apply(interpolatedPoints, noises, config)

        return if (config.finalSmoothingEnabled) {
            applyFinalSmoothing(constrained)
        } else {
            constrained
        }
    }

    private fun applyFinalSmoothing(samples: List<SgvSample>): List<SgvSample> {
        if (samples.size < 3) return samples

        return samples.mapIndexed { index, sample ->
            if (sample.isAnchor || index == 0 || index == samples.lastIndex) {
                sample
            } else {
                val smoothed = 0.25 * samples[index - 1].rawValueMgDl +
                    0.5 * sample.rawValueMgDl +
                    0.25 * samples[index + 1].rawValueMgDl
                sample.copy(
                    valueMgDl = smoothed.roundToInt(),
                    rawValueMgDl = smoothed,
                    deltaMgDlPer5Min = smoothed - samples[index - 1].rawValueMgDl
                )
            }
        }
    }
}