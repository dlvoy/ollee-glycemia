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
            Log.d(tag, "→ Status request: $url")
            Log.d(tag, "→ Token provided: ${!token.isNullOrBlank()}")
            
            val response = makeRequest(url)
            Log.d(tag, "← Status response code: ${response.code}")
            
            when {
                response.code == 401 -> {
                    val body = response.body?.string() ?: ""
                    Log.e(tag, "← Auth failed (401). Response: $body")
                    NightscoutParseResult.Failure("Authentication failed: invalid token")
                }
                response.code == 403 -> {
                    val body = response.body?.string() ?: ""
                    Log.e(tag, "← Access denied (403). Response: $body")
                    NightscoutParseResult.Failure("Access denied: insufficient permissions")
                }
                response.code >= 500 -> {
                    val body = response.body?.string() ?: ""
                    Log.e(tag, "← Server error (${response.code}). Response: $body")
                    NightscoutParseResult.Failure("Server error: HTTP ${response.code}")
                }
                response.code != 200 -> {
                    val body = response.body?.string() ?: ""
                    Log.e(tag, "← HTTP error ${response.code}. Response: $body")
                    NightscoutParseResult.Failure("HTTP error ${response.code}")
                }
                else -> {
                    val body = response.body?.string() ?: return NightscoutParseResult.Failure("Empty response body")
                    Log.d(tag, "← Success (200). Response: $body")
                    NightscoutStatusParser.parse(body)
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(tag, "× Timeout: ${e.message}")
            NightscoutParseResult.Failure("Connection timeout")
        } catch (e: java.net.ConnectException) {
            Log.e(tag, "× Connection failed: ${e.message}")
            NightscoutParseResult.Failure("Connection failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "× Status request failed: ${e.message}", e)
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
            Log.d(tag, "→ Entries request: $url")
            Log.d(tag, "→ Token provided: ${!token.isNullOrBlank()}")
            
            val response = makeRequest(url)
            Log.d(tag, "← Entries response code: ${response.code}")

            when {
                response.code == 401 -> {
                    val body = response.body?.string() ?: ""
                    Log.e(tag, "← Auth failed (401). Response: $body")
                    NightscoutParseResult.Failure("Authentication failed: invalid token")
                }
                response.code == 403 -> {
                    val body = response.body?.string() ?: ""
                    Log.e(tag, "← Access denied (403). Response: $body")
                    NightscoutParseResult.Failure("Access denied: insufficient permissions")
                }
                response.code >= 500 -> {
                    val body = response.body?.string() ?: ""
                    Log.e(tag, "← Server error (${response.code}). Response: $body")
                    NightscoutParseResult.Failure("Server error: HTTP ${response.code}")
                }
                response.code != 200 -> {
                    val body = response.body?.string() ?: ""
                    Log.e(tag, "← HTTP error ${response.code}. Response: $body")
                    NightscoutParseResult.Failure("HTTP error ${response.code}")
                }
                else -> {
                    val body = response.body?.string() ?: return NightscoutParseResult.Failure("Empty response body")
                    Log.d(tag, "← Success (200). Response: $body")
                    NightscoutEntryParser.parse(body)
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(tag, "× Timeout: ${e.message}")
            NightscoutParseResult.Failure("Connection timeout")
        } catch (e: java.net.ConnectException) {
            Log.e(tag, "× Connection failed: ${e.message}")
            NightscoutParseResult.Failure("Connection failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "× Entries request failed: ${e.message}", e)
            NightscoutParseResult.Failure("Network error: ${e.message}")
        }
    }

    private fun makeRequest(url: String): okhttp3.Response {
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "OlleeGlycemia/1.0")

        // Add token if provided (Nightscout uses api-secret header)
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("api-secret", token)
            val maskedToken = if (token.length > 4) token.take(4) + "..." else token
            Log.d(tag, "→ api-secret header: $maskedToken (length: ${token.length})")
        } else {
            Log.w(tag, "→ No token provided")
        }

        val request = requestBuilder.build()
        Log.d(tag, "→ Request URL: ${request.url}")
        Log.d(tag, "→ Request method: ${request.method}")
        request.headers.forEach { (name, value) ->
            if (name == "api-secret") {
                Log.d(tag, "→ Header: $name: ${value.take(20)}...")
            } else {
                Log.d(tag, "→ Header: $name: $value")
            }
        }
        
        return httpClient.newCall(request).execute()
    }

    private fun normalizeUrl(url: String): String {
        // Ensure no double slashes except in scheme
        return url.replace(Regex("([^:])//+"), "$1/")
    }
}
