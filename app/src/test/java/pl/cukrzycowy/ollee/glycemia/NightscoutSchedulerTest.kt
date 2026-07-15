package pl.cukrzycowy.ollee.glycemia

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for NightscoutScheduler: fetch timing, backoff, sliding window logic.
 */
class NightscoutSchedulerTest {

    @Test
    fun regularMode_freshReading_calculatesCorrectNextFetch() {
        val now = System.currentTimeMillis()
        val readingTimeMs = now - 100L    // Just received
        val receivedAtMs = readingTimeMs  // Just received

        val delay = NightscoutScheduler.calculateNextFetchDelay(
            lastReadingTimestampMs = readingTimeMs,
            lastReadingReceivedAt = receivedAtMs,
            timeNow = now,
            failureCount = 0
        )

        // Should fetch at readingTime + 5min + minimal sliding window delay
        val expectedMinDelay = (5 * 60 * 1000L) - 100L  // ~5min
        assertTrue("Delay should be around 5 minutes", delay in (expectedMinDelay - 1000L)..(expectedMinDelay + 1000L))
    }

    @Test
    fun staleData_olderThan10min_usesExponentialBackoff() {
        val now = System.currentTimeMillis()
        val readingTimeMs = now - (11 * 60 * 1000L)  // 11 minutes old = stale
        val receivedAtMs = readingTimeMs

        val delay = NightscoutScheduler.calculateNextFetchDelay(
            lastReadingTimestampMs = readingTimeMs,
            lastReadingReceivedAt = receivedAtMs,
            timeNow = now,
            failureCount = 0
        )

        // First retry: 5 seconds
        assertEquals(5 * 1000L, delay)
    }

    @Test
    fun staleData_secondFailure_backoffIncreases() {
        // Use future timestamps to avoid edge cases
        val now = System.currentTimeMillis()
        val readingTimeMs = now - (11 * 60 * 1000L)  // 11 minutes old = stale
        val receivedAtMs = readingTimeMs

        val delay = NightscoutScheduler.calculateNextFetchDelay(
            lastReadingTimestampMs = readingTimeMs,
            lastReadingReceivedAt = receivedAtMs,
            timeNow = now,
            failureCount = 1
        )

        // Second retry: 15 seconds
        assertEquals("Second backoff should be 15s", 15 * 1000L, delay)
    }

    @Test
    fun staleData_thirdFailure_maxBackoff() {
        val now = System.currentTimeMillis()
        val readingTimeMs = now - (11 * 60 * 1000L)  // 11 minutes old = stale
        val receivedAtMs = readingTimeMs

        val delay = NightscoutScheduler.calculateNextFetchDelay(
            lastReadingTimestampMs = readingTimeMs,
            lastReadingReceivedAt = receivedAtMs,
            timeNow = now,
            failureCount = 2
        )

        // Third+ retry: 30 seconds
        assertEquals("Third+ backoff should be 30s", 30 * 1000L, delay)
    }

    @Test
    fun delayedMode_noNewReadingFor60s_switchesToFastRetry() {
        val now = System.currentTimeMillis()
        val readingTimeMs = now - (5 * 60 * 1000L)  // 5 minutes old (fresh, not stale)
        val receivedAtMs = now - (61 * 1000L)       // Received 61 seconds ago

        val delay = NightscoutScheduler.calculateNextFetchDelay(
            lastReadingTimestampMs = readingTimeMs,
            lastReadingReceivedAt = receivedAtMs,
            timeNow = now,
            failureCount = 0
        )

        // Switch to delayed mode: 15 second interval
        assertEquals("Delayed mode should use 15s interval", 15 * 1000L, delay)
    }

    @Test
    fun isReadingFresh_withinTenMinutes_returnTrue() {
        val now = System.currentTimeMillis()
        val readingTimeMs = now - (9 * 60 * 1000L)  // 9 minutes old

        assertTrue(NightscoutScheduler.isReadingFresh(readingTimeMs, now))
    }

    @Test
    fun isReadingFresh_olderThanTenMinutes_returnFalse() {
        val now = System.currentTimeMillis()
        val readingTimeMs = now - (11 * 60 * 1000L)  // 11 minutes old

        assertFalse(NightscoutScheduler.isReadingFresh(readingTimeMs, now))
    }

    @Test
    fun entryToReading_convertsCorrectly() {
        val entry = NightscoutEntry(
            sgv = 143,
            delta = -0.999,
            direction = "Flat",
            timestampMillis = 1684132303223L
        )

        val reading = NightscoutScheduler.entryToReading(entry)

        assertEquals("143", reading.bg)
        assertEquals("Flat", reading.trend)
        assertEquals(-0.999, reading.delta ?: 0.0, 0.001)
        assertEquals(1684132303223L, reading.timestamp)
    }

    @Test
    fun entryToReading_nullDirection_usesFlat() {
        val entry = NightscoutEntry(
            sgv = 150,
            delta = 2.5,
            direction = null,
            timestampMillis = 1684132303223L
        )

        val reading = NightscoutScheduler.entryToReading(entry)

        assertEquals("FLAT", reading.trend)
    }

    @Test
    fun slidingWindow_newReading_minimalDelay() {
        val now = System.currentTimeMillis()
        val readingTimeMs = now - 1000L   // 1 second old
        val receivedAtMs = now - 100L     // Received 100ms ago

        val delay = NightscoutScheduler.calculateNextFetchDelay(
            lastReadingTimestampMs = readingTimeMs,
            lastReadingReceivedAt = receivedAtMs,
            timeNow = now,
            failureCount = 0
        )

        // Should be close to 5 minutes - minimal sliding window
        assertTrue("Delay should be around 5min", delay > 298000L && delay < 302000L)
    }

    @Test
    fun slidingWindow_olderReading_increasedDelay() {
        val now = System.currentTimeMillis()
        val readingTimeMs = now - (3 * 60 * 1000L)  // Reading timestamp is 3 minutes old
        val receivedAtMs = now - 500L               // But we just received it 500ms ago

        val delay = NightscoutScheduler.calculateNextFetchDelay(
            lastReadingTimestampMs = readingTimeMs,
            lastReadingReceivedAt = receivedAtMs,
            timeNow = now,
            failureCount = 0
        )

        // Regular mode: fetch at readingTime + 5min + sliding window delay
        // nextFetch = (now - 3min) + 5min + ~1min = now + 2min + 1min = now + 3min
        assertTrue("Delay should be between 2-4 minutes", delay in 120000L..240000L)
    }

    @Test
    fun entryToReading_nullDelta_acceptsIt() {
        val entry = NightscoutEntry(
            sgv = 120,
            delta = null,
            direction = "UP",
            timestampMillis = 1684132303223L
        )

        val reading = NightscoutScheduler.entryToReading(entry)

        assertNull(reading.delta)
        assertEquals("UP", reading.trend)
    }
}
