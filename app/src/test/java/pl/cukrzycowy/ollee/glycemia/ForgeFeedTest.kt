package pl.cukrzycowy.ollee.glycemia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeFeedTest {

    private val t0 = 1_000_000_000L
    private val reading = WatchPayload("120 -5", measuredAtMs = t0 - 83_000)

    @Test
    fun nothingDueWithoutPayload() {
        val feed = ForgeFeed()
        assertFalse(feed.due(t0))
        assertNull(feed.nextFrame(t0))
    }

    @Test
    fun firstPayloadIsDueAndBuildsFrameWithAge() {
        val feed = ForgeFeed()
        feed.submit(reading)
        assertTrue(feed.due(t0))
        assertEquals("G83,120 -5", feed.nextFrame(t0))
        assertFalse(feed.due(t0))
    }

    @Test
    fun ageIsComputedWhenTheFrameIsBuilt() {
        val feed = ForgeFeed()
        feed.submit(reading)
        assertEquals("G93,120 -5", feed.nextFrame(t0 + 10_000))
    }

    @Test
    fun samePayloadIsNotResentBeforeKeepalive() {
        val feed = ForgeFeed()
        feed.submit(reading)
        feed.nextFrame(t0)
        feed.submit(reading.copy())
        assertFalse(feed.due(t0 + 4 * 60_000))
    }

    @Test
    fun keepaliveIsDueAfterFiveMinutes() {
        val feed = ForgeFeed()
        feed.submit(reading)
        feed.nextFrame(t0)
        assertFalse(feed.due(t0 + ForgeFeed.KEEPALIVE_MS - 1))
        assertTrue(feed.due(t0 + ForgeFeed.KEEPALIVE_MS))
        assertEquals("G383,120 -5", feed.nextFrame(t0 + ForgeFeed.KEEPALIVE_MS))
    }

    @Test
    fun changedPayloadIsDueImmediately() {
        val feed = ForgeFeed()
        feed.submit(reading)
        feed.nextFrame(t0)
        feed.submit(WatchPayload("121  1", measuredAtMs = t0))
        assertTrue(feed.due(t0 + 1))
        assertEquals("G1,121  1", feed.nextFrame(t0 + 1_000))
    }

    @Test
    fun resetForcesResend() {
        val feed = ForgeFeed()
        feed.submit(reading)
        feed.nextFrame(t0)
        feed.reset()
        assertTrue(feed.due(t0 + 1))
    }

    @Test
    fun resetWithoutPayloadStaysQuiet() {
        val feed = ForgeFeed()
        feed.reset()
        assertFalse(feed.due(t0))
    }

    @Test
    fun unknownMeasurementTimeSendsDash() {
        val feed = ForgeFeed()
        feed.submit(WatchPayload("--- --"))
        assertEquals("G-,--- --", feed.nextFrame(t0))
    }

    @Test
    fun pausedPayloadBuildsPausedFrame() {
        val feed = ForgeFeed()
        feed.submit(WatchPayload(" pause", paused = true))
        assertEquals("P, pause", feed.nextFrame(t0))
    }

    @Test
    fun futureMeasurementTimeClampsToZero() {
        val feed = ForgeFeed()
        feed.submit(WatchPayload("120   ", measuredAtMs = t0 + 60_000))
        assertEquals("G0,120   ", feed.nextFrame(t0))
    }
}
