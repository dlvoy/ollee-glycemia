package com.arthur.bgollee

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
        val intent = Intent("org.nightscout.android.broadcast")
            .putExtra("glucose_value", 145.0)
            .putExtra("delta", 4.5)
            .putExtra("trend_arrow", 5)

        val reading = provider.parseIntent(intent)

        requireNotNull(reading)
        assertEquals("145", reading.bg)
        assertEquals("UP", reading.trend)
        assertEquals(4.5, reading.delta ?: Double.NaN, 0.0)
    }

    @Test
    fun parseIntent_mapsCompatibleJsonBroadcast() {
        val intent = Intent("com.eveningoutpost.dexdrip.ExternalStatusChange")
            .putExtra("status", "{\"sgv\":123,\"delta\":-2.0,\"direction\":\"Flat\"}")

        val reading = provider.parseIntent(intent)

        requireNotNull(reading)
        assertEquals("123", reading.bg)
        assertEquals("FLAT", reading.trend)
        assertEquals(-2.0, reading.delta ?: Double.NaN, 0.0)
    }

    @Test
    fun parseIntent_mapsXdripBroadcastAndCalculatesDeltaFromSlope() {
        val intent = Intent("com.eveningoutpost.dexdrip.BROADCAST")
            .putExtra("com.eveningoutpost.dexdrip.Extras.BgEstimate", 111.0)
            .putExtra("com.eveningoutpost.dexdrip.Extras.BgSlope", -2.0)

        val reading = provider.parseIntent(intent)

        requireNotNull(reading)
        assertEquals("111", reading.bg)
        assertEquals("DOWN", reading.trend)
        assertEquals(-600000.0, reading.delta ?: Double.NaN, 0.0)
    }

    @Test
    fun parseIntent_returnsNullForUnknownAction() {
        val reading = provider.parseIntent(Intent("unknown.action"))

        assertNull(reading)
    }
}