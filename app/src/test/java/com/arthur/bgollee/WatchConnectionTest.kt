package pl.cukrzycowy.ollee.glycemia

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WatchConnectionTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val watch = PairedWatch("AA:BB:CC:DD:EE:FF", "Test Watch")

    @Before
    fun setUp() {
        context.getSharedPreferences("data", 0).edit().clear().apply()
    }

    @Test
    fun creation_startsOffline() {
        var state: WatchConnState? = null
        val conn = WatchConnection(context, watch) { _, s -> state = s }

        assertEquals(WatchConnState.OFFLINE, conn.state)
    }

    @Test
    fun disconnectSoft_setsOfflineState() {
        var state: WatchConnState? = null
        val conn = WatchConnection(context, watch) { _, s -> state = s }

        conn.disconnectSoft()
        assertEquals(WatchConnState.OFFLINE, conn.state)
    }

    @Test
    fun updateWatch_changesWatchData() {
        val conn = WatchConnection(context, watch) { _, _ -> }
        val updated = PairedWatch("AA:BB:CC:DD:EE:FF", "New Name", isCustomName = true)

        conn.updateWatch(updated)
        assertEquals("New Name", conn.watch.name)
        assertTrue(conn.watch.isCustomName)
    }

    private fun assertTrue(condition: Boolean) {
        if (!condition) throw AssertionError("Expected true")
    }
}
