package pl.cukrzycowy.ollee.glycemia

import java.util.UUID

/**
 * The watch firmware's own "OAP" framing over the Nordic UART service, as used to set the
 * screensaver/tag text. Pure functions so the bytes can be unit tested.
 */
object OapProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val CHAR_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

    /** CRC-16/CCITT-FALSE: init 0xFFFF, poly 0x1021, no reflection, no final XOR. */
    fun crc16(data: ByteArray): Int {
        var crc = 0xFFFF
        for (b in data) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return crc
    }

    /** `[00][len][AA 55][crcHi crcLo][02 2F][ASCII text]`, where len counts everything after itself. */
    fun screensaverPacket(text: String): ByteArray {
        val payload = byteArrayOf(0x02, 0x2f) + text.toByteArray(Charsets.US_ASCII)
        val crc = crc16(payload)
        return byteArrayOf(
            0x00,
            (payload.size + 4).toByte(),
            0xaa.toByte(),
            0x55,
            (crc shr 8).toByte(),
            (crc and 0xFF).toByte()
        ) + payload
    }
}
