package pl.cukrzycowy.ollee.glycemia

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Nightscout provider skeleton for Phase 1.
 * TODO: Add HTTP client wrapper, adaptive scheduling, and fetch loop in later phases.
 */
class NightscoutProvider : ConfigurableGlycemiaProvider {

    override val id: String = ID
    override val displayName: String = "Nightscout"

    private var callback: ((GlycemiaReading) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    override fun start(context: Context, onReading: (GlycemiaReading) -> Unit) {
        Log.d(TAG, "Starting Nightscout provider")
        callback = onReading
        isRunning = true

        // Phase 2+: Read config, validate, schedule fetch
        // For now: save INVALID_CONFIG state since provider is not fully initialized
        val invalidState = NightscoutLastFetchState(
            status = NightscoutFetchStatus.INVALID_CONFIG,
            attemptedAtMillis = null,
            completedAtMillis = null,
            readingTimestampMillis = null,
            bg = null,
            detail = "Provider not fully initialized in Phase 1"
        )
        NightscoutFetchStateStore.write(context, invalidState)
    }

    override fun stop(context: Context) {
        Log.d(TAG, "Stopping Nightscout provider")
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        callback = null
    }

    override fun getConfigSpec(context: Context): ProviderConfigSpec {
        return ProviderConfigSpec(
            title = context.getString(R.string.provider_nightscout_config_title),
            fields = listOf(
                ProviderConfigField(
                    key = KEY_BASE_URL,
                    label = context.getString(R.string.provider_nightscout_url_label),
                    type = ProviderConfigField.FieldType.TEXT,
                    defaultValue = "",
                    helperText = context.getString(R.string.provider_nightscout_url_helper)
                ),
                ProviderConfigField(
                    key = KEY_TOKEN,
                    label = context.getString(R.string.provider_nightscout_token_label),
                    type = ProviderConfigField.FieldType.TEXT,
                    defaultValue = "",
                    helperText = context.getString(R.string.provider_nightscout_token_helper)
                )
            )
        )
    }

    override fun getSavedConfig(context: Context): Map<String, String> {
        return ProviderConfigStore.read(context, id, getConfigSpec(context))
    }

    override fun saveConfig(context: Context, values: Map<String, String>) {
        // Phase 2: Normalize URL before saving
        val normalizedValues = values.toMutableMap()
        ProviderConfigStore.write(context, id, normalizedValues)
    }

    override fun getConfigSummary(context: Context): String {
        val config = getSavedConfig(context)
        val baseUrl = config[KEY_BASE_URL]

        return if (baseUrl.isNullOrEmpty()) {
            context.getString(R.string.provider_nightscout_summary_not_configured)
        } else {
            context.getString(R.string.provider_nightscout_summary_configured, baseUrl)
        }
    }

    companion object {
        const val ID = "nightscout"
        private const val TAG = "NightscoutProvider"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_TOKEN = "token"
    }
}
