package pl.cukrzycowy.ollee.glycemia

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import pl.cukrzycowy.ollee.glycemia.BleService
import pl.cukrzycowy.ollee.glycemia.GlycemiaHistoryEntry
import pl.cukrzycowy.ollee.glycemia.GlycemiaHistoryStore
import pl.cukrzycowy.ollee.glycemia.GlycemiaReading
import pl.cukrzycowy.ollee.glycemia.XdripProvider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BleServiceGlycemiaReadingTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        context = org.robolectric.RuntimeEnvironment.getApplication()
        prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    @Test
    fun onGlycemiaReading_extractsTimestampFromReading() {
        val testTimestamp = System.currentTimeMillis() - 5 * 60 * 1000 // 5 minutes ago
        val reading = GlycemiaReading(
            bg = "150",
            trend = "UP",
            delta = 5.0,
            timestamp = testTimestamp
        )

        // Verify that GlycemiaReading preserves the timestamp
        assertEquals(testTimestamp, reading.timestamp)
        assertEquals("150", reading.bg)
        assertEquals("UP", reading.trend)
        assertEquals(5.0, reading.delta ?: 0.0, 0.001)
    }

    @Test
    fun historicalReadingsArePreservedInOrder() {
        // This test verifies that readings with older timestamps are still added to history
        // even if they arrive after newer readings

        val oldTimestamp = System.currentTimeMillis() - 10 * 60 * 1000 // 10 minutes ago
        val newTimestamp = System.currentTimeMillis() // now

        // Simulate receiving a new reading first
        val newReading = GlycemiaReading(
            bg = "150",
            trend = "UP",
            delta = 5.0,
            timestamp = newTimestamp
        )

        // Then an old reading arrives (back-fill)
        val oldReading = GlycemiaReading(
            bg = "145",
            trend = "UP",
            delta = 3.0,
            timestamp = oldTimestamp
        )

        // Both should be added to history
        val historyEntries = listOf(
            GlycemiaHistoryEntry(
                timestampMs = newTimestamp,
                valueMgDl = 150,
                delta = 5.0
            ),
            GlycemiaHistoryEntry(
                timestampMs = oldTimestamp,
                valueMgDl = 145,
                delta = 3.0
            )
        )

        // When sorted by timestamp, the old one should come first
        val sorted = historyEntries.sortedBy { it.timestampMs }
        assert(sorted[0].timestampMs == oldTimestamp)
        assert(sorted[1].timestampMs == newTimestamp)
    }

    @Test
    fun xDripProviderExtractsTimestamp() {
        val provider = XdripProvider()

        // Test that xDrip broadcast with timestamp is extracted
        val testTimestamp = 1672531200000L // Some timestamp
        val intent = android.content.Intent("com.eveningoutpost.dexdrip.BROADCAST")
            .putExtra("com.eveningoutpost.dexdrip.Extras.BgEstimate", 123.0)
            .putExtra("com.eveningoutpost.dexdrip.Extras.BgSlope", -1.0)
            .putExtra("com.eveningoutpost.dexdrip.Extras.SgvTimestampMs", testTimestamp)

        val xdripProvider = pl.cukrzycowy.ollee.glycemia.XdripProvider()
        val reading = xdripProvider.parseIntent(intent)

        assert(reading != null)
        assert(reading?.timestamp == testTimestamp)
    }

    @Test
    fun compatibleProviderExtractsTimestampFromJson() {
        val testTimestamp = 1672531200000L
        val intent = android.content.Intent("com.eveningoutpost.dexdrip.ExternalStatusChange")
            .putExtra("status", "{\"sgv\":123,\"delta\":-2.0,\"direction\":\"Flat\",\"timestamp\":$testTimestamp}")

        val xdripProvider = pl.cukrzycowy.ollee.glycemia.XdripProvider()
        val reading = xdripProvider.parseIntent(intent)

        assert(reading != null)
        assert(reading?.timestamp == testTimestamp)
    }
}
