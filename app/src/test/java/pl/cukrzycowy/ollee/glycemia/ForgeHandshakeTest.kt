package pl.cukrzycowy.ollee.glycemia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeHandshakeTest {

    private val hello = "H1,gly,2".toByteArray(Charsets.US_ASCII)

    /** Notifications on, query queued and written. */
    private fun asked(): ForgeHandshake = ForgeHandshake().apply {
        onNotificationsEnabled()
        assertTrue(queryDue())
        onQueryQueued()
        assertTrue(onQuerySent())
    }

    @Test
    fun nothingDue_beforeNotificationsAreOn() {
        val hs = ForgeHandshake()
        hs.requestQuery(force = true)
        assertFalse(hs.queryDue())
    }

    @Test
    fun queryThenHello_detects() {
        val hs = asked()
        assertFalse(hs.queryDue())
        assertEquals(ForgeHandshake.Result.DETECTED, hs.onNotification(hello))
        assertTrue(hs.detected)
        assertEquals(2, hs.faceVersion)
        assertNull("a late timeout changes nothing", hs.onReplyTimeout())
        assertTrue(hs.detected)
    }

    @Test
    fun noReply_notDetected_once() {
        val hs = asked()
        assertEquals(ForgeHandshake.Result.NOT_DETECTED, hs.onReplyTimeout())
        assertNull(hs.onReplyTimeout())
        assertFalse(hs.detected)
    }

    @Test
    fun replyBeforeWriteCallback_doesNotStartTheTimeout() {
        val hs = ForgeHandshake()
        hs.onNotificationsEnabled()
        hs.onQueryQueued()
        assertEquals(ForgeHandshake.Result.DETECTED, hs.onNotification(hello))
        assertFalse(hs.onQuerySent())
        assertNull(hs.onReplyTimeout())
        assertTrue(hs.detected)
    }

    @Test
    fun timeoutBeforeWriteCompleted_isIgnored() {
        val hs = ForgeHandshake()
        hs.onNotificationsEnabled()
        hs.onQueryQueued()
        assertNull(hs.onReplyTimeout())
    }

    @Test
    fun outstandingQuery_isNotDueTwice() {
        val hs = asked()
        hs.requestQuery()
        hs.requestQuery(force = true)
        assertFalse(hs.queryDue())
    }

    @Test
    fun failedWrite_makesQueryDueAgain() {
        val hs = ForgeHandshake()
        hs.onNotificationsEnabled()
        hs.onQueryQueued()
        assertFalse(hs.queryDue())
        hs.onQueryFailed()
        assertTrue(hs.queryDue())
        assertNull(hs.onReplyTimeout())
    }

    @Test
    fun keepaliveRequest_ignoredOnceDetected_forcedOneIsNot() {
        val hs = asked()
        hs.onNotification(hello)
        hs.requestQuery()
        assertFalse(hs.queryDue())
        hs.requestQuery(force = true)
        assertTrue(hs.queryDue())
    }

    @Test
    fun forcedQueryWithoutReply_clearsDetected() {
        val hs = asked()
        hs.onNotification(hello)
        hs.requestQuery(force = true)
        hs.onQueryQueued()
        hs.onQuerySent()
        assertEquals(ForgeHandshake.Result.NOT_DETECTED, hs.onReplyTimeout())
        assertFalse(hs.detected)
        assertNull(hs.faceVersion)
    }

    @Test
    fun keepaliveRequest_afterNotDetected_asksAgain() {
        val hs = asked()
        hs.onReplyTimeout()
        hs.requestQuery()
        assertTrue(hs.queryDue())
    }

    @Test
    fun foreignAndMalformedNotifications_areIgnored() {
        val hs = asked()
        for (junk in listOf("H1,abc,2", "H1,gly,x", "OK", "")) {
            assertNull(junk, hs.onNotification(junk.toByteArray(Charsets.US_ASCII)))
        }
        assertFalse(hs.detected)
        assertEquals("the query still times out", ForgeHandshake.Result.NOT_DETECTED, hs.onReplyTimeout())
    }

    @Test
    fun unsolicitedHello_counts() {
        val hs = ForgeHandshake()
        hs.onNotificationsEnabled()
        assertEquals(ForgeHandshake.Result.DETECTED, hs.onNotification(hello))
        assertFalse("detected, nothing left to ask", hs.queryDue())
    }

    @Test
    fun reset_clearsEverything() {
        val hs = asked()
        hs.onNotification(hello)
        hs.reset()
        assertFalse(hs.ready)
        assertFalse(hs.detected)
        assertNull(hs.faceVersion)
        assertFalse(hs.queryDue())
        assertNull(hs.onReplyTimeout())
    }
}
