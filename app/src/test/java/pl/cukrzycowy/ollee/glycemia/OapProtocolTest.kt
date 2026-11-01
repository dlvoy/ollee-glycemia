package pl.cukrzycowy.ollee.glycemia

import org.junit.Assert.assertEquals
import org.junit.Test

class OapProtocolTest {

    private fun hex(bytes: ByteArray) = bytes.joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }

    @Test
    fun crc16_matchesCcittFalseCheckValue() {
        assertEquals(0x29B1, OapProtocol.crc16("123456789".toByteArray(Charsets.US_ASCII)))
    }

    // Golden bytes produced by the pre-refactor implementation (checked with an independent Python CRC)
    @Test
    fun screensaverPacket_matchesLegacyBytes() {
        assertEquals("00 0c aa 55 b2 f9 02 2f 31 32 30 20 20 35", hex(OapProtocol.screensaverPacket("120  5")))
        assertEquals("00 0c aa 55 c4 a5 02 2f 31 32 30 20 2d 35", hex(OapProtocol.screensaverPacket("120 -5")))
        assertEquals("00 0c aa 55 21 cc 02 2f 45 72 72 20 20 20", hex(OapProtocol.screensaverPacket("Err   ")))
        assertEquals("00 0c aa 55 05 79 02 2f 2d 2d 2d 20 2d 2d", hex(OapProtocol.screensaverPacket("--- --")))
    }
}
