package pl.cukrzycowy.ollee.glycemia

import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GattWriteQueueTest {

    private val issued = mutableListOf<String>()
    private val results = mutableListOf<String>()
    private var drained = 0
    private var acceptWrites = true
    private lateinit var queue: GattWriteQueue

    @Before
    fun setUp() {
        issued.clear(); results.clear(); drained = 0; acceptWrites = true
        queue = GattWriteQueue(
            timeoutMs = 2000L,
            issue = { w, bytes -> if (acceptWrites) { issued.add("${w.key}:${String(bytes)}"); true } else false },
            onDrained = { drained++ }
        )
    }

    private fun write(key: String, text: String?) = GattWriteQueue.QueuedWrite(
        key = key,
        bytes = { text?.toByteArray() },
        onResult = { results.add("$key:${text}:$it") }
    )

    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test
    fun writesRunStrictlyOneAtATime() {
        queue.enqueue(write("oap", "a"))
        queue.enqueue(write("forge", "b"))
        assertEquals(listOf("oap:a"), issued)

        queue.onWriteComplete(true)
        assertEquals(listOf("oap:a", "forge:b"), issued)
        assertEquals(listOf("oap:a:SUCCESS"), results)

        queue.onWriteComplete(true)
        assertEquals(listOf("oap:a:SUCCESS", "forge:b:SUCCESS"), results)
        assertTrue(queue.isIdle)
    }

    @Test
    fun queuedWriteWithSameKeyIsReplaced() {
        queue.enqueue(write("forge", "1")) // goes out right away
        queue.enqueue(write("forge", "2"))
        queue.enqueue(write("forge", "3"))
        queue.onWriteComplete(true)
        queue.onWriteComplete(true)
        assertEquals(listOf("forge:1", "forge:3"), issued)
    }

    @Test
    fun timeoutMovesTheQueueOn() {
        queue.enqueue(write("oap", "a"))
        queue.enqueue(write("forge", "b"))
        idle(1999)
        assertEquals(listOf("oap:a"), issued)
        idle(2)
        assertEquals(listOf("oap:a", "forge:b"), issued)
        assertEquals(listOf("oap:a:TIMEOUT"), results)
    }

    @Test
    fun failedCompletionIsReported() {
        queue.enqueue(write("oap", "a"))
        queue.onWriteComplete(false)
        assertEquals(listOf("oap:a:FAILED"), results)
    }

    @Test
    fun rejectedWriteIsReportedAndSkipped() {
        acceptWrites = false
        queue.enqueue(write("oap", "a"))
        assertEquals(listOf("oap:a:FAILED"), results)
        assertTrue(queue.isIdle)
        assertEquals(1, drained)
    }

    @Test
    fun nullBytesAreSkipped() {
        queue.enqueue(write("forge", null))
        queue.enqueue(write("oap", "a"))
        assertEquals(listOf("oap:a"), issued)
    }

    @Test
    fun clearDropsPendingAndInFlightWithoutResults() {
        queue.enqueue(write("oap", "a"))
        queue.enqueue(write("forge", "b"))
        queue.clear()
        idle(5000)
        queue.onWriteComplete(true)
        assertEquals(listOf("oap:a"), issued)
        assertTrue(results.isEmpty())
        assertTrue(queue.isIdle)
    }

    @Test
    fun drainCallbackFiresOnceAfterTheLastWrite() {
        queue.enqueue(write("oap", "a"))
        queue.enqueue(write("forge", "b"))
        queue.onWriteComplete(true)
        assertEquals(0, drained)
        queue.onWriteComplete(true)
        assertEquals(1, drained)
    }

    @Test
    fun lateCompletionAfterTimeoutIsIgnored() {
        queue.enqueue(write("oap", "a"))
        idle(2001)
        queue.onWriteComplete(true) // nothing in flight any more
        assertEquals(listOf("oap:a:TIMEOUT"), results)
    }
}
