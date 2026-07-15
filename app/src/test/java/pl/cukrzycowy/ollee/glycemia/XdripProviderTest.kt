package pl.cukrzycowy.ollee.glycemia

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class XdripProviderTest {

    private val provider = XdripProvider()

    @Test
    fun parseIntent_mapsNightscoutBroadcast() {
        val testTimestamp = 1000000L
        val intent = Intent("org.nightscout.android.broadcast")
            .putExtra("glucose_value", 145.0)
            .putExtra("delta", 4.5)
            .putExtra("trend_arrow", 5)
            .putExtra("timestamp", testTimestamp)

        val reading = provider.parseIntent(intent)

        requireNotNull(reading)
        assertEquals("145", reading.bg)
        assertEquals("UP", reading.trend)
        assertEquals(4.5, reading.delta ?: Double.NaN, 0.0)
        assertEquals(testTimestamp, reading.timestamp)
    }

    @Test
    fun parseIntent_nightscoutBroadcast_fallsBackToCurrentTimeWhenNoTimestamp() {
        val intent = Intent("org.nightscout.android.broadcast")
            .putExtra("glucose_value", 145.0)

        val reading = provider.parseIntent(intent)

        requireNotNull(reading)
        assertEquals("145", reading.bg)
        assert(reading.timestamp >= System.currentTimeMillis() - 1000) // Allow 1s margin
    }

    @Test
    fun parseIntent_mapsCompatibleJsonBroadcast() {
        val testTimestamp = 2000000L
        val intent = Intent("com.eveningoutpost.dexdrip.ExternalStatusChange")
            .putExtra("status", "{\"sgv\":123,\"delta\":-2.0,\"direction\":\"Flat\",\"timestamp\":$testTimestamp}")

        val reading = provider.parseIntent(intent)

        requireNotNull(reading)
        assertEquals("123", reading.bg)
        assertEquals("FLAT", reading.trend)
        assertEquals(-2.0, reading.delta ?: Double.NaN, 0.0)
        assertEquals(testTimestamp, reading.timestamp)
    }

    @Test
    fun parseIntent_compatibleJsonBroadcast_extractsTimeFromJsonAlternateFields() {
        val intent1 = Intent("com.eveningoutpost.dexdrip.ExternalStatusChange")
            .putExtra("status", "{\"sgv\":123,\"time\":3000000}")
        val reading1 = provider.parseIntent(intent1)
        assertEquals(3000000L, reading1?.timestamp)

        val intent2 = Intent("com.eveningoutpost.dexdrip.ExternalStatusChange")
            .putExtra("status", "{\"sgv\":123,\"date\":4000000}")
        val reading2 = provider.parseIntent(intent2)
        assertEquals(4000000L, reading2?.timestamp)
    }

    @Test
    fun parseIntent_mapsXdripBroadcastWithExtrasTime() {
        val testTimestamp = 5000000L
        val intent = Intent("com.eveningoutpost.dexdrip.BROADCAST")
            .putExtra("com.eveningoutpost.dexdrip.Extras.BgEstimate", 111.0)
            .putExtra("com.eveningoutpost.dexdrip.Extras.BgSlope", -2.0)
            .putExtra("com.eveningoutpost.dexdrip.Extras.Time", testTimestamp)

        val reading = provider.parseIntent(intent)

        requireNotNull(reading)
        assertEquals("111", reading.bg)
        assertEquals("DOWN", reading.trend)
        assertEquals(-600000.0, reading.delta ?: Double.NaN, 0.0)
        assertEquals(testTimestamp, reading.timestamp)
    }

    @Test
    fun parseIntent_mapsXdripBroadcastWithBgTimestamp() {
        val testTimestamp = 6000000L
        val intent = Intent("com.eveningoutpost.dexdrip.BROADCAST")
            .putExtra("com.eveningoutpost.dexdrip.Extras.BgEstimate", 120.0)
            .putExtra("com.eveningoutpost.dexdrip.Extras.BgSlope", 1.5)
            .putExtra("bg.timeStamp", testTimestamp)

        val reading = provider.parseIntent(intent)

        requireNotNull(reading)
        assertEquals("120", reading.bg)
        assertEquals("UP", reading.trend)
        assertEquals(testTimestamp, reading.timestamp)
    }

    @Test
    fun parseIntent_handlesBgEstimateNoData() {
        val intent = Intent("com.eveningoutpost.dexdrip.BgEstimateNoData")

        val reading = provider.parseIntent(intent)

        requireNotNull(reading)
        assertEquals("---", reading.bg)
        assertNull(reading.trend)
        assertNull(reading.delta)
        assert(reading.timestamp >= System.currentTimeMillis() - 1000)
    }

    @Test
    fun parseIntent_returnsNullForUnknownAction() {
        val reading = provider.parseIntent(Intent("unknown.action"))

        assertNull(reading)
    }
}