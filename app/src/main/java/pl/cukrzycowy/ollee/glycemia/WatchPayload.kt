package pl.cukrzycowy.ollee.glycemia

/**
 * What the app wants a watch to show right now.
 *
 * @param text the 6-char screensaver/tag text (also the Forge watchface's Main field)
 * @param measuredAtMs when the glucose was measured, or null when unknown (cleared, label, no data)
 * @param paused true when [text] is a pause/stop label rather than a reading
 */
data class WatchPayload(
    val text: String,
    val measuredAtMs: Long? = null,
    val paused: Boolean = false
)
