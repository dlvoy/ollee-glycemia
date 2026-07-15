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

    // State for scheduler
    private var httpClient: NightscoutHttpClient? = null
    private var lastReadingTimestamp: Long = 0L
    private var lastReadingReceivedAt: Long = 0L
    private var failureCount: Int = 0
    private var context: Context? = null

    override fun start(context: Context, onReading: (GlycemiaReading) -> Unit) {
        Log.d(TAG, "Starting Nightscout provider")
        callback = onReading
        this.context = context
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

        // Create HTTP client
        httpClient = NightscoutHttpClient(normalizedUrl, effectiveToken)

        // Initialize state and start fetching
        lastReadingReceivedAt = 0L
        lastReadingTimestamp = 0L
        failureCount = 0
        
        val initialState = NightscoutLastFetchState(
            status = NightscoutFetchStatus.NEVER_RUN,
            attemptedAtMillis = System.currentTimeMillis(),
            completedAtMillis = System.currentTimeMillis(),
            readingTimestampMillis = null,
            bg = null,
            detail = "Provider started, waiting for first fetch"
        )
        NightscoutFetchStateStore.write(context, initialState)
        
        // Fetch immediately, then schedule adaptive fetches
        fetchReadings()
    }

    override fun stop(context: Context) {
        Log.d(TAG, "Stopping Nightscout provider")
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        callback = null
        httpClient = null
        this.context = null
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

    /**
     * Schedule the next fetch based on adaptive scheduling logic.
     */
    private fun scheduleFetch() {
        if (!isRunning || httpClient == null || callback == null || context == null) {
            return
        }

        val delayMs = NightscoutScheduler.calculateNextFetchDelay(
            lastReadingTimestampMs = lastReadingTimestamp,
            lastReadingReceivedAt = lastReadingReceivedAt,
            timeNow = System.currentTimeMillis(),
            failureCount = failureCount
        )

        Log.d(TAG, "Scheduling next fetch in ${delayMs}ms")
        handler.postDelayed({ Thread { fetchReadings() }.start() }, delayMs)
    }

    /**
     * Fetch latest reading from Nightscout and schedule next fetch.
     */
    private fun fetchReadings() {
        if (!isRunning || httpClient == null || callback == null || context == null) {
            return
        }

        val attemptTime = System.currentTimeMillis()
        val entriesResult = httpClient!!.entries(count = 1)

        when (entriesResult) {
            is NightscoutParseResult.Success -> {
                val entry = entriesResult.value
                val reading = NightscoutScheduler.entryToReading(entry)

                // Update state
                lastReadingTimestamp = entry.timestampMillis
                lastReadingReceivedAt = System.currentTimeMillis()
                failureCount = 0

                // Call provider callback
                callback!!(reading)

                // Store state
                val successState = NightscoutLastFetchState(
                    status = NightscoutFetchStatus.OK,
                    attemptedAtMillis = attemptTime,
                    completedAtMillis = System.currentTimeMillis(),
                    readingTimestampMillis = entry.timestampMillis,
                    bg = entry.sgv.toString(),
                    detail = null,
                    httpCode = 200
                )
                NightscoutFetchStateStore.write(context!!, successState)

                Log.d(TAG, "Fetched reading: ${entry.sgv} (age=${System.currentTimeMillis() - entry.timestampMillis}ms)")

                // Schedule next fetch
                scheduleFetch()
            }
            is NightscoutParseResult.Failure -> {
                failureCount++
                Log.w(TAG, "Fetch failed (attempt $failureCount): ${entriesResult.reason}")

                // Classify error and store state
                val errorStatus = when {
                    entriesResult.reason.contains("401") || entriesResult.reason.contains("Authentication") ->
                        NightscoutFetchStatus.AUTH_ERROR
                    entriesResult.reason.contains("timeout", ignoreCase = true) ->
                        NightscoutFetchStatus.CONNECTION_ERROR
                    entriesResult.reason.contains("Empty response") ->
                        NightscoutFetchStatus.NO_DATA
                    entriesResult.reason.contains("invalid", ignoreCase = true) ->
                        NightscoutFetchStatus.INVALID_RESPONSE
                    else -> NightscoutFetchStatus.CONNECTION_ERROR
                }

                val failState = NightscoutLastFetchState(
                    status = errorStatus,
                    attemptedAtMillis = attemptTime,
                    completedAtMillis = System.currentTimeMillis(),
                    readingTimestampMillis = lastReadingTimestamp.takeIf { it > 0 },
                    bg = null,
                    detail = entriesResult.reason,
                    nextFetchAtMillis = System.currentTimeMillis() + 
                        NightscoutScheduler.calculateNextFetchDelay(
                            lastReadingTimestampMs = lastReadingTimestamp,
                            lastReadingReceivedAt = lastReadingReceivedAt,
                            timeNow = System.currentTimeMillis(),
                            failureCount = failureCount
                        )
                )
                NightscoutFetchStateStore.write(context!!, failState)

                // Schedule retry with backoff
                scheduleFetch()
            }
        }
    }

    companion object {
        const val ID = "nightscout"
        private const val TAG = "NightscoutProvider"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_TOKEN = "token"
    }
}
