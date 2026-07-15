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

        // Read config and validate
        val config = getSavedConfig(context)
        val baseUrl = config[KEY_BASE_URL]
        val token = config[KEY_TOKEN]

        if (baseUrl.isNullOrEmpty()) {
            val invalidState = NightscoutLastFetchState(
                status = NightscoutFetchStatus.INVALID_CONFIG,
                attemptedAtMillis = System.currentTimeMillis(),
                completedAtMillis = System.currentTimeMillis(),
                readingTimestampMillis = null,
                bg = null,
                detail = "Base URL not configured"
            )
            NightscoutFetchStateStore.write(context, invalidState)
            return
        }

        // Validate and normalize URL
        val normalizationResult = NightscoutUrlNormalizer.normalize(baseUrl)
        if (normalizationResult is NightscoutConfigValidationResult.Invalid) {
            val invalidState = NightscoutLastFetchState(
                status = NightscoutFetchStatus.INVALID_CONFIG,
                attemptedAtMillis = System.currentTimeMillis(),
                completedAtMillis = System.currentTimeMillis(),
                readingTimestampMillis = null,
                bg = null,
                detail = normalizationResult.reason
            )
            NightscoutFetchStateStore.write(context, invalidState)
            return
        }

        val normalizedConfig = (normalizationResult as NightscoutConfigValidationResult.Valid).normalized
        val normalizedUrl = normalizedConfig.baseUrl
        val effectiveToken = (token ?: "").ifEmpty { normalizedConfig.extractedToken }

        // Test connection with status() call
        val httpClient = NightscoutHttpClient(normalizedUrl, effectiveToken)
        val statusResult = httpClient.status()

        when (statusResult) {
            is NightscoutParseResult.Success -> {
                val status = statusResult.value
                Log.d(TAG, "Connected to Nightscout v${status.version}: ${status.title ?: "unnamed"}")
                
                val okState = NightscoutLastFetchState(
                    status = NightscoutFetchStatus.OK,
                    attemptedAtMillis = System.currentTimeMillis(),
                    completedAtMillis = System.currentTimeMillis(),
                    readingTimestampMillis = null,
                    bg = null,
                    detail = "Connected to ${status.version}"
                )
                NightscoutFetchStateStore.write(context, okState)

                // Phase 3+: Schedule regular fetch here
                // For now, just mark as ready
            }
            is NightscoutParseResult.Failure -> {
                Log.e(TAG, "Connection test failed: ${statusResult.reason}")
                
                val failureStatus = when {
                    statusResult.reason.contains("401") || statusResult.reason.contains("Authentication") -> 
                        NightscoutFetchStatus.AUTH_ERROR
                    statusResult.reason.contains("timeout", ignoreCase = true) -> 
                        NightscoutFetchStatus.CONNECTION_ERROR
                    statusResult.reason.contains("unsupported", ignoreCase = true) -> 
                        NightscoutFetchStatus.INVALID_RESPONSE
                    else -> NightscoutFetchStatus.CONNECTION_ERROR
                }

                val failState = NightscoutLastFetchState(
                    status = failureStatus,
                    attemptedAtMillis = System.currentTimeMillis(),
                    completedAtMillis = System.currentTimeMillis(),
                    readingTimestampMillis = null,
                    bg = null,
                    detail = statusResult.reason
                )
                NightscoutFetchStateStore.write(context, failState)
            }
        }
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
