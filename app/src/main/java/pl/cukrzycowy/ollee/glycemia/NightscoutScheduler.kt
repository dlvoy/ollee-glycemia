package pl.cukrzycowy.ollee.glycemia

import kotlin.math.min

/**
 * Adaptive scheduling for Nightscout readings.
 * 
 * Strategies:
 * - Regular mode: fetch at reading_timestamp + 5min + sliding window delay
 * - Old data retry: exponential backoff (5s, 15s, 30s) when reading is stale
 * - Delayed mode: after 60s without new reading, increase frequency
 * - Sliding window: delay increases as reading gets older (to avoid thundering herd)
 */
object NightscoutScheduler {

    // Timing constants (milliseconds)
    const val REGULAR_INTERVAL = 5 * 60 * 1000L      // 5 minutes in regular mode
    const val MAX_READING_AGE = 10 * 60 * 1000L      // Consider >10min as stale
    const val DELAYED_MODE_THRESHOLD = 60 * 1000L    // Switch to delayed after 60s without new reading
    const val DELAYED_MODE_INTERVAL = 15 * 1000L     // 15s retry in delayed mode
    const val MAX_DELAY_WINDOW = 2 * 60 * 1000L      // Max sliding window delay (2 minutes)

    // Retry backoff sequence for stale data
    private val RETRY_DELAYS = listOf(5 * 1000L, 15 * 1000L, 30 * 1000L)

    /**
     * Calculate next fetch time based on current reading state.
     *
     * @param lastReadingTimestampMs - timestamp of last reading from Nightscout
     * @param lastReadingReceivedAt - when we received the last reading (system time)
     * @param timeNow - current system time
     * @param failureCount - consecutive failures (used for backoff)
     * @return milliseconds until next fetch attempt
     */
    fun calculateNextFetchDelay(
        lastReadingTimestampMs: Long,
        lastReadingReceivedAt: Long,
        timeNow: Long,
        failureCount: Int = 0
    ): Long {
        val timeSinceLastReading = timeNow - lastReadingReceivedAt
        val readingAge = timeNow - lastReadingTimestampMs

        // If reading is very stale (>10min), use exponential backoff
        if (readingAge > MAX_READING_AGE) {
            val backoffDelay = getBackoffDelay(failureCount)
            return backoffDelay
        }

        // If we haven't received a new reading in >60s, switch to delayed mode
        if (timeSinceLastReading > DELAYED_MODE_THRESHOLD) {
            return DELAYED_MODE_INTERVAL
        }

        // Regular mode: fetch at reading_timestamp + 5min + sliding window delay
        val nextExpectedReadingTime = lastReadingTimestampMs + REGULAR_INTERVAL
        val slidingWindowDelay = calculateSlidingWindowDelay(readingAge)
        val nextFetchTime = nextExpectedReadingTime + slidingWindowDelay

        val delayFromNow = maxOf(0L, nextFetchTime - timeNow)

        return delayFromNow
    }

    /**
     * Exponential backoff for retry attempts.
     * Sequence: 5s, 15s, 30s, then stick at 30s
     */
    private fun getBackoffDelay(failureCount: Int): Long {
        val index = min(failureCount, RETRY_DELAYS.size - 1)
        return RETRY_DELAYS[index]
    }

    /**
     * Sliding window delay to spread out requests and reduce thundering herd.
     * As reading gets older (up to 5min), delay increases from 0 to MAX_DELAY_WINDOW.
     */
    private fun calculateSlidingWindowDelay(readingAgeMs: Long): Long {
        val maxAge = 5 * 60 * 1000L  // Ramp delay over 5 minutes
        if (readingAgeMs >= maxAge) {
            return MAX_DELAY_WINDOW
        }

        // Linear ramp: delay = (readingAge / maxAge) * MAX_DELAY_WINDOW
        return (readingAgeMs.toDouble() / maxAge * MAX_DELAY_WINDOW).toLong()
    }

    /**
     * Determine if a reading is considered "new" enough.
     * Older than 10 minutes = stale and needs retry.
     */
    fun isReadingFresh(readingTimestampMs: Long, timeNow: Long): Boolean {
        val age = timeNow - readingTimestampMs
        return age <= MAX_READING_AGE
    }

    /**
     * Convert NightscoutEntry to GlycemiaReading for provider callback.
     */
    fun entryToReading(entry: NightscoutEntry): GlycemiaReading {
        return GlycemiaReading(
            bg = entry.sgv.toString(),
            trend = entry.direction ?: "FLAT",
            delta = entry.delta,
            timestamp = entry.timestampMillis
        )
    }
}
