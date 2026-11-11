package pl.cukrzycowy.ollee.glycemia

import java.util.UUID

/**
 * Frame protocol v1 for the Forge glycemia watchface (see docs/watchface/PROTOCOL.md).
 * One frame per BLE write, printable ASCII, at most [MAX_FRAME_BYTES] bytes.
 *
 *  - `G<age>,<text>`: live state; age in whole seconds (or `-` when unknown), text is the 6-char tag
 *  - `P,<text>`: the watch is paused/stopped by the user; text is the label
 *  - `Q`: handshake query; the glycemia face answers with a TX notification `H1,gly,<face version>`
 */
object ForgeProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("c6fb0001-a37d-40ac-89be-887d25e1f5b3")
    // RX/TX UUIDs follow the service pattern; the forge_emulator docs only print "C6FB0002-..."
    val RX_UUID: UUID = UUID.fromString("c6fb0002-a37d-40ac-89be-887d25e1f5b3")
    val TX_UUID: UUID = UUID.fromString("c6fb0003-a37d-40ac-89be-887d25e1f5b3")
    /** Client Characteristic Configuration descriptor: turns TX notifications on. */
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val MAX_FRAME_BYTES = 20
    const val MAX_AGE_SEC = 86_400L

    const val QUERY_FRAME = "Q"
    /** Identifies watchface/glycemia.js in its hello, so replies from other Forge faces are ignored. */
    const val FACE_ID = "gly"

    data class Hello(val protocol: Int, val faceVersion: Int)

    /** Parses the face's handshake reply `H<protocol>,<face id>,<face version>[,...]`; null for anything else. */
    fun parseHello(data: ByteArray): Hello? {
        val text = String(data, Charsets.ISO_8859_1)
        if (!text.startsWith("H")) return null
        val parts = text.substring(1).split(',')
        if (parts.size < 3 || parts[1] != FACE_ID) return null
        val protocol = parts[0].takeIf { it.length in 1..3 && it.all(Char::isDigit) }?.toInt() ?: return null
        val version = parts[2].takeIf { it.length in 1..5 && it.all(Char::isDigit) }?.toInt() ?: return null
        return Hello(protocol, version)
    }

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
