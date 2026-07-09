package com.arthur.bgollee

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.random.Random

class ConstantProvider : ConfigurableGlycemiaProvider {

    override val id: String = "constant"
    override val displayName: String = "Constant (test, 100 mg/dL)"

    private var callback: ((GlycemiaReading) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private val random = Random(System.currentTimeMillis())

    private var configuredValueMgDl: Int = DEFAULT_VALUE_MGDL
    private var configuredIntervalMode: String = IntervalMode.ONE_MINUTE.value

    private val runnable = object : Runnable {
        override fun run() {
            callback?.invoke(
                GlycemiaReading(
                    bg = configuredValueMgDl.toString(),
                    trend = "FLAT",
                    delta = 0.0,
                    timestamp = System.currentTimeMillis()
                )
            )
            handler.postDelayed(this, nextDelayMs())
        }
    }

    override fun start(context: Context, onReading: (GlycemiaReading) -> Unit) {
        Log.d("ConstantProvider", "Starting constant provider")
        val config = getSavedConfig(context)
        configuredValueMgDl = (config[KEY_VALUE_MGDL]?.toIntOrNull() ?: DEFAULT_VALUE_MGDL)
            .coerceIn(40, 400)
        configuredIntervalMode = config[KEY_INTERVAL_MODE] ?: IntervalMode.ONE_MINUTE.value
        callback = onReading
        handler.removeCallbacksAndMessages(null)
        handler.post(runnable)
    }

    override fun stop(context: Context) {
        Log.d("ConstantProvider", "Stopping constant provider")
        handler.removeCallbacksAndMessages(null)
        callback = null
    }

    override fun getConfigSpec(context: Context): ProviderConfigSpec {
        return ProviderConfigSpec(
            title = context.getString(R.string.provider_constant_config_title),
            fields = listOf(
                ProviderConfigField(
                    key = KEY_VALUE_MGDL,
                    label = context.getString(R.string.provider_constant_value_label),
                    type = ProviderConfigField.FieldType.INTEGER,
                    defaultValue = DEFAULT_VALUE_MGDL.toString(),
                    helperText = context.getString(R.string.provider_constant_value_helper)
                ),
                ProviderConfigField(
                    key = KEY_INTERVAL_MODE,
                    label = context.getString(R.string.provider_constant_interval_label),
                    type = ProviderConfigField.FieldType.CHOICE,
                    defaultValue = IntervalMode.ONE_MINUTE.value,
                    options = listOf(
                        ProviderConfigOption(IntervalMode.THIRTY_SECONDS.value, context.getString(R.string.provider_interval_30s)),
                        ProviderConfigOption(IntervalMode.ONE_MINUTE.value, context.getString(R.string.provider_interval_1m)),
                        ProviderConfigOption(IntervalMode.FIVE_MINUTES.value, context.getString(R.string.provider_interval_5m)),
                        ProviderConfigOption(IntervalMode.RANDOM_5_TO_20_MINUTES.value, context.getString(R.string.provider_interval_random_5_20))
                    )
                )
            )
        )
    }

    override fun getSavedConfig(context: Context): Map<String, String> {
        return ProviderConfigStore.read(context, id, getConfigSpec(context))
    }

    override fun saveConfig(context: Context, values: Map<String, String>) {
        val normalizedValues = values.toMutableMap()
        val bgValue = values[KEY_VALUE_MGDL]?.toIntOrNull()?.coerceIn(40, 400) ?: DEFAULT_VALUE_MGDL
        normalizedValues[KEY_VALUE_MGDL] = bgValue.toString()

        val intervalMode = values[KEY_INTERVAL_MODE]
            ?.takeIf { candidate -> IntervalMode.entries.any { it.value == candidate } }
            ?: IntervalMode.ONE_MINUTE.value
        normalizedValues[KEY_INTERVAL_MODE] = intervalMode

        ProviderConfigStore.write(context, id, normalizedValues)
    }

    override fun getConfigSummary(context: Context): String {
        val config = getSavedConfig(context)
        val value = (config[KEY_VALUE_MGDL]?.toIntOrNull() ?: DEFAULT_VALUE_MGDL).coerceIn(40, 400)
        val intervalLabel = when (config[KEY_INTERVAL_MODE] ?: IntervalMode.ONE_MINUTE.value) {
            IntervalMode.THIRTY_SECONDS.value -> context.getString(R.string.provider_interval_30s)
            IntervalMode.FIVE_MINUTES.value -> context.getString(R.string.provider_interval_5m)
            IntervalMode.RANDOM_5_TO_20_MINUTES.value -> context.getString(R.string.provider_interval_random_5_20)
            else -> context.getString(R.string.provider_interval_1m)
        }
        return context.getString(R.string.provider_constant_summary_format, value, intervalLabel)
    }

    private fun nextDelayMs(): Long {
        return when (configuredIntervalMode) {
            IntervalMode.THIRTY_SECONDS.value -> 30_000L
            IntervalMode.FIVE_MINUTES.value -> 300_000L
            IntervalMode.RANDOM_5_TO_20_MINUTES.value -> random.nextLong(300_000L, 1_200_001L)
            else -> 60_000L
        }
    }

    private enum class IntervalMode(val value: String) {
        THIRTY_SECONDS("30_seconds"),
        ONE_MINUTE("1_minute"),
        FIVE_MINUTES("5_minutes"),
        RANDOM_5_TO_20_MINUTES("random_5_20_minutes")
    }

    companion object {
        private const val KEY_VALUE_MGDL = "value_mgdl"
        private const val KEY_INTERVAL_MODE = "interval_mode"
        private const val DEFAULT_VALUE_MGDL = 100
    }
}
