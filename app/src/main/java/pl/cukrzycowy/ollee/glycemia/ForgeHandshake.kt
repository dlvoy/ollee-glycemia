package pl.cukrzycowy.ollee.glycemia

/**
 * Per-link state of the Forge face handshake (docs/watchface/PROTOCOL.md): says when a `Q` query
 * is due and turns a `H` reply, or a missing one, into a result to store. Pure policy, driven by
 * [WatchConnection] (which owns the [REPLY_TIMEOUT_MS] timer), like [ForgeFeed].
 *
 * A query is due once TX notifications are on ([onNotificationsEnabled]), and again on
 * [requestQuery]: while the face is not detected, or always when forced (the user tapped sync).
 */
class ForgeHandshake {

    companion object {
        /** Counted from the moment the `Q` write completed. */
        const val REPLY_TIMEOUT_MS = 5_000L
    }

    enum class Result { DETECTED, NOT_DETECTED }

    /** TX notifications are on, so a reply can arrive. */
    var ready = false
        private set
    var detected = false
        private set
    var faceVersion: Int? = null
        private set

    private var wanted = false
    private var pending = false // a query is queued or written, and not answered yet
    private var sent = false    // ...and its write completed: the reply timeout runs

    /** A new link: nothing known yet. */
    fun reset() {
        ready = false
        detected = false
        faceVersion = null
        wanted = false
        pending = false
        sent = false
    }

    fun onNotificationsEnabled() {
        ready = true
        wanted = true
    }

    /** Ask again. Ignored once the face is detected, unless [force]d. */
    fun requestQuery(force: Boolean = false) {
        if (force || !detected) wanted = true
    }

    fun queryDue(): Boolean = ready && wanted && !pending

    fun onQueryQueued() {
        wanted = false
        pending = true
    }

    /**
     * The `Q` write completed. Returns true when the reply timeout should start, false when the
     * reply already came in (a notification can beat the write callback).
     */
    fun onQuerySent(): Boolean {
        if (!pending) return false
        sent = true
        return true
    }

    /** The `Q` write failed: due again on the next check. */
    fun onQueryFailed() {
        pending = false
        sent = false
        wanted = true
    }

    /** A TX notification. [Result.DETECTED] for our face's hello, null for anything else. */
    fun onNotification(data: ByteArray): Result? {
        val hello = ForgeProtocol.parseHello(data) ?: return null
        detected = true
        faceVersion = hello.faceVersion
        wanted = false
        pending = false
        sent = false
        return Result.DETECTED
    }

    /** The reply timeout ran out: [Result.NOT_DETECTED] if the written query is still unanswered. */
    fun onReplyTimeout(): Result? {
        if (!sent) return null
        pending = false
        sent = false
        detected = false
        faceVersion = null
        return Result.NOT_DETECTED
    }
}
