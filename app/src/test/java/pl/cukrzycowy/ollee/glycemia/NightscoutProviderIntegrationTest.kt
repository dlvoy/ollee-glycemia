package pl.cukrzycowy.ollee.glycemia

import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests for Nightscout provider components: pure logic testing.
 * 
 * Tests cover:
 * - Configuration parsing and validation
 * - URL normalization and token extraction
 * - Scheduling logic with state transitions
 * - Entry parsing and reading conversion
 * 
 * Note: Full provider lifecycle tests require Robolectric/Android context.
 * These tests validate the core logic components independently.
 */
class NightscoutProviderIntegrationTest {

    @Test
    fun urlNormalization_shouldHandleMultipleFormats() {
        // Test cases: various URL formats that should all normalize correctly
        val testCases = listOf(
            "example.org" to "https://example.org",
            "http://example.org" to "http://example.org",
            "https://example.org/" to "https://example.org",
            "https://example.org/api/v1" to "https://example.org",
            "https://example.org?token=sync-1234567890abcdef" to "https://example.org"
        )

        testCases.forEach { (input, expectedBase) ->
            // Act
            val result = NightscoutUrlNormalizer.normalize(input)

            // Assert
            assertTrue("Normalization should succeed for: $input", 
                result is NightscoutConfigValidationResult.Valid)
            result.let { validResult ->
                if (validResult is NightscoutConfigValidationResult.Valid) {
                    assertEquals("URL should normalize to: $expectedBase", expectedBase, validResult.normalized.baseUrl)
                }
            }
        }
    }

    @Test
    fun tokenExtraction_shouldExtractFromQueryParam() {
        // Arrange
        val urlWithToken = "https://example.org?token=sync-1234567890abcdef"

        // Act
        val result = NightscoutUrlNormalizer.normalize(urlWithToken)

        // Assert
        assertTrue("Should extract token successfully", 
            result is NightscoutConfigValidationResult.Valid)
        result.let { validResult ->
            if (validResult is NightscoutConfigValidationResult.Valid) {
                assertEquals("Token should be extracted", "sync-1234567890abcdef", validResult.normalized.extractedToken)
            }
        }
    }

    @Test
    fun urlValidation_shouldRejectEmptyUrl() {
        // Act
        val result = NightscoutUrlNormalizer.normalize("")

        // Assert
        assertTrue("Should reject empty URL", result is NightscoutConfigValidationResult.Invalid)
    }

    @Test
    fun urlValidation_shouldRejectInvalidScheme() {
        // Act
        val result = NightscoutUrlNormalizer.normalize("ftp://example.org")

        // Assert
        assertTrue("Should reject non-http scheme", result is NightscoutConfigValidationResult.Invalid)
    }

    @Test
    fun tokenFormat_shouldValidateCorrectFormat() {
        // Arrange
        val validToken = "sync-1234567890abcdef"

        // Act
        val warning = NightscoutUrlNormalizer.validateTokenFormat(validToken)

        // Assert
        assertNull("Valid token should have no warning", warning)
    }

    @Test
    fun tokenFormat_shouldRejectInvalidFormat() {
        // Arrange
        val invalidToken = "invalid-token"

        // Act
        val warning = NightscoutUrlNormalizer.validateTokenFormat(invalidToken)

        // Assert
        assertNotNull("Invalid token should have warning", warning)
    }

    @Test
    fun tokenFormat_shouldRejectEmptyToken() {
        // Arrange
        val emptyToken = ""

        // Act
        val warning = NightscoutUrlNormalizer.validateTokenFormat(emptyToken)

        // Assert
        assertNotNull("Empty token should have warning", warning)
    }

    @Test
    fun schedulingLogic_shouldCalculateCorrectDelay_freshReading() {
        // Arrange
        val now = System.currentTimeMillis()
        val readingTimeMs = now - 100L    // Just received
        val receivedAtMs = readingTimeMs  // Just received

        // Act
        val delay = NightscoutScheduler.calculateNextFetchDelay(
            lastReadingTimestampMs = readingTimeMs,
            lastReadingReceivedAt = receivedAtMs,
            timeNow = now,
            failureCount = 0
        )

        // Assert - should be around 5 minutes
        val expectedMinDelay = (5 * 60 * 1000L) - 100L
        assertTrue("Delay should be around 5 minutes", 
            delay in (expectedMinDelay - 1000L)..(expectedMinDelay + 1000L))
    }

    @Test
    fun schedulingLogic_shouldUseBackoffForStaleData() {
        // Arrange
        val now = System.currentTimeMillis()
        val readingTimeMs = now - (11 * 60 * 1000L)  // 11 minutes old = stale
        val receivedAtMs = readingTimeMs

        // Act
        val delay = NightscoutScheduler.calculateNextFetchDelay(
            lastReadingTimestampMs = readingTimeMs,
            lastReadingReceivedAt = receivedAtMs,
            timeNow = now,
            failureCount = 0
        )

        // Assert - first retry: 5 seconds
        assertEquals("Should use exponential backoff", 5 * 1000L, delay)
    }

    @Test
    fun schedulingLogic_shouldIncreaseBackoffOnRetry() {
        // Arrange
        val now = System.currentTimeMillis()
        val readingTimeMs = now - (11 * 60 * 1000L)
        val receivedAtMs = readingTimeMs

        // Act
        val delay = NightscoutScheduler.calculateNextFetchDelay(
            lastReadingTimestampMs = readingTimeMs,
            lastReadingReceivedAt = receivedAtMs,
            timeNow = now,
            failureCount = 1  // Second retry
        )

        // Assert - second retry: 15 seconds
        assertEquals("Should increase backoff on retry", 15 * 1000L, delay)
    }

    @Test
    fun readingFreshness_shouldDetectOldReadings() {
        // Arrange
        val now = System.currentTimeMillis()
        val readingTimeMs = now - (11 * 60 * 1000L)  // 11 minutes old

        // Act
        val isFresh = NightscoutScheduler.isReadingFresh(readingTimeMs, now)

        // Assert
        assertFalse("Reading older than 10min should be stale", isFresh)
    }

    @Test
    fun readingFreshness_shouldAcceptRecentReadings() {
        // Arrange
        val now = System.currentTimeMillis()
        val readingTimeMs = now - (5 * 60 * 1000L)  // 5 minutes old

        // Act
        val isFresh = NightscoutScheduler.isReadingFresh(readingTimeMs, now)

        // Assert
        assertTrue("Reading within 10min should be fresh", isFresh)
    }

    @Test
    fun entryToReading_shouldConvertCorrectly() {
        // Arrange
        val entry = NightscoutEntry(
            sgv = 143,
            delta = -0.999,
            direction = "Flat",
            timestampMillis = 1684132303223L
        )

        // Act
        val reading = NightscoutScheduler.entryToReading(entry)

        // Assert
        assertEquals("BG should be converted to string", "143", reading.bg)
        assertEquals("Trend should match direction", "Flat", reading.trend)
        assertEquals("Delta should be preserved", -0.999, reading.delta ?: 0.0, 0.001)
        assertEquals("Timestamp should match", 1684132303223L, reading.timestamp)
    }

    @Test
    fun entryToReading_shouldHandleNullTrend() {
        // Arrange
        val entry = NightscoutEntry(
            sgv = 150,
            delta = 2.5,
            direction = null,
            timestampMillis = 1684132303223L
        )

        // Act
        val reading = NightscoutScheduler.entryToReading(entry)

        // Assert
        assertEquals("Should default to FLAT when direction is null", "FLAT", reading.trend)
    }

    @Test
    fun entryToReading_shouldPreserveNullDelta() {
        // Arrange
        val entry = NightscoutEntry(
            sgv = 120,
            delta = null,
            direction = "UP",
            timestampMillis = 1684132303223L
        )

        // Act
        val reading = NightscoutScheduler.entryToReading(entry)

        // Assert
        assertNull("Delta should remain null", reading.delta)
        assertEquals("Trend should be preserved", "UP", reading.trend)
    }

    @Test
    fun statusParsing_shouldValidateVersion() {
        // Arrange - Nightscout response JSON with old version
        val oldVersionJson = """{"status":"ok","version":"14.0.0"}"""

        // Act
        val result = NightscoutStatusParser.parse(oldVersionJson)

        // Assert
        assertTrue("Should reject version < 15", result is NightscoutParseResult.Failure)
    }

    @Test
    fun statusParsing_shouldAcceptNewVersion() {
        // Arrange
        val validVersionJson = """{"status":"ok","version":"15.0.0"}"""

        // Act
        val result = NightscoutStatusParser.parse(validVersionJson)

        // Assert
        assertTrue("Should accept version >= 15", result is NightscoutParseResult.Success)
    }

    @Test
    fun entryParsing_shouldHandleMultipleDateFormats() {
        // Arrange - entry with timestamp priority: date > mills
        val entryWithDate = """[{"sgv":143,"date":1684132303223,"delta":-0.5,"direction":"Flat"}]"""

        // Act
        val result = NightscoutEntryParser.parse(entryWithDate)

        // Assert
        assertTrue("Should parse entry successfully", result is NightscoutParseResult.Success)
        if (result is NightscoutParseResult.Success) {
            assertEquals("Should use 'date' field", 1684132303223L, result.value.timestampMillis)
            assertEquals("Should parse sgv", 143, result.value.sgv)
        }
    }

    @Test
    fun delayedModeDetection_shouldTriggerAfter60Seconds() {
        // Arrange
        val now = System.currentTimeMillis()
        val readingTimeMs = now - (5 * 60 * 1000L)  // 5 minutes old (fresh)
        val receivedAtMs = now - (61 * 1000L)       // Received 61 seconds ago

        // Act
        val delay = NightscoutScheduler.calculateNextFetchDelay(
            lastReadingTimestampMs = readingTimeMs,
            lastReadingReceivedAt = receivedAtMs,
            timeNow = now,
            failureCount = 0
        )

        // Assert - should switch to delayed mode: 15 second interval
        assertEquals("Should switch to delayed mode", 15 * 1000L, delay)
    }
}
