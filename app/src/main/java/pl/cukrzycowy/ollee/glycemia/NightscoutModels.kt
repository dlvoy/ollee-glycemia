package pl.cukrzycowy.ollee.glycemia

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Nightscout API response models and parsers.
 */

data class NightscoutEntry(
    val sgv: Int,
    val delta: Double?,
    val direction: String?,
    val timestampMillis: Long
)

data class NightscoutStatus(
    val status: String,
    val version: String,
    val title: String?
)

sealed class NightscoutParseResult<T> {
    data class Success<T>(val value: T) : NightscoutParseResult<T>()
    data class Failure<T>(val reason: String) : NightscoutParseResult<T>()
}

object NightscoutEntryParser {
    /**
     * Parse a Nightscout entries response array.
     * Expected: GET /api/v1/entries.json?count=1 returns [{ sgv, delta, direction, date/mills/dateString, ... }]
     *
     * Rules:
     * - Response must be a JSON array
     * - Empty array -> Failure
     * - First element must have numeric sgv
     * - Prefer date (milliseconds since epoch)
     * - Fallback to mills if date is missing or <= 0
     * - Fallback to parsing dateString or sysTime if both date and mills are missing
     * - Map direction to trend string (uppercase)
     */
    fun parse(jsonText: String): NightscoutParseResult<NightscoutEntry> {
        return try {
            val array = JSONArray(jsonText)
            
            if (array.length() == 0) {
                return NightscoutParseResult.Failure("Empty entries array")
            }

            val entry = array.getJSONObject(0)
            
            // Extract SGV
            if (!entry.has("sgv")) {
                return NightscoutParseResult.Failure("Missing sgv field")
            }
            val sgv = try {
                entry.getInt("sgv")
            } catch (e: Exception) {
                return NightscoutParseResult.Failure("Invalid sgv value: ${e.message}")
            }
            
            if (sgv <= 0) {
                return NightscoutParseResult.Failure("Invalid sgv value: $sgv")
            }

            // Extract delta
            val delta = try {
                if (entry.has("delta") && !entry.isNull("delta")) {
                    entry.getDouble("delta")
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }

            // Extract direction
            val direction = try {
                if (entry.has("direction") && !entry.isNull("direction")) {
                    entry.getString("direction")
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }

            // Extract timestamp
            val timestampMillis = extractTimestamp(entry)
            if (timestampMillis <= 0) {
                return NightscoutParseResult.Failure("Could not extract valid timestamp")
            }

            return NightscoutParseResult.Success(
                NightscoutEntry(
                    sgv = sgv,
                    delta = delta,
                    direction = direction,
                    timestampMillis = timestampMillis
                )
            )
        } catch (e: JSONException) {
            NightscoutParseResult.Failure("Invalid JSON: ${e.message}")
        } catch (e: Exception) {
            NightscoutParseResult.Failure("Parse error: ${e.message}")
        }
    }

    private fun extractTimestamp(entry: JSONObject): Long {
        // Try date first (milliseconds)
        if (entry.has("date") && !entry.isNull("date")) {
            try {
                val date = entry.getLong("date")
                if (date > 0) return date
            } catch (e: Exception) {
                // Ignore and try next
            }
        }

        // Try mills
        if (entry.has("mills") && !entry.isNull("mills")) {
            try {
                val mills = entry.getLong("mills")
                if (mills > 0) return mills
            } catch (e: Exception) {
                // Ignore and try next
            }
        }

        // Try dateString or sysTime as fallback
        val dateStrings = listOf("dateString", "sysTime")
        for (field in dateStrings) {
            if (entry.has(field) && !entry.isNull(field)) {
                try {
                    val dateStr = entry.getString(field)
                    return parseRFC3339(dateStr)
                } catch (e: Exception) {
                    // Ignore and try next
                }
            }
        }

        return 0L
    }

    private fun parseRFC3339(dateStr: String): Long {
        return try {
            // Simple RFC 3339 parser for format: 2026-07-15T16:18:23.223Z
            // For production, use java.time.Instant if available
            val instant = java.time.Instant.parse(dateStr)
            instant.toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }
}

object NightscoutStatusParser {
    /**
     * Parse Nightscout status response.
     * Expected: GET /api/v1/status.json returns { status, version, title?, ... }
     *
     * Rules:
     * - Response must be a JSON object
     * - Must have status == "ok"
     * - Must have version field
     * - Version must be 15.x.x or newer
     */
    fun parse(jsonText: String): NightscoutParseResult<NightscoutStatus> {
        return try {
            val obj = JSONObject(jsonText)

            if (!obj.has("status")) {
                return NightscoutParseResult.Failure("Missing status field")
            }

            val status = obj.getString("status")
            if (status != "ok") {
                return NightscoutParseResult.Failure("Status is not 'ok': $status")
            }

            if (!obj.has("version")) {
                return NightscoutParseResult.Failure("Missing version field")
            }

            val version = obj.getString("version")
            if (!isVersionSupported(version)) {
                return NightscoutParseResult.Failure("Unsupported Nightscout version: $version (require 15.x.x or newer)")
            }

            val title = try {
                if (obj.has("title") && !obj.isNull("title")) obj.getString("title") else null
            } catch (e: Exception) {
                null
            }

            return NightscoutParseResult.Success(
                NightscoutStatus(
                    status = status,
                    version = version,
                    title = title
                )
            )
        } catch (e: JSONException) {
            NightscoutParseResult.Failure("Invalid JSON: ${e.message}")
        } catch (e: Exception) {
            NightscoutParseResult.Failure("Parse error: ${e.message}")
        }
    }

    private fun isVersionSupported(version: String): Boolean {
        return try {
            val parts = version.split(".")
            if (parts.isEmpty()) return false
            val major = parts[0].toIntOrNull() ?: return false
            major >= 15
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Status validator for already-parsed Nightscout status.
 * (Different from parser - takes structured data, not JSON text)
 */
object NightscoutStatusValidator {
    fun validate(status: NightscoutStatus): NightscoutStatusValidationResult {
        return if (status.version.isBlank()) {
            NightscoutStatusValidationResult.Invalid(
                NightscoutConnectionStatus.INVALID_RESPONSE,
                "Version field missing or empty"
            )
        } else if (!isVersionSupported(status.version)) {
            NightscoutStatusValidationResult.Invalid(
                NightscoutConnectionStatus.UNSUPPORTED_VERSION,
                "Nightscout version must be 15.x.x or newer (found: ${status.version})"
            )
        } else {
            NightscoutStatusValidationResult.Valid(
                version = status.version,
                title = status.title
            )
        }
    }

    private fun isVersionSupported(version: String): Boolean {
        return try {
            val parts = version.split(".")
            if (parts.isEmpty()) return false
            val major = parts[0].toIntOrNull() ?: return false
            major >= 15
        } catch (e: Exception) {
            false
        }
    }
}

// Result types for status validation
sealed class NightscoutStatusValidationResult {
    data class Valid(val version: String, val title: String?) :
        NightscoutStatusValidationResult()
    data class Invalid(val status: NightscoutConnectionStatus, val detail: String) :
        NightscoutStatusValidationResult()
}

enum class NightscoutConnectionStatus {
    OK,
    INVALID_URL,
    INVALID_AUTH,
    INVALID_CONFIG,
    CONNECTION_ERROR,
    INVALID_RESPONSE,
    UNSUPPORTED_VERSION,
    SERVER_ERROR
}
