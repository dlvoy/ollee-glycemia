package com.arthur.bgollee

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.arthur.bgollee.synthetic.SgvGenerationConfig
import com.arthur.bgollee.synthetic.SgvSample
import com.arthur.bgollee.synthetic.SgvScenarioFactory
import com.arthur.bgollee.synthetic.SgvSyntheticGenerator
import kotlin.math.abs
import kotlin.random.Random

class VirtualHumanProvider : ConfigurableGlycemiaProvider {

    override val id: String = "virtual_human"
    override val displayName: String = "Wirtualny pacjent"

    private val handler = Handler(Looper.getMainLooper())
    private val generator = SgvSyntheticGenerator()
    private val scenarioFactory = SgvScenarioFactory()

    private var callback: ((GlycemiaReading) -> Unit)? = null
    private var random: Random = Random(System.currentTimeMillis())
    private var currentSamples: List<SgvSample> = emptyList()
    private var currentIndex: Int = 0
    private var lastEmittedSample: SgvSample? = null
    private var configuredScenario: String = SCENARIO_AUTO
    private var configuredSeed: Long? = null

    private val emissionRunnable = object : Runnable {
        override fun run() {
            emitNextReading()
            handler.postDelayed(this, SAMPLE_INTERVAL_MS)
        }
    }

    override fun start(context: Context, onReading: (GlycemiaReading) -> Unit) {
        callback = onReading
        val config = getSavedConfig(context)
        configuredScenario = config[KEY_SCENARIO] ?: SCENARIO_AUTO
        configuredSeed = config[KEY_SEED]?.toLongOrNull()
        random = configuredSeed?.let { Random(it) } ?: Random(System.currentTimeMillis())
        currentSamples = emptyList()
        currentIndex = 0
        lastEmittedSample = null

        handler.removeCallbacksAndMessages(null)
        emitNextReading()
        handler.postDelayed(emissionRunnable, SAMPLE_INTERVAL_MS)
    }

    override fun stop(context: Context) {
        handler.removeCallbacksAndMessages(null)
        callback = null
        currentSamples = emptyList()
        currentIndex = 0
        lastEmittedSample = null
    }

    override fun getConfigSpec(context: Context): ProviderConfigSpec {
        return ProviderConfigSpec(
            title = context.getString(R.string.provider_virtual_human_config_title),
            fields = listOf(
                ProviderConfigField(
                    key = KEY_SCENARIO,
                    label = context.getString(R.string.provider_virtual_human_scenario_label),
                    type = ProviderConfigField.FieldType.CHOICE,
                    defaultValue = SCENARIO_AUTO,
                    options = listOf(
                        ProviderConfigOption(SCENARIO_AUTO, context.getString(R.string.provider_virtual_human_scenario_auto)),
                        ProviderConfigOption(SgvScenarioFactory.ScenarioType.FASTING_STABLE.name, context.getString(R.string.provider_virtual_human_scenario_fasting)),
                        ProviderConfigOption(SgvScenarioFactory.ScenarioType.POST_MEAL_RISE.name, context.getString(R.string.provider_virtual_human_scenario_post_meal)),
                        ProviderConfigOption(SgvScenarioFactory.ScenarioType.OVERNIGHT_DRIFT.name, context.getString(R.string.provider_virtual_human_scenario_overnight)),
                        ProviderConfigOption(SgvScenarioFactory.ScenarioType.HYPO_RECOVERY.name, context.getString(R.string.provider_virtual_human_scenario_hypo))
                    )
                ),
                ProviderConfigField(
                    key = KEY_SEED,
                    label = context.getString(R.string.provider_virtual_human_seed_label),
                    type = ProviderConfigField.FieldType.LONG,
                    defaultValue = "",
                    optional = true,
                    helperText = context.getString(R.string.provider_virtual_human_seed_helper)
                )
            )
        )
    }

    override fun getSavedConfig(context: Context): Map<String, String> {
        return ProviderConfigStore.read(context, id, getConfigSpec(context))
    }

    override fun saveConfig(context: Context, values: Map<String, String>) {
        val scenario = values[KEY_SCENARIO]
            ?.takeIf { candidate ->
                candidate == SCENARIO_AUTO || SgvScenarioFactory.ScenarioType.entries.any { it.name == candidate }
            }
            ?: SCENARIO_AUTO
        val seed = values[KEY_SEED]?.trim().orEmpty()
        ProviderConfigStore.write(
            context,
            id,
            mapOf(
                KEY_SCENARIO to scenario,
                KEY_SEED to seed
            )
        )
    }

    override fun getConfigSummary(context: Context): String {
        val config = getSavedConfig(context)
        val scenarioLabel = when (config[KEY_SCENARIO] ?: SCENARIO_AUTO) {
            SgvScenarioFactory.ScenarioType.FASTING_STABLE.name -> context.getString(R.string.provider_virtual_human_scenario_fasting)
            SgvScenarioFactory.ScenarioType.POST_MEAL_RISE.name -> context.getString(R.string.provider_virtual_human_scenario_post_meal)
            SgvScenarioFactory.ScenarioType.OVERNIGHT_DRIFT.name -> context.getString(R.string.provider_virtual_human_scenario_overnight)
            SgvScenarioFactory.ScenarioType.HYPO_RECOVERY.name -> context.getString(R.string.provider_virtual_human_scenario_hypo)
            else -> context.getString(R.string.provider_virtual_human_scenario_auto)
        }
        val seed = config[KEY_SEED]?.trim().orEmpty()
        val seedLabel = if (seed.isEmpty()) {
            context.getString(R.string.provider_virtual_human_seed_random)
        } else {
            seed
        }
        return context.getString(R.string.provider_virtual_human_summary_format, scenarioLabel, seedLabel)
    }

    private fun emitNextReading() {
        if (callback == null) return

        if (currentSamples.isEmpty() || currentIndex >= currentSamples.size) {
            currentSamples = generateSamples()
            currentIndex = 0
        }

        val sample = currentSamples[currentIndex]
        currentIndex += 1
        lastEmittedSample = sample

        callback?.invoke(
            GlycemiaReading(
                bg = sample.valueMgDl.toString(),
                trend = trendForDelta(sample.deltaMgDlPer5Min),
                delta = sample.deltaMgDlPer5Min,
                timestamp = sample.timestampMs
            )
        )
    }

    private fun generateSamples(): List<SgvSample> {
        val scenario = chooseScenario(lastEmittedSample)
        val startTimestamp = (lastEmittedSample?.timestampMs ?: System.currentTimeMillis()) +
            if (lastEmittedSample == null) 0L else SAMPLE_INTERVAL_MS
        val initialState = lastEmittedSample?.let {
            SgvScenarioFactory.InitialState(
                valueMgDl = it.rawValueMgDl,
                deltaMgDlPer5Min = it.deltaMgDlPer5Min
            )
        }
        val anchors = scenarioFactory.createAnchors(startTimestamp, scenario, random, initialState)

        return generator.generate(
            anchors = anchors,
            config = SgvGenerationConfig(randomSeed = random.nextLong())
        )
    }

    private fun chooseScenario(lastSample: SgvSample?): SgvScenarioFactory.ScenarioType {
        if (configuredScenario != SCENARIO_AUTO) {
            return SgvScenarioFactory.ScenarioType.valueOf(configuredScenario)
        }

        if (lastSample == null) {
            val initialOptions = listOf(
                SgvScenarioFactory.ScenarioType.FASTING_STABLE,
                SgvScenarioFactory.ScenarioType.POST_MEAL_RISE,
                SgvScenarioFactory.ScenarioType.OVERNIGHT_DRIFT
            )
            return initialOptions[random.nextInt(initialOptions.size)]
        }

        return when {
            lastSample.rawValueMgDl < 75.0 -> SgvScenarioFactory.ScenarioType.HYPO_RECOVERY
            lastSample.rawValueMgDl > 165.0 -> SgvScenarioFactory.ScenarioType.OVERNIGHT_DRIFT
            lastSample.deltaMgDlPer5Min > 1.5 && lastSample.rawValueMgDl < 170.0 -> SgvScenarioFactory.ScenarioType.POST_MEAL_RISE
            abs(lastSample.deltaMgDlPer5Min) < 0.75 -> {
                val stableOptions = listOf(
                    SgvScenarioFactory.ScenarioType.FASTING_STABLE,
                    SgvScenarioFactory.ScenarioType.OVERNIGHT_DRIFT
                )
                stableOptions[random.nextInt(stableOptions.size)]
            }
            else -> SgvScenarioFactory.ScenarioType.FASTING_STABLE
        }
    }

    private fun trendForDelta(delta: Double): String {
        return when {
            delta > 3.0 -> "UP2"
            delta > 1.0 -> "UP"
            delta > -1.0 -> "FLAT"
            delta > -3.0 -> "DOWN"
            else -> "DOWN2"
        }
    }

    companion object {
        private const val SAMPLE_INTERVAL_MS = 5 * 60_000L
        private const val KEY_SCENARIO = "scenario"
        private const val KEY_SEED = "seed"
        private const val SCENARIO_AUTO = "AUTO"
    }
}