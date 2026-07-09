package com.arthur.bgollee

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProviderConfigurationTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun constantProvider_saveConfig_normalizesAndReadsValues() {
        val provider = ConstantProvider()

        provider.saveConfig(
            context,
            mapOf(
                "value_mgdl" to "999",
                "interval_mode" to "random_5_20_minutes"
            )
        )

        val saved = provider.getSavedConfig(context)

        assertEquals("400", saved["value_mgdl"])
        assertEquals("random_5_20_minutes", saved["interval_mode"])
        assertEquals(
            context.getString(R.string.provider_constant_summary_format, 400, context.getString(R.string.provider_interval_random_5_20)),
            provider.getConfigSummary(context)
        )
    }

    @Test
    fun constantProvider_invalidIntervalFallsBackToDefault() {
        val provider = ConstantProvider()

        provider.saveConfig(
            context,
            mapOf(
                "value_mgdl" to "100",
                "interval_mode" to "bad_value"
            )
        )

        val saved = provider.getSavedConfig(context)

        assertEquals("1_minute", saved["interval_mode"])
    }

    @Test
    fun virtualHumanProvider_saveConfig_persistsScenarioAndSeed() {
        val provider = VirtualHumanProvider()

        provider.saveConfig(
            context,
            mapOf(
                "scenario" to "POST_MEAL_RISE",
                "seed" to "12345"
            )
        )

        val saved = provider.getSavedConfig(context)

        assertEquals("POST_MEAL_RISE", saved["scenario"])
        assertEquals("12345", saved["seed"])
        assertEquals(
            context.getString(
                R.string.provider_virtual_human_summary_format,
                context.getString(R.string.provider_virtual_human_scenario_post_meal),
                "12345"
            ),
            provider.getConfigSummary(context)
        )
    }

    @Test
    fun virtualHumanProvider_invalidScenarioFallsBackToAuto() {
        val provider = VirtualHumanProvider()

        provider.saveConfig(
            context,
            mapOf(
                "scenario" to "NOT_REAL",
                "seed" to ""
            )
        )

        val saved = provider.getSavedConfig(context)

        assertEquals("AUTO", saved["scenario"])
        assertEquals("", saved["seed"])
        assertEquals(
            context.getString(
                R.string.provider_virtual_human_summary_format,
                context.getString(R.string.provider_virtual_human_scenario_auto),
                context.getString(R.string.provider_virtual_human_seed_random)
            ),
            provider.getConfigSummary(context)
        )
    }
}