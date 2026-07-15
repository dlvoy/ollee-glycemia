package pl.cukrzycowy.ollee.glycemia

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * HTTP client for Nightscout API with error handling and classification.
 *
 * Responsibilities:
 * - Make authenticated HTTP requests to Nightscout API
 * - Handle errors with proper classification (auth, network, validation, etc.)
 * - Parse responses using NightscoutEntryParser and NightscoutStatusParser
 * - Return structured results via NightscoutParseResult
 */
class NightscoutHttpClient(
    private val baseUrl: String,
    private val token: String?
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val tag = "NightscoutHttp"

    /**
     * Fetch Nightscout status (server version, title).
     * GET /api/v1/status.json
     *
     * Returns: Success(NightscoutStatus) or Failure(reason)
     */
    fun status(): NightscoutParseResult<NightscoutStatus> {
        return try {
            val url = normalizeUrl("$baseUrl/api/v1/status.json")
            val response = makeRequest(url)

            when {
                response.code == 401 -> {
                    NightscoutParseResult.Failure("Authentication failed: invalid token")
                }
                response.code == 403 -> {
                    NightscoutParseResult.Failure("Access denied: insufficient permissions")
                }
                response.code >= 500 -> {
                    NightscoutParseResult.Failure("Server error: HTTP ${response.code}")
                }
                response.code != 200 -> {
                    NightscoutParseResult.Failure("HTTP error ${response.code}")
                }
                else -> {
                    val body = response.body?.string() ?: return NightscoutParseResult.Failure("Empty response body")
                    NightscoutStatusParser.parse(body)
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            NightscoutParseResult.Failure("Connection timeout")
        } catch (e: java.net.ConnectException) {
            NightscoutParseResult.Failure("Connection failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "Status request failed", e)
            NightscoutParseResult.Failure("Network error: ${e.message}")
        }
    }

    /**
     * Fetch latest glucose entry.
     * GET /api/v1/entries.json?count=1
     *
     * Returns: Success(NightscoutEntry) or Failure(reason)
     */
    fun entries(count: Int = 1): NightscoutParseResult<NightscoutEntry> {
        return try {
            val url = normalizeUrl("$baseUrl/api/v1/entries.json?count=$count")
            val response = makeRequest(url)

            when {
                response.code == 401 -> {
                    NightscoutParseResult.Failure("Authentication failed: invalid token")
                }
                response.code == 403 -> {
                    NightscoutParseResult.Failure("Access denied: insufficient permissions")
                }
                response.code >= 500 -> {
                    NightscoutParseResult.Failure("Server error: HTTP ${response.code}")
                }
                response.code != 200 -> {
                    NightscoutParseResult.Failure("HTTP error ${response.code}")
                }
                else -> {
                    val body = response.body?.string() ?: return NightscoutParseResult.Failure("Empty response body")
                    NightscoutEntryParser.parse(body)
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            NightscoutParseResult.Failure("Connection timeout")
        } catch (e: java.net.ConnectException) {
            NightscoutParseResult.Failure("Connection failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "Entries request failed", e)
            NightscoutParseResult.Failure("Network error: ${e.message}")
        }
    }

    private fun makeRequest(url: String): okhttp3.Response {
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "OlleeGlycemia/1.0")

        // Add token if provided
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        return httpClient.newCall(request).execute()
    }

    private fun normalizeUrl(url: String): String {
        // Ensure no double slashes except in scheme
        return url.replace(Regex("([^:])//+"), "$1/")
    }
}
