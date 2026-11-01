package pl.cukrzycowy.ollee.glycemia

/**
 * Decides when a Forge frame has to be sent and builds it. Pure policy, shared by
 * [WatchConnection] and the debug relay. A frame is due when the payload changed, after
 * [reset] (a reconnect or a failed write), or when [keepaliveMs] passed since the last frame.
 *
 * The age in a frame is computed in [nextFrame], i.e. when the write is actually dequeued.
 */
class ForgeFeed(private val keepaliveMs: Long = KEEPALIVE_MS) {

    companion object {
        /** The face treats 11 minutes of silence as "app lost", so stay well below that. */
        const val KEEPALIVE_MS = 5 * 60_000L
    }

    private var payload: WatchPayload? = null
    private var emitted: WatchPayload? = null
    private var lastEmitAtMs = 0L
    private var forced = true

    fun submit(payload: WatchPayload) {
        this.payload = payload
    }

    /** The next [due] check returns true (if there is a payload). */
    fun reset() {
        forced = true
    }

    fun hasPayload(): Boolean = payload != null

    fun due(nowMs: Long): Boolean {
        val p = payload ?: return false
        return forced || p != emitted || nowMs - lastEmitAtMs >= keepaliveMs
    }

    /** Builds the frame for the current payload and marks it as sent. Null without a payload. */
    fun nextFrame(nowMs: Long): String? {
        val p = payload ?: return null
        emitted = p
        forced = false
        lastEmitAtMs = nowMs
        return if (p.paused) {
            ForgeProtocol.pausedFrame(p.text)
        } else {
            ForgeProtocol.glycemiaFrame(p.measuredAtMs?.let { (nowMs - it) / 1000 }, p.text)
        }
    }
}
