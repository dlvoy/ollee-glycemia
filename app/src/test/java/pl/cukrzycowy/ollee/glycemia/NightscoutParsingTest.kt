package pl.cukrzycowy.ollee.glycemia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Tests for URL normalization and token validation.
 */
class NightscoutUrlNormalizerTest {

    @Test
    fun normalize_missingScheme_prependsHttps() {
        val result = NightscoutUrlNormalizer.normalize("user.ns.example.com")
        assertTrue(result is NightscoutConfigValidationResult.Valid)
        val valid = result as NightscoutConfigValidationResult.Valid
        assertEquals("https://user.ns.example.com", valid.normalized.baseUrl)
        assertNull(valid.normalized.extractedToken)
    }

    @Test
    fun normalize_withHttpsScheme_preservesIt() {
        val result = NightscoutUrlNormalizer.normalize("https://example.org")
        assertTrue(result is NightscoutConfigValidationResult.Valid)
        val valid = result as NightscoutConfigValidationResult.Valid
        assertEquals("https://example.org", valid.normalized.baseUrl)
    }

    @Test
    fun normalize_withTrailingSlash_removesIt() {
        val result = NightscoutUrlNormalizer.normalize("https://example.org/")
        assertTrue(result is NightscoutConfigValidationResult.Valid)
        val valid = result as NightscoutConfigValidationResult.Valid
        assertEquals("https://example.org", valid.normalized.baseUrl)
    }

    @Test
    fun normalize_withTokenInQuery_extractsToken() {
        val result = NightscoutUrlNormalizer.normalize("https://example.org/?token=sync-1234567890abcdef")
        assertTrue(result is NightscoutConfigValidationResult.Valid)
        val valid = result as NightscoutConfigValidationResult.Valid
        assertEquals("https://example.org", valid.normalized.baseUrl)
        assertEquals("sync-1234567890abcdef", valid.normalized.extractedToken)
    }

    @Test
    fun normalize_withApiV1Path_stripsIt() {
        val result = NightscoutUrlNormalizer.normalize("https://example.org/api/v1/")
        assertTrue(result is NightscoutConfigValidationResult.Valid)
        val valid = result as NightscoutConfigValidationResult.Valid
        assertEquals("https://example.org", valid.normalized.baseUrl)
    }

    @Test
    fun normalize_withApiV1StatusJsonEndpoint_stripsItCompletely() {
        val result = NightscoutUrlNormalizer.normalize("https://example.org/api/v1/status.json")
        assertTrue(result is NightscoutConfigValidationResult.Valid)
        val valid = result as NightscoutConfigValidationResult.Valid
        assertEquals("https://example.org", valid.normalized.baseUrl)
    }

    @Test
    fun normalize_withApiV1EntriesJsonEndpoint_stripsItCompletely() {
        val result = NightscoutUrlNormalizer.normalize("https://example.org/api/v1/entries.json")
        assertTrue(result is NightscoutConfigValidationResult.Valid)
        val valid = result as NightscoutConfigValidationResult.Valid
        assertEquals("https://example.org", valid.normalized.baseUrl)
    }

    @Test
    fun normalize_withCustomPort_preservesIt() {
        val result = NightscoutUrlNormalizer.normalize("https://example.org:8443/api/v1/status.json")
        assertTrue(result is NightscoutConfigValidationResult.Valid)
        val valid = result as NightscoutConfigValidationResult.Valid
        assertEquals("https://example.org:8443", valid.normalized.baseUrl)
    }

    @Test
    fun normalize_withHttpScheme_acceptsIt() {
        val result = NightscoutUrlNormalizer.normalize("http://example.org")
        assertTrue(result is NightscoutConfigValidationResult.Valid)
        val valid = result as NightscoutConfigValidationResult.Valid
        assertEquals("http://example.org", valid.normalized.baseUrl)
    }

    @Test
    fun normalize_withInvalidScheme_rejectsIt() {
        val result = NightscoutUrlNormalizer.normalize("ftp://example.org")
        assertTrue(result is NightscoutConfigValidationResult.Invalid)
    }

    @Test
    fun normalize_emptyUrl_rejectsIt() {
        val result = NightscoutUrlNormalizer.normalize("")
        assertTrue(result is NightscoutConfigValidationResult.Invalid)
    }

    @Test
    fun normalize_onlyScheme_rejectsIt() {
        val result = NightscoutUrlNormalizer.normalize("https://")
        assertTrue(result is NightscoutConfigValidationResult.Invalid)
    }

    @Test
    fun normalize_trimsWhitespace() {
        val result = NightscoutUrlNormalizer.normalize("  https://example.org  ")
        assertTrue(result is NightscoutConfigValidationResult.Valid)
        val valid = result as NightscoutConfigValidationResult.Valid
        assertEquals("https://example.org", valid.normalized.baseUrl)
    }

    @Test
    fun validateTokenFormat_validToken_returnsNull() {
        val warning = NightscoutUrlNormalizer.validateTokenFormat("sync-1234567890abcdef")
        assertNull(warning)
    }

    @Test
    fun validateTokenFormat_multiLetterPrefix_returnsNull() {
        val warning = NightscoutUrlNormalizer.validateTokenFormat("abc-1234567890abcdef")
        assertNull(warning)
    }

    @Test
    fun validateTokenFormat_emptyToken_returnsWarning() {
        val warning = NightscoutUrlNormalizer.validateTokenFormat("")
        assertTrue(warning?.contains("empty") == true)
    }

    @Test
    fun validateTokenFormat_invalidLength_returnsWarning() {
        val warning = NightscoutUrlNormalizer.validateTokenFormat("sync-123")
        assertTrue(warning != null)
    }

    @Test
    fun validateTokenFormat_singleLetterPrefix_returnsWarning() {
        val warning = NightscoutUrlNormalizer.validateTokenFormat("s-1234567890abcdef")
        assertTrue(warning != null)
    }

    @Test
    fun validateTokenFormat_uppercase_returnsWarning() {
        val warning = NightscoutUrlNormalizer.validateTokenFormat("SYNC-1234567890ABCDEF")
        assertTrue(warning != null)
    }
}

/**
 * Tests for Nightscout entry parsing.
 */
class NightscoutEntryParserTest {

    @Test
    fun parse_validEntry_succeeds() {
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
    fun parse_emptyArray_fails() {
        val json = "[]"
        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun parse_missingSgv_fails() {
        val json = """
            [{
                "delta": 1.0,
                "date": 1784132303223
            }]
        """.trimIndent()

        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun parse_sgvZero_fails() {
        val json = """
            [{
                "sgv": 0,
                "date": 1784132303223
            }]
        """.trimIndent()

        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun parse_noTimestamp_fails() {
        val json = """
            [{
                "sgv": 143
            }]
        """.trimIndent()

        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun parse_usesDateField() {
        val json = """
            [{
                "sgv": 143,
                "date": 1000000,
                "mills": 2000000
            }]
        """.trimIndent()

        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
        val entry = (result as NightscoutParseResult.Success).value
        assertEquals(1000000L, entry.timestampMillis)
    }

    @Test
    fun parse_fallsBackToMills() {
        val json = """
            [{
                "sgv": 143,
                "mills": 2000000
            }]
        """.trimIndent()

        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
        val entry = (result as NightscoutParseResult.Success).value
        assertEquals(2000000L, entry.timestampMillis)
    }

    @Test
    fun parse_nullDelta_acceptsIt() {
        val json = """
            [{
                "sgv": 143,
                "delta": null,
                "date": 1784132303223
            }]
        """.trimIndent()

        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
        val entry = (result as NightscoutParseResult.Success).value
        assertNull(entry.delta)
    }

    @Test
    fun parse_missingDirection_acceptsIt() {
        val json = """
            [{
                "sgv": 143,
                "date": 1784132303223
            }]
        """.trimIndent()

        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
        val entry = (result as NightscoutParseResult.Success).value
        assertNull(entry.direction)
    }

    @Test
    fun parse_invalidJson_fails() {
        val json = "not json"
        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun parse_jsonObject_fails() {
        val json = """{"sgv": 143}"""
        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun parse_dateStringField_acceptsIt() {
        val json = """
            [{
                "sgv": 143,
                "dateString": "2026-07-15T16:18:23.223Z"
            }]
        """.trimIndent()

        val result = NightscoutEntryParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
        val entry = (result as NightscoutParseResult.Success).value
        assertTrue(entry.timestampMillis > 0)
    }
}

/**
 * Tests for Nightscout status parsing.
 */
class NightscoutStatusParserTest {

    @Test
    fun parse_validStatus_succeeds() {
        val json = """
            {
                "status": "ok",
                "version": "15.0.6"
            }
        """.trimIndent()

        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
        val status = (result as NightscoutParseResult.Success).value
        assertEquals("ok", status.status)
        assertEquals("15.0.6", status.version)
    }

    @Test
    fun parse_withTitle_includesIt() {
        val json = """
            {
                "status": "ok",
                "version": "15.0.6",
                "title": "My Nightscout"
            }
        """.trimIndent()

        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
        val status = (result as NightscoutParseResult.Success).value
        assertEquals("My Nightscout", status.title)
    }

    @Test
    fun parse_missingStatus_fails() {
        val json = """
            {
                "version": "15.0.6"
            }
        """.trimIndent()

        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun parse_statusNotOk_fails() {
        val json = """
            {
                "status": "error",
                "version": "15.0.6"
            }
        """.trimIndent()

        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun parse_missingVersion_fails() {
        val json = """
            {
                "status": "ok"
            }
        """.trimIndent()

        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun parse_version14_fails() {
        val json = """
            {
                "status": "ok",
                "version": "14.2.0"
            }
        """.trimIndent()

        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun parse_version15_succeeds() {
        val json = """
            {
                "status": "ok",
                "version": "15.0.0"
            }
        """.trimIndent()

        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
    }

    @Test
    fun parse_version16_succeeds() {
        val json = """
            {
                "status": "ok",
                "version": "16.5.0"
            }
        """.trimIndent()

        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
    }

    @Test
    fun parse_invalidJson_fails() {
        val json = "not json"
        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun parse_jsonArray_fails() {
        val json = "[{}]"
        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Failure)
    }

    @Test
    fun parse_nullTitle_acceptsIt() {
        val json = """
            {
                "status": "ok",
                "version": "15.0.6",
                "title": null
            }
        """.trimIndent()

        val result = NightscoutStatusParser.parse(json)
        assertTrue(result is NightscoutParseResult.Success)
        val status = (result as NightscoutParseResult.Success).value
        assertNull(status.title)
    }
}
