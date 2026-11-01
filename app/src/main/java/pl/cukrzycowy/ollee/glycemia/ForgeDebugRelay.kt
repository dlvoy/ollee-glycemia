package pl.cukrzycowy.ollee.glycemia

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Debug-only stand-in for a Forge watch: receives the same [WatchPayload]s as a real watch
 * connection, turns them into Forge frames with [ForgeFeed], and serves them over HTTP on
 * 127.0.0.1 so `tools/forge-bridge` can type them into forge_emulator. The Android emulator has
 * no paired watch, so this is how the app side is exercised end to end without hardware.
 *
 * Every caller guards it with `BuildConfig.DEBUG`; nothing starts it in release builds.
 *
 *  - `GET /frames?after=<seq>` -> `{"link":true,"seq":N,"frames":[{"seq":n,"data":"G83,120 -5"}]}`
 *  - `OPTIONS *`               -> CORS preflight (the emulator page is served from another port)
 */
class ForgeDebugRelay(private val clock: () -> Long = System::currentTimeMillis) {

    companion object {
        const val DEFAULT_PORT = 8765
        const val MAX_FRAMES = 50
        private const val TAG = "ForgeDebug"
        val instance = ForgeDebugRelay()
    }

    private data class Frame(val seq: Long, val data: String)

    private var feed = ForgeFeed()
    private val frames = ArrayDeque<Frame>()
    private var seq = 0L
    private var lastLive: WatchPayload? = null
    private var server: ServerSocket? = null

    /** The simulated watch has a BLE connection to the "phone". */
    var link = true
        private set

    /** Readings are not forwarded, so the age grows (keepalives still go out). */
    var silenced = false
        private set

    /** The app stays "connected" but sends nothing at all (no readings, no keepalives): NO_APP on the face. */
    var muted = false
        private set

    /** The simulated watch is paused: only the pause label is sent. */
    var paused = false
        private set

    @get:Synchronized
    val isRunning: Boolean get() = server?.isClosed == false

    /** Starts the HTTP server (idempotent). Pass 0 for an ephemeral port. Returns the bound port. */
    @Synchronized
    fun start(port: Int = DEFAULT_PORT): Int {
        server?.takeIf { !it.isClosed }?.let { return it.localPort }
        val socket = ServerSocket(port, 8, InetAddress.getByName("127.0.0.1"))
        server = socket
        thread(name = "forge-relay", isDaemon = true) {
            while (!socket.isClosed) {
                try {
                    socket.accept().use { handle(it) }
                } catch (e: IOException) {
                    if (socket.isClosed) break
                    Log.w(TAG, "relay connection failed: ${e.message}")
                }
            }
        }
        Log.i(TAG, "relay listening on 127.0.0.1:${socket.localPort}")
        return socket.localPort
    }

    @Synchronized
    fun stop() {
        server?.close()
        server = null
        feed = ForgeFeed()
        frames.clear()
        lastLive = null
        link = true; silenced = false; paused = false; muted = false
    }

    /** Same data a real watch connection gets from the service. */
    @Synchronized
    fun submit(payload: WatchPayload) {
        if (!isRunning || silenced) return
        lastLive = payload
        if (!paused) {
            feed.submit(payload)
            emitIfDue()
        }
    }

    /** Called about once a minute by the service: sends the keepalive when due. */
    @Synchronized
    fun tick() {
        if (isRunning) emitIfDue()
    }

    @Synchronized
    fun setLink(on: Boolean) {
        if (link == on) return
        link = on
        if (on) {
            feed.reset() // a reconnect: the face gets a frame right away
            emitIfDue()
        }
    }

    @Synchronized
    fun setSilenced(on: Boolean) {
        silenced = on
    }

    @Synchronized
    fun setMuted(on: Boolean) {
        muted = on
        if (!on) feed.reset() // the app is back: the face gets a frame right away
        emitIfDue()
    }

    @Synchronized
    fun setPaused(on: Boolean) {
        if (paused == on) return
        paused = on
        if (on) {
            feed.submit(WatchPayload(" pause", measuredAtMs = null, paused = true))
        } else {
            lastLive?.let { feed.submit(it) }
            feed.reset()
        }
        emitIfDue()
    }

    @Synchronized
    fun status(): String =
        "running=$isRunning link=$link silenced=$silenced muted=$muted paused=$paused seq=$seq last=${frames.lastOrNull()?.data}"

    @Synchronized
    fun framesJson(after: Long): String {
        val array = JSONArray()
        frames.filter { it.seq > after }.forEach {
            array.put(JSONObject().put("seq", it.seq).put("data", it.data))
        }
        return JSONObject().put("link", link).put("seq", seq).put("frames", array).toString()
    }

    private fun emitIfDue() {
        if (!link || muted) return
        val now = clock()
        if (!feed.due(now)) return
        val data = feed.nextFrame(now) ?: return
        frames.addLast(Frame(++seq, data))
        while (frames.size > MAX_FRAMES) frames.removeFirst()
        Log.i(TAG, "frame #$seq: $data")
    }

    private fun handle(socket: Socket) {
        socket.soTimeout = 2000
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.ISO_8859_1))
        val requestLine = reader.readLine() ?: return
        while (true) { // skip the headers
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
        }
        val parts = requestLine.split(" ")
        val method = parts.getOrNull(0).orEmpty()
        val target = parts.getOrNull(1).orEmpty()
        val path = target.substringBefore('?')
        val query = target.substringAfter('?', "")

        val (status, body) = when {
            method == "OPTIONS" -> "204 No Content" to ""
            method == "GET" && path == "/frames" -> {
                val after = query.split('&').firstOrNull { it.startsWith("after=") }
                    ?.substringAfter('=')?.toLongOrNull() ?: 0L
                "200 OK" to framesJson(after)
            }
            else -> "404 Not Found" to "{}"
        }
        val bytes = body.toByteArray(Charsets.UTF_8)
        val head = buildString {
            append("HTTP/1.1 $status\r\n")
            append("Content-Type: application/json; charset=utf-8\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Private-Network: true\r\n")
            append("Access-Control-Allow-Methods: GET, OPTIONS\r\n")
            append("Access-Control-Allow-Headers: *\r\n")
            append("Cache-Control: no-store\r\n")
            append("Connection: close\r\n\r\n")
        }
        socket.getOutputStream().apply {
            write(head.toByteArray(Charsets.ISO_8859_1))
            write(bytes)
            flush()
        }
    }
}
