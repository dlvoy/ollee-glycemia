package pl.cukrzycowy.ollee.glycemia

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ForgeDebugRelayTest {

    private var now = 1_000_000_000L
    private lateinit var relay: ForgeDebugRelay
    private var port = 0

    @Before
    fun setUp() {
        relay = ForgeDebugRelay { now }
        port = relay.start(0)
    }

    @After
    fun tearDown() = relay.stop()

    private fun request(path: String, method: String = "GET"): Pair<HttpURLConnection, String> {
        val c = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
        c.requestMethod = method
        val code = c.responseCode
        val body = (if (code < 400) c.inputStream else c.errorStream).bufferedReader().readText()
        return c to body
    }

    private fun frames(after: Long = 0): List<Pair<Long, String>> {
        val json = JSONObject(request("/frames?after=$after").second)
        val array = json.getJSONArray("frames")
        return (0 until array.length()).map { array.getJSONObject(it).let { f -> f.getLong("seq") to f.getString("data") } }
    }

    @Test
    fun servesFramesAsJsonWithCorsHeaders() {
        relay.submit(WatchPayload("120 -5", measuredAtMs = now - 83_000))
        val (conn, body) = request("/frames")
        assertEquals(200, conn.responseCode)
        assertEquals("*", conn.getHeaderField("Access-Control-Allow-Origin"))
        assertEquals("true", conn.getHeaderField("Access-Control-Allow-Private-Network"))
        val json = JSONObject(body)
        assertTrue(json.getBoolean("link"))
        assertEquals(1L, json.getLong("seq"))
        assertEquals("G83,120 -5", json.getJSONArray("frames").getJSONObject(0).getString("data"))
    }

    @Test
    fun afterParameterFiltersOldFrames() {
        relay.submit(WatchPayload("120 -5", measuredAtMs = now))
        relay.submit(WatchPayload("121  1", measuredAtMs = now))
        assertEquals(listOf(1L to "G0,120 -5", 2L to "G0,121  1"), frames(0))
        assertEquals(listOf(2L to "G0,121  1"), frames(1))
        assertTrue(frames(2).isEmpty())
    }

    @Test
    fun preflightIsAnswered() {
        val (conn, _) = request("/frames", "OPTIONS")
        assertEquals(204, conn.responseCode)
        assertEquals("*", conn.getHeaderField("Access-Control-Allow-Origin"))
        assertTrue(conn.getHeaderField("Access-Control-Allow-Methods").contains("GET"))
    }

    @Test
    fun unknownPathIs404() {
        assertEquals(404, request("/nope").first.responseCode)
    }

    @Test
    fun keepaliveFollowsTheFeedPolicy() {
        relay.submit(WatchPayload("120 -5", measuredAtMs = now))
        now += ForgeFeed.KEEPALIVE_MS - 1
        relay.tick()
        assertEquals(1, frames().size)
        now += 1
        relay.tick()
        assertEquals(listOf(1L to "G0,120 -5", 2L to "G300,120 -5"), frames())
    }

    @Test
    fun linkOffEmitsNothingAndLinkOnResends() {
        relay.submit(WatchPayload("120 -5", measuredAtMs = now))
        relay.setLink(false)
        relay.submit(WatchPayload("121  1", measuredAtMs = now))
        assertEquals(1, frames().size)
        assertFalse(JSONObject(request("/frames").second).getBoolean("link"))
        relay.setLink(true)
        assertEquals(listOf(1L to "G0,120 -5", 2L to "G0,121  1"), frames())
    }

    @Test
    fun silencedRelayIgnoresReadingsButAgeKeepsGrowingInKeepalives() {
        relay.submit(WatchPayload("120 -5", measuredAtMs = now))
        relay.setSilenced(true)
        relay.submit(WatchPayload("130  9", measuredAtMs = now))
        now += ForgeFeed.KEEPALIVE_MS
        relay.tick()
        assertEquals(listOf(1L to "G0,120 -5", 2L to "G300,120 -5"), frames())
    }

    @Test
    fun mutedRelayStaysQuietEvenForKeepalivesAndResumesWithAFrame() {
        relay.submit(WatchPayload("120 -5", measuredAtMs = now))
        relay.setMuted(true)
        now += ForgeFeed.KEEPALIVE_MS * 3
        relay.tick()
        relay.submit(WatchPayload("121  1", measuredAtMs = now))
        assertEquals(1, frames().size)
        relay.setMuted(false)
        assertEquals(listOf(1L to "G0,120 -5", 2L to "G0,121  1"), frames())
    }

    @Test
    fun pauseSendsLabelFrameThenResumeSendsLiveAgain() {
        relay.submit(WatchPayload("120 -5", measuredAtMs = now))
        relay.setPaused(true)
        relay.setPaused(false)
        assertEquals(listOf(1L to "G0,120 -5", 2L to "P, pause", 3L to "G0,120 -5"), frames())
    }

    @Test
    fun ringBufferKeepsTheLastFiftyFrames() {
        repeat(60) { relay.submit(WatchPayload("1${it % 10}0   ", measuredAtMs = now - it * 1000L)) }
        val all = frames()
        assertEquals(ForgeDebugRelay.MAX_FRAMES, all.size)
        assertEquals(60L, all.last().first)
        assertEquals(11L, all.first().first)
    }

    @Test
    fun stoppedRelayIgnoresSubmissions() {
        relay.stop()
        relay.submit(WatchPayload("120 -5"))
        assertFalse(relay.isRunning)
        assertTrue(relay.framesJson(0).contains("\"frames\":[]"))
    }
}
