package pl.cukrzycowy.ollee.glycemia

import java.util.UUID

/**
 * Frame protocol v1 for the Forge glycemia watchface (see docs/watchface/PROTOCOL.md).
 * One frame per BLE write, printable ASCII, at most [MAX_FRAME_BYTES] bytes.
 *
 *  - `G<age>,<text>`: live state; age in whole seconds (or `-` when unknown), text is the 6-char tag
 *  - `P,<text>`: the watch is paused/stopped by the user; text is the label
 */
object ForgeProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("c6fb0001-a37d-40ac-89be-887d25e1f5b3")
    // RX/TX UUIDs follow the service pattern; the forge_emulator docs only print "C6FB0002-..."
    val RX_UUID: UUID = UUID.fromString("c6fb0002-a37d-40ac-89be-887d25e1f5b3")

    const val MAX_FRAME_BYTES = 20
    const val MAX_AGE_SEC = 86_400L

    /** Replaces anything outside printable ASCII with `?` and trims to fit one frame. */
    fun sanitize(text: String, maxLen: Int): String =
        text.take(maxLen).map { if (it.code in 0x20..0x7e) it else '?' }.joinToString("")

    fun glycemiaFrame(ageSec: Long?, text: String): String {
        val age = if (ageSec == null) "-" else ageSec.coerceIn(0L, MAX_AGE_SEC).toString()
        val prefix = "G$age,"
        return prefix + sanitize(text, MAX_FRAME_BYTES - prefix.length)
    }

    fun pausedFrame(text: String): String = "P," + sanitize(text, MAX_FRAME_BYTES - 2)

    fun toBytes(frame: String): ByteArray = frame.toByteArray(Charsets.US_ASCII)
}
