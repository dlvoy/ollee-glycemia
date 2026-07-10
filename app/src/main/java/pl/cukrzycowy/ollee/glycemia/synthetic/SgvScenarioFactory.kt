package pl.cukrzycowy.ollee.glycemia.synthetic

import kotlin.random.Random

class SgvScenarioFactory {

    data class InitialState(
        val valueMgDl: Double,
        val deltaMgDlPer5Min: Double
    )

    enum class ScenarioType {
        FASTING_STABLE,
        POST_MEAL_RISE,
        OVERNIGHT_DRIFT,
        HYPO_RECOVERY
    }

    fun createAnchors(
        startTimestampMs: Long,
        scenarioType: ScenarioType,
        random: Random,
        initialState: InitialState? = null
    ): List<SgvAnchor> {
        return when (scenarioType) {
            ScenarioType.FASTING_STABLE -> createFastingStable(startTimestampMs, random, initialState)
            ScenarioType.POST_MEAL_RISE -> createPostMealRise(startTimestampMs, random, initialState)
            ScenarioType.OVERNIGHT_DRIFT -> createOvernightDrift(startTimestampMs, random, initialState)
            ScenarioType.HYPO_RECOVERY -> createHypoRecovery(startTimestampMs, random, initialState)
        }
    }

    private fun createFastingStable(
        startTimestampMs: Long,
        random: Random,
        initialState: InitialState?
    ): List<SgvAnchor> {
        val base = initialState?.valueMgDl ?: random.nextInt(95, 121).toDouble()
        return listOf(
            SgvAnchor(startTimestampMs, base, initialState?.deltaMgDlPer5Min ?: 0.0),
            SgvAnchor(startTimestampMs + 60.minutesMs, base + random.nextDouble(-6.0, 6.0), 0.5),
            SgvAnchor(startTimestampMs + 120.minutesMs, base + random.nextDouble(-10.0, 10.0), 0.0),
            SgvAnchor(startTimestampMs + 180.minutesMs, base + random.nextDouble(-6.0, 6.0), -0.5),
            SgvAnchor(startTimestampMs + 240.minutesMs, base + random.nextDouble(-4.0, 4.0), 0.0)
        )
    }

    private fun createPostMealRise(
        startTimestampMs: Long,
        random: Random,
        initialState: InitialState?
    ): List<SgvAnchor> {
        val start = initialState?.valueMgDl ?: random.nextInt(90, 126).toDouble()
        val riseMagnitude = when {
            start < 90.0 -> random.nextDouble(55.0, 110.0)
            start < 140.0 -> random.nextDouble(35.0, 90.0)
            else -> random.nextDouble(15.0, 55.0)
        }
        val peak = (start + riseMagnitude).coerceAtMost(240.0)
        val recovery = (start + random.nextDouble(0.0, 25.0)).coerceAtMost(160.0)
        return listOf(
            SgvAnchor(startTimestampMs, start, initialState?.deltaMgDlPer5Min ?: 1.0),
            SgvAnchor(startTimestampMs + 45.minutesMs, start + (peak - start) * 0.45, 4.0),
            SgvAnchor(startTimestampMs + 90.minutesMs, peak, 0.0),
            SgvAnchor(startTimestampMs + 180.minutesMs, peak - (peak - recovery) * 0.55, -3.0),
            SgvAnchor(startTimestampMs + 240.minutesMs, recovery, -1.0)
        )
    }

    private fun createOvernightDrift(
        startTimestampMs: Long,
        random: Random,
        initialState: InitialState?
    ): List<SgvAnchor> {
        val start = initialState?.valueMgDl ?: random.nextInt(100, 141).toDouble()
        val drift = when {
            start < 85.0 -> random.nextDouble(8.0, 35.0)
            start > 160.0 -> random.nextDouble(-35.0, -8.0)
            else -> random.nextDouble(-18.0, 18.0)
        }
        return listOf(
            SgvAnchor(startTimestampMs, start, initialState?.deltaMgDlPer5Min ?: 0.0),
            SgvAnchor(startTimestampMs + 90.minutesMs, start + drift * 0.33, 0.5),
            SgvAnchor(startTimestampMs + 180.minutesMs, start + drift * 0.66, 0.0),
            SgvAnchor(startTimestampMs + 240.minutesMs, start + drift, 0.0)
        )
    }

    private fun createHypoRecovery(
        startTimestampMs: Long,
        random: Random,
        initialState: InitialState?
    ): List<SgvAnchor> {
        val start = (initialState?.valueMgDl ?: random.nextInt(55, 71).toDouble()).coerceAtMost(80.0)
        val recovered = maxOf(start + 20.0, random.nextInt(95, 131).toDouble())
        return listOf(
            SgvAnchor(startTimestampMs, start, initialState?.deltaMgDlPer5Min ?: 2.0),
            SgvAnchor(startTimestampMs + 30.minutesMs, start + (recovered - start) * 0.45, 5.0),
            SgvAnchor(startTimestampMs + 60.minutesMs, recovered, 1.0),
            SgvAnchor(startTimestampMs + 120.minutesMs, recovered + random.nextDouble(-10.0, 10.0), 0.0)
        )
    }

    private val Int.minutesMs: Long
        get() = this * 60_000L
}