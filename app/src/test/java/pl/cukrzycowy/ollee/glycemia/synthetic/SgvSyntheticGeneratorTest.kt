package pl.cukrzycowy.ollee.glycemia.synthetic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SgvSyntheticGeneratorTest {

    @Test
    fun deltaEstimator_setsZeroDeltaAtLocalPeak() {
        val config = SgvGenerationConfig(randomizeEstimatedDeltas = false)
        val anchors = SgvAnchorNormalizer().normalize(
            anchors = listOf(
                SgvAnchor(timestampMs = 0L, valueMgDl = 100.0),
                SgvAnchor(timestampMs = 60.minutesMs, valueMgDl = 150.0),
                SgvAnchor(timestampMs = 120.minutesMs, valueMgDl = 110.0)
            ),
            config = config
        )

        val estimated = SgvDeltaEstimator().estimate(anchors, config, kotlin.random.Random(42))

        assertEquals(0.0, estimated[1].deltaMgDlPer5Min ?: Double.NaN, 0.0001)
    }

    @Test
    fun hermiteInterpolator_createsCurvedSegment() {
        val points = HermiteInterpolator().interpolate(
            anchors = listOf(
                NormalizedAnchor(timestampMs = 0L, stepIndex = 0, valueMgDl = 100.0, deltaMgDlPer5Min = 4.0),
                NormalizedAnchor(timestampMs = 60.minutesMs, stepIndex = 12, valueMgDl = 140.0, deltaMgDlPer5Min = -2.0)
            ),
            config = SgvGenerationConfig(noiseEnabled = false, randomizeEstimatedDeltas = false)
        )

        val midpoint = points.first { it.stepIndex == 6 }

        assertEquals(100.0, points.first().rawValueMgDl, 0.0001)
        assertEquals(140.0, points.last().rawValueMgDl, 0.0001)
        assertNotEquals(120.0, midpoint.rawValueMgDl, 0.0001)
    }

    @Test
    fun deltaEstimator_estimatesBoundaryAndMiddleSlopes() {
        val config = SgvGenerationConfig(randomizeEstimatedDeltas = false)
        val anchors = SgvAnchorNormalizer().normalize(
            anchors = listOf(
                SgvAnchor(timestampMs = 0L, valueMgDl = 100.0),
                SgvAnchor(timestampMs = 60.minutesMs, valueMgDl = 124.0),
                SgvAnchor(timestampMs = 120.minutesMs, valueMgDl = 148.0)
            ),
            config = config
        )

        val estimated = SgvDeltaEstimator().estimate(anchors, config, Random(7))

        assertEquals(2.0, estimated[0].deltaMgDlPer5Min ?: Double.NaN, 0.0001)
        assertEquals(2.0, estimated[1].deltaMgDlPer5Min ?: Double.NaN, 0.0001)
        assertEquals(2.0, estimated[2].deltaMgDlPer5Min ?: Double.NaN, 0.0001)
    }

    @Test
    fun generator_preservesAnchorsAndSpacing() {
        val config = SgvGenerationConfig(
            noiseEnabled = false,
            randomizeEstimatedDeltas = false,
            preserveAnchors = true,
            randomSeed = 7L
        )
        val anchors = listOf(
            SgvAnchor(timestampMs = 0L, valueMgDl = 100.0, deltaMgDlPer5Min = 1.0),
            SgvAnchor(timestampMs = 60.minutesMs, valueMgDl = 145.0, deltaMgDlPer5Min = 2.0),
            SgvAnchor(timestampMs = 120.minutesMs, valueMgDl = 130.0, deltaMgDlPer5Min = -1.0)
        )

        val samples = SgvSyntheticGenerator().generate(anchors, config)

        assertEquals(25, samples.size)
        assertEquals(100.0, samples.first().rawValueMgDl, 0.0001)
        assertEquals(145.0, samples.first { it.timestampMs == 60.minutesMs }.rawValueMgDl, 0.0001)
        assertEquals(130.0, samples.last().rawValueMgDl, 0.0001)
        assertTrue(samples.zipWithNext().all { (left, right) -> right.timestampMs - left.timestampMs == 5.minutesMs })
    }

    @Test
    fun generator_isDeterministicForSameSeed() {
        val config = SgvGenerationConfig(randomSeed = 42L)
        val anchors = listOf(
            SgvAnchor(timestampMs = 0L, valueMgDl = 100.0),
            SgvAnchor(timestampMs = 60.minutesMs, valueMgDl = 150.0),
            SgvAnchor(timestampMs = 120.minutesMs, valueMgDl = 120.0)
        )

        val first = SgvSyntheticGenerator().generate(anchors, config)
        val second = SgvSyntheticGenerator().generate(anchors, config)

        assertEquals(first, second)
    }

    @Test
    fun constraintProcessor_limitsStepDelta() {
        val samples = SgvConstraintProcessor().apply(
            points = listOf(
                InterpolatedPoint(timestampMs = 0L, stepIndex = 0, rawValueMgDl = 100.0, isAnchor = false),
                InterpolatedPoint(timestampMs = 5.minutesMs, stepIndex = 1, rawValueMgDl = 130.0, isAnchor = false),
                InterpolatedPoint(timestampMs = 10.minutesMs, stepIndex = 2, rawValueMgDl = 70.0, isAnchor = false)
            ),
            noises = doubleArrayOf(0.0, 0.0, 0.0),
            config = SgvGenerationConfig(
                maxRiseMgDlPer5Min = 8.0,
                maxFallMgDlPer5Min = 8.0,
                preserveAnchors = false,
                noiseEnabled = false
            )
        )

        assertTrue(samples[1].rawValueMgDl - samples[0].rawValueMgDl <= 8.0001)
        assertTrue(samples[2].rawValueMgDl - samples[1].rawValueMgDl >= -8.0001)
    }

    @Test
    fun constraintProcessor_clampsRangeAndPreservesAnchors() {
        val samples = SgvConstraintProcessor().apply(
            points = listOf(
                InterpolatedPoint(timestampMs = 0L, stepIndex = 0, rawValueMgDl = 35.0, isAnchor = true),
                InterpolatedPoint(timestampMs = 5.minutesMs, stepIndex = 1, rawValueMgDl = 420.0, isAnchor = false),
                InterpolatedPoint(timestampMs = 10.minutesMs, stepIndex = 2, rawValueMgDl = 390.0, isAnchor = true)
            ),
            noises = doubleArrayOf(0.0, 25.0, 0.0),
            config = SgvGenerationConfig(
                minSgvMgDl = 40.0,
                maxSgvMgDl = 400.0,
                maxRiseMgDlPer5Min = 20.0,
                maxFallMgDlPer5Min = 20.0,
                preserveAnchors = true,
                noiseEnabled = true
            )
        )

        assertEquals(40.0, samples.first().rawValueMgDl, 0.0001)
        assertTrue(samples[1].rawValueMgDl <= 400.0)
        assertEquals(390.0, samples.last().rawValueMgDl, 0.0001)
    }

    @Test
    fun scenarioFactory_preservesInitialStateAtBoundary() {
        val anchors = SgvScenarioFactory().createAnchors(
            startTimestampMs = 5.minutesMs,
            scenarioType = SgvScenarioFactory.ScenarioType.OVERNIGHT_DRIFT,
            random = Random(42),
            initialState = SgvScenarioFactory.InitialState(
                valueMgDl = 178.0,
                deltaMgDlPer5Min = -1.5
            )
        )

        assertEquals(5.minutesMs, anchors.first().timestampMs)
        assertEquals(178.0, anchors.first().valueMgDl, 0.0001)
        assertEquals(-1.5, anchors.first().deltaMgDlPer5Min ?: Double.NaN, 0.0001)
        assertTrue(anchors.last().valueMgDl < anchors.first().valueMgDl)
    }

    @Test
    fun generator_endToEnd_staysWithinConstraintsAndRemainsSmooth() {
        val config = SgvGenerationConfig(
            randomSeed = 123L,
            noiseEnabled = true,
            preserveAnchors = true,
            maxRiseMgDlPer5Min = 8.0,
            maxFallMgDlPer5Min = 8.0,
            maxDeltaChangePerStep = 3.0
        )
        val anchors = listOf(
            SgvAnchor(timestampMs = 0L, valueMgDl = 100.0, deltaMgDlPer5Min = 1.0),
            SgvAnchor(timestampMs = 60.minutesMs, valueMgDl = 145.0, deltaMgDlPer5Min = 2.0),
            SgvAnchor(timestampMs = 120.minutesMs, valueMgDl = 130.0, deltaMgDlPer5Min = -1.0),
            SgvAnchor(timestampMs = 210.minutesMs, valueMgDl = 180.0, deltaMgDlPer5Min = 0.0),
            SgvAnchor(timestampMs = 300.minutesMs, valueMgDl = 115.0, deltaMgDlPer5Min = -2.0)
        )

        val samples = SgvSyntheticGenerator().generate(anchors, config)

        assertTrue(samples.zipWithNext().all { (left, right) -> right.timestampMs - left.timestampMs == 5.minutesMs })
        assertTrue(samples.all { it.rawValueMgDl in 40.0..400.0 })
        assertEquals(100.0, samples.first { it.timestampMs == 0L }.rawValueMgDl, 0.0001)
        assertEquals(145.0, samples.first { it.timestampMs == 60.minutesMs }.rawValueMgDl, 0.0001)
        assertEquals(130.0, samples.first { it.timestampMs == 120.minutesMs }.rawValueMgDl, 0.0001)
        assertEquals(180.0, samples.first { it.timestampMs == 210.minutesMs }.rawValueMgDl, 0.0001)
        assertEquals(115.0, samples.first { it.timestampMs == 300.minutesMs }.rawValueMgDl, 0.0001)
        assertTrue(samples.zipWithNext().all { (left, right) -> right.rawValueMgDl - left.rawValueMgDl <= 8.0001 })
        assertTrue(samples.zipWithNext().all { (left, right) -> right.rawValueMgDl - left.rawValueMgDl >= -8.0001 })
        assertTrue(
            samples.zipWithNext().zipWithNext().all { (firstPair, secondPair) ->
                val firstDelta = firstPair.second.rawValueMgDl - firstPair.first.rawValueMgDl
                val secondDelta = secondPair.second.rawValueMgDl - secondPair.first.rawValueMgDl
                kotlin.math.abs(secondDelta - firstDelta) <= 3.0001
            }
        )
    }

    private val Int.minutesMs: Long
        get() = this * 60_000L
}