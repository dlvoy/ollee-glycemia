package pl.cukrzycowy.ollee.glycemia

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ForgeProtocolTest {

    // Shared with the watchface tests (watchface/test/fixtures/frames.json). Gradle runs unit tests
    // with the module directory as the working directory.
    private val fixtures: JSONObject by lazy {
        val file = listOf("../watchface/test/fixtures/frames.json", "watchface/test/fixtures/frames.json")
            .map(::File).first { it.exists() }
        JSONObject(file.readText())
    }

    @Test
    fun encode_matchesSharedFixtures() {
        val cases = fixtures.getJSONArray("encode")
        assertTrue(cases.length() > 0)
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val expected = c.getString("frame")
            val actual = when (c.getString("kind")) {
                "G" -> ForgeProtocol.glycemiaFrame(
                    if (c.isNull("ageSec")) null else c.getLong("ageSec"),
                    c.getString("text")
                )
                "P" -> ForgeProtocol.pausedFrame(c.getString("text"))
                else -> error("unknown kind in ${c.getString("name")}")
            }
            assertEquals(c.getString("name"), expected, actual)
        }
    }

    @Test
    fun frames_neverExceedTwentyBytes() {
        val long = "x".repeat(50)
        assertEquals(20, ForgeProtocol.glycemiaFrame(86_400, long).length)
        assertEquals(20, ForgeProtocol.pausedFrame(long).length)
        assertTrue(ForgeProtocol.toBytes(ForgeProtocol.glycemiaFrame(null, long)).size <= ForgeProtocol.MAX_FRAME_BYTES)
    }

    @Test
    fun sanitize_replacesControlAndNonAscii() {
        assertEquals("a?b?c", ForgeProtocol.sanitize("a\nbéc", 10))
    }

    @Test
    fun uuids_followForgeServicePattern() {
        assertEquals("c6fb0001-a37d-40ac-89be-887d25e1f5b3", ForgeProtocol.SERVICE_UUID.toString())
        assertEquals("c6fb0002-a37d-40ac-89be-887d25e1f5b3", ForgeProtocol.RX_UUID.toString())
        assertEquals("c6fb0003-a37d-40ac-89be-887d25e1f5b3", ForgeProtocol.TX_UUID.toString())
    }

    @Test
    fun handshake_matchesSharedFixtures() {
        val hs = fixtures.getJSONObject("handshake")
        assertEquals(hs.getString("query"), ForgeProtocol.QUERY_FRAME)
        assertTrue(ForgeProtocol.toBytes(ForgeProtocol.QUERY_FRAME).size <= ForgeProtocol.MAX_FRAME_BYTES)
        assertTrue(hs.getString("hello").length <= ForgeProtocol.MAX_FRAME_BYTES)
        assertNotNull(ForgeProtocol.parseHello(hs.getString("hello").toByteArray(Charsets.ISO_8859_1)))

        val cases = hs.getJSONArray("parse")
        assertTrue(cases.length() > 0)
        for (i in 0 until cases.length()) {
            val c = cases.getJSONObject(i)
            val name = c.getString("name")
            val hello = ForgeProtocol.parseHello(c.getString("data").toByteArray(Charsets.ISO_8859_1))
            if (c.getBoolean("valid")) {
                assertEquals(name, ForgeProtocol.Hello(c.getInt("protocol"), c.getInt("faceVersion")), hello)
            } else {
                assertNull(name, hello)
            }
        }
    }

    @Test
    fun parseHello_rejectsBinaryJunk() {
        assertNull(ForgeProtocol.parseHello(byteArrayOf(0x48, 0x00, 0x2c, 0xff.toByte())))
        assertNull(ForgeProtocol.parseHello(ByteArray(0)))
    }
}
