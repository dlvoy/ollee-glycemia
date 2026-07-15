package pl.cukrzycowy.ollee.glycemia

/**
 * Nightscout fetch state for diagnostics and persistence.
 */

enum class NightscoutFetchStatus {
    NEVER_RUN,
    OK,
    NO_DATA,
    DELAYED_DATA,
    CONNECTION_ERROR,
    AUTH_ERROR,
    INVALID_CONFIG,
    INVALID_RESPONSE,
    SERVER_ERROR
}

data class NightscoutLastFetchState(
    val status: NightscoutFetchStatus,
    val attemptedAtMillis: Long?,
    val completedAtMillis: Long?,
    val readingTimestampMillis: Long?,
    val bg: String?,
    val detail: String?,
    val httpCode: Int? = null,
    val nextFetchAtMillis: Long? = null
)

object NightscoutFetchStateStore {
    private const val PREFS_NAME = "data"
    private const val PREFIX = "nightscout_fetch"

    private fun prefKey(suffix: String): String = "${PREFIX}_$suffix"

    fun read(context: android.content.Context): NightscoutLastFetchState {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

        val statusStr = prefs.getString(prefKey("status"), NightscoutFetchStatus.NEVER_RUN.name)
            ?: NightscoutFetchStatus.NEVER_RUN.name
        val status = try {
            NightscoutFetchStatus.valueOf(statusStr)
        } catch (e: Exception) {
            NightscoutFetchStatus.NEVER_RUN
        }

        val attemptedAtMillis = prefs.getLong(prefKey("attempted_at_ms"), 0L).let { if (it == 0L) null else it }
        val completedAtMillis = prefs.getLong(prefKey("completed_at_ms"), 0L).let { if (it == 0L) null else it }
        val readingTimestampMillis = prefs.getLong(prefKey("reading_timestamp_ms"), 0L).let { if (it == 0L) null else it }
        val bg = prefs.getString(prefKey("bg"), null)
        val detail = prefs.getString(prefKey("detail"), null)
        val httpCode = prefs.getInt(prefKey("http_code"), 0).let { if (it == 0) null else it }
        val nextFetchAtMillis = prefs.getLong(prefKey("next_fetch_at_ms"), 0L).let { if (it == 0L) null else it }

        return NightscoutLastFetchState(
            status = status,
            attemptedAtMillis = attemptedAtMillis,
            completedAtMillis = completedAtMillis,
            readingTimestampMillis = readingTimestampMillis,
            bg = bg,
            detail = detail,
            httpCode = httpCode,
            nextFetchAtMillis = nextFetchAtMillis
        )
    }

    fun write(context: android.content.Context, state: NightscoutLastFetchState) {
        val editor = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()

        editor.putString(prefKey("status"), state.status.name)
        
        if (state.attemptedAtMillis != null) {
            editor.putLong(prefKey("attempted_at_ms"), state.attemptedAtMillis)
        } else {
            editor.remove(prefKey("attempted_at_ms"))
        }

        if (state.completedAtMillis != null) {
            editor.putLong(prefKey("completed_at_ms"), state.completedAtMillis)
        } else {
            editor.remove(prefKey("completed_at_ms"))
        }

        if (state.readingTimestampMillis != null) {
            editor.putLong(prefKey("reading_timestamp_ms"), state.readingTimestampMillis)
        } else {
            editor.remove(prefKey("reading_timestamp_ms"))
        }

        if (state.bg != null) {
            editor.putString(prefKey("bg"), state.bg)
        } else {
            editor.remove(prefKey("bg"))
        }

        if (state.detail != null) {
            editor.putString(prefKey("detail"), state.detail)
        } else {
            editor.remove(prefKey("detail"))
        }

        if (state.httpCode != null) {
            editor.putInt(prefKey("http_code"), state.httpCode)
        } else {
            editor.remove(prefKey("http_code"))
        }

        if (state.nextFetchAtMillis != null) {
            editor.putLong(prefKey("next_fetch_at_ms"), state.nextFetchAtMillis)
        } else {
            editor.remove(prefKey("next_fetch_at_ms"))
        }

        editor.apply()
    }
}
