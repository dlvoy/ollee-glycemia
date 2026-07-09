package com.arthur.bgollee.synthetic

import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.random.Random

class SgvNoiseGenerator {

    fun generate(
        points: List<InterpolatedPoint>,
        anchorSteps: Set<Int>,
        config: SgvGenerationConfig,
        random: Random
    ): DoubleArray {
        if (!config.noiseEnabled) return DoubleArray(points.size)

        val noiseValues = DoubleArray(points.size)
        var currentNoise = 0.0

        points.forEachIndexed { index, point ->
            currentNoise = currentNoise * config.noisePersistence + random.nextGaussianLike() * config.noiseVolatilityMgDl
            currentNoise = currentNoise.coerceIn(-config.maxNoiseMgDl, config.maxNoiseMgDl)

            val distanceToAnchor = anchorSteps.minOf { anchorStep ->
                (anchorStep - point.stepIndex).absoluteValue
            }
            val suppressionRadius = config.anchorNoiseSuppressionRadiusSteps.coerceAtLeast(1)
            val scale = min(1.0, distanceToAnchor / suppressionRadius.toDouble())

            noiseValues[index] = if (point.isAnchor) 0.0 else currentNoise * scale
        }

        return noiseValues
    }

    private fun Random.nextGaussianLike(): Double {
        val first = nextDouble().coerceAtLeast(1e-9)
        val second = nextDouble()
        return kotlin.math.sqrt(-2.0 * kotlin.math.ln(first)) * kotlin.math.cos(2.0 * Math.PI * second)
    }
}