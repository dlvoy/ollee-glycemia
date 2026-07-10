package pl.cukrzycowy.ollee.glycemia.synthetic

data class SgvGenerationConfig(
    val sampleIntervalMinutes: Int = 5,
    val minSgvMgDl: Double = 40.0,
    val maxSgvMgDl: Double = 400.0,
    val maxRiseMgDlPer5Min: Double = 8.0,
    val maxFallMgDlPer5Min: Double = 8.0,
    val maxDeltaChangePerStep: Double = 3.0,
    val noiseEnabled: Boolean = true,
    val noisePersistence: Double = 0.92,
    val noiseVolatilityMgDl: Double = 0.8,
    val maxNoiseMgDl: Double = 5.0,
    val preserveAnchors: Boolean = true,
    val anchorNoiseSuppressionRadiusSteps: Int = 3,
    val randomizeEstimatedDeltas: Boolean = true,
    val deltaRandomizationMinFactor: Double = 0.75,
    val deltaRandomizationMaxFactor: Double = 1.25,
    val finalSmoothingEnabled: Boolean = false,
    val randomSeed: Long? = null
)