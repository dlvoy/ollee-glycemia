package pl.cukrzycowy.ollee.glycemia

import android.os.Handler
import android.os.Looper

/**
 * Serializes GATT writes: Android rejects a write while another is in flight, and each update now
 * makes two (OAP tag text, then the Forge frame). Descriptor writes (turning on Forge TX
 * notifications) go through it too; the caller's `issue` decides what a key writes, and both
 * write callbacks end in [onWriteComplete].
 *
 * The next write starts when the previous one completes ([onWriteComplete]) or after [timeoutMs].
 * A queued write with the same key as a new one is replaced, so the queue never holds stale
 * values. Must be used from the thread of [looper] (the main thread).
 */
class GattWriteQueue(
    looper: Looper = Looper.getMainLooper(),
    private val timeoutMs: Long = 2000L,
    private val issue: (QueuedWrite, ByteArray) -> Boolean,
    private val onDrained: () -> Unit = {}
) {
    enum class Result { SUCCESS, FAILED, TIMEOUT }

    /** [bytes] is evaluated when the write is dequeued; null skips it. */
    class QueuedWrite(
        val key: String,
        val bytes: () -> ByteArray?,
        val onResult: (Result) -> Unit = {}
    )

    private val handler = Handler(looper)
    private val pending = ArrayDeque<QueuedWrite>()
    private var inFlight: QueuedWrite? = null
    private val timeout = Runnable { finish(Result.TIMEOUT) }

    val isIdle: Boolean get() = inFlight == null && pending.isEmpty()

    fun enqueue(write: QueuedWrite) {
        pending.removeAll { it.key == write.key }
        pending.addLast(write)
        if (inFlight == null) startNext()
    }

    /** Call from the GATT callback (on the queue's thread) when a write finished. */
    fun onWriteComplete(success: Boolean) {
        if (inFlight == null) return
        finish(if (success) Result.SUCCESS else Result.FAILED)
    }

    /** Drops everything, e.g. on disconnect. Results of dropped writes are not reported. */
    fun clear() {
        handler.removeCallbacks(timeout)
        pending.clear()
        inFlight = null
    }

    private fun finish(result: Result) {
        handler.removeCallbacks(timeout)
        val done = inFlight ?: return
        inFlight = null
        done.onResult(result)
        startNext(afterWrite = true)
    }

    private fun startNext(afterWrite: Boolean = false) {
        var worked = afterWrite
        while (inFlight == null) {
            val next = pending.removeFirstOrNull() ?: break
            worked = true
            val data = next.bytes() ?: continue
            if (!issue(next, data)) {
                next.onResult(Result.FAILED)
                continue
            }
            inFlight = next
            handler.postDelayed(timeout, timeoutMs)
        }
        if (worked && isIdle) onDrained()
    }
}
