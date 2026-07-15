package pl.cukrzycowy.ollee.glycemia

import java.net.URI
import java.net.URLDecoder

/**
 * Nightscout URL and token normalization.
 */

data class NormalizedNightscoutConfig(
    val baseUrl: String,
    val extractedToken: String?,
    val warning: String? = null
)

sealed class NightscoutConfigValidationResult {
    data class Valid(val normalized: NormalizedNightscoutConfig) : NightscoutConfigValidationResult()
    data class Invalid(val reason: String) : NightscoutConfigValidationResult()
}

object NightscoutUrlNormalizer {
    private val TOKEN_PATTERN = Regex("^[a-z]{2,}-[0-9a-z]{16}$")

    /**
     * Normalize and validate a Nightscout URL.
     *
     * Input cases handled:
     * - user.ns.example.com -> https://user.ns.example.com
     * - https://user.ns.example.com -> https://user.ns.example.com
     * - https://user.ns.example.com/?token=ollee-xxxxxxxxxxxxxxxx -> baseUrl, extracted token
     * - https://user.ns.example.com/api/v1/ -> https://user.ns.example.com
     * - https://user.ns.example.com:8443/api/v1/status.json -> https://user.ns.example.com:8443
     *
     * Normalization rules:
     * 1. Trim whitespace
     * 2. If missing scheme, prepend https://
     * 3. Parse as URI
     * 4. Require scheme http or https
     * 5. Require host
     * 6. Preserve port if present
     * 7. Extract query parameter token if present
     * 8. Strip query and fragment from base URL
     * 9. Strip Nightscout API paths (/api/v1, /api/v1/, /api/v1/status.json, /api/v1/entries.json)
     * 10. Remove trailing / from final base URL
     */
    fun normalize(url: String): NightscoutConfigValidationResult {
        if (url.isBlank()) {
            return NightscoutConfigValidationResult.Invalid("URL cannot be empty")
        }

        var input = url.trim()

        // Add scheme if missing
        if (!input.contains("://")) {
            input = "https://$input"
        }

        // Parse as URI
        val uri = try {
            URI(input)
        } catch (e: Exception) {
            return NightscoutConfigValidationResult.Invalid("Malformed URL: ${e.message}")
        }

        // Validate scheme
        val scheme = uri.scheme?.lowercase() ?: ""
        if (scheme !in listOf("http", "https")) {
            return NightscoutConfigValidationResult.Invalid("Invalid scheme: $scheme (must be http or https)")
        }

        // Require host
        val host = uri.host
        if (host.isNullOrBlank()) {
            return NightscoutConfigValidationResult.Invalid("URL must contain a host")
        }

        // Extract token from query parameter
        val extractedToken = extractTokenFromQuery(uri.query)

        // Build normalized base URL
        val port = if (uri.port > 0) ":${uri.port}" else ""
        var baseUrl = "$scheme://$host$port"

        // Strip API paths from the end of URI path
        var path = uri.path?.let { it.ifEmpty { "/" } } ?: "/"
        path = stripNightscoutApiPaths(path)

        if (path != "/") {
            baseUrl += path
        }

        // Remove trailing slash
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.dropLast(1)
        }

        return NightscoutConfigValidationResult.Valid(
            NormalizedNightscoutConfig(
                baseUrl = baseUrl,
                extractedToken = extractedToken,
                warning = null
            )
        )
    }

    private fun extractTokenFromQuery(query: String?): String? {
        if (query.isNullOrBlank()) return null

        return try {
            query.split("&").find { it.startsWith("token=") }?.let {
                val encoded = it.substringAfter("token=")
                URLDecoder.decode(encoded, "UTF-8")
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun stripNightscoutApiPaths(path: String): String {
        var result = path

        // Strip known Nightscout endpoints
        val pathsToStrip = listOf(
            "/api/v1/status.json",
            "/api/v1/entries.json",
            "/api/v1/",
            "/api/v1"
        )

        for (pathToStrip in pathsToStrip) {
            if (result.endsWith(pathToStrip)) {
                result = result.removeSuffix(pathToStrip)
                break
            }
        }

        return result.ifEmpty { "/" }
    }

    /**
     * Validate token format.
     * Returns a warning if token doesn't match expected format, otherwise null.
     * Warning is informational; the server is authoritative.
     */
    fun validateTokenFormat(token: String): String? {
        if (token.isBlank()) {
            return "Token cannot be empty"
        }

        if (!TOKEN_PATTERN.matches(token)) {
            return "Token format looks unusual. Expected format: ollee-xxxxxxxxxxxxxxxx or similar"
        }

        return null
    }
}
