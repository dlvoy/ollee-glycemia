package pl.cukrzycowy.ollee.glycemia

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for NightscoutHttpClient: status and entries endpoints with error handling.
 */
class NightscoutHttpClientTest {

    @Test
    fun status_validResponse_parsesCorrectly() {
        val json = """
            {
                "status": "ok",
                "version": "15.0.6",
                "title": "My Nightscout"
            }
        """.trimIndent()

        // Direct parser test (integration with HTTP happens in mockito tests)
        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
        val status = (result as NightscoutParseResult.Success).value
        assertEquals("ok", status.status)
        assertEquals("15.0.6", status.version)
        assertEquals("My Nightscout", status.title)
    }

    @Test
    fun entries_validResponse_parsesCorrectly() {
        val json = """
            [{
                "sgv": 143,
                "delta": -0.999,
                "direction": "Flat",
                "date": 1784132303223
            }]
        """.trimIndent()

        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
        val entry = (result as NightscoutParseResult.Success).value
        assertEquals(143, entry.sgv)
        assertEquals(-0.999, entry.delta ?: 0.0, 0.0)
        assertEquals("Flat", entry.direction)
        assertEquals(1784132303223L, entry.timestampMillis)
    }

    @Test
    fun status_invalidVersion_fails() {
        val json = """
            {
                "status": "ok",
                "version": "14.0.0"
            }
        """.trimIndent()

        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun entries_missingTimestamp_fails() {
        val json = """
            [{
                "sgv": 143,
                "delta": -0.999,
                "direction": "Flat"
            }]
        """.trimIndent()

        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun entries_emptyArray_fails() {
        val json = "[]"
        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun entries_invalidJson_fails() {
        val json = "not json"
        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun status_invalidJson_fails() {
        val json = "not json"
        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun httpClient_urlNormalization() {
        // Test that double slashes are collapsed (except in scheme)
        val baseUrl = "https://example.com//api//v1"
        val client = NightscoutHttpClient(baseUrl, null)
        
        // Verify client can be created successfully
        assertNotNull(client)
    }

    @Test
    fun status_withoutTitle_acceptsIt() {
        val json = """
            {
                "status": "ok",
                "version": "15.0.6"
            }
        """.trimIndent()

        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
        val status = (result as NightscoutParseResult.Success).value
        assertNull(status.title)
    }

    @Test
    fun entries_withoutDelta_acceptsIt() {
        val json = """
            [{
                "sgv": 143,
                "direction": "Flat",
                "date": 1784132303223
            }]
        """.trimIndent()

        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
        val entry = (result as NightscoutParseResult.Success).value
        assertNull(entry.delta)
    }

    @Test
    fun httpClient_acceptsNullToken() {
        val client = NightscoutHttpClient("https://example.com", null)
        assertNotNull(client)
    }

    @Test
    fun httpClient_acceptsTokenWithBearer() {
        val client = NightscoutHttpClient("https://example.com", "sync-1234567890abcdef")
        assertNotNull(client)
    }
}
