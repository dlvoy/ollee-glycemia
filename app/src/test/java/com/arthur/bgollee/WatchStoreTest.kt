package pl.cukrzycowy.ollee.glycemia

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WatchStoreTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        context.getSharedPreferences("data", 0).edit().clear().apply()
    }

    @Test
    fun add_createsNewWatch() {
        val watch = WatchStore.add(context, "00:11:22:33:44:55")
        assertEquals("00:11:22:33:44:55", watch.address)
        assertEquals("Ollee Watch #1", watch.name)
    }

    @Test
    fun add_duplicate_returnsSame() {
        val w1 = WatchStore.add(context, "AA:BB:CC:DD:EE:FF")
        val w2 = WatchStore.add(context, "AA:BB:CC:DD:EE:FF")
        assertEquals(w1.address, w2.address)
        assertEquals(1, WatchStore.getAll(context).size)
    }

    @Test
    fun add_multiple_incrementsIndex() {
        WatchStore.add(context, "11:11:11:11:11:11")
        WatchStore.add(context, "22:22:22:22:22:22")
        val w3 = WatchStore.add(context, "33:33:33:33:33:33")

        assertEquals("Ollee Watch #3", w3.name)
        assertEquals(3, WatchStore.getAll(context).size)
    }

    @Test
    fun rename_updatesName() {
        WatchStore.add(context, "AA:BB:CC:DD:EE:FF")
        WatchStore.rename(context, "AA:BB:CC:DD:EE:FF", "My Watch")

        val all = WatchStore.getAll(context)
        assertEquals(1, all.size)
        assertEquals("My Watch", all.first().name)
        assertTrue(all.first().isCustomName)
    }

    @Test
    fun remove_deletesWatch() {
        WatchStore.add(context, "AA:BB:CC:DD:EE:FF")
        WatchStore.remove(context, "AA:BB:CC:DD:EE:FF")

        assertEquals(0, WatchStore.getAll(context).size)
    }

    @Test
    fun migrateLegacy_addsDeviceAddress() {
        val prefs = context.getSharedPreferences("data", 0)
        prefs.edit().putString("device_address", "AA:BB:CC:DD:EE:FF").apply()

        val all = WatchStore.getAll(context)
        assertEquals(1, all.size)
        assertEquals("AA:BB:CC:DD:EE:FF", all.first().address)
        assertEquals("Ollee Watch #1", all.first().name)
    }

    @Test
    fun migrateLegacy_onlyOnce() {
        val prefs = context.getSharedPreferences("data", 0)
        prefs.edit().putString("device_address", "AA:BB:CC:DD:EE:FF").apply()

        WatchStore.getAll(context)
        WatchStore.remove(context, "AA:BB:CC:DD:EE:FF")

        WatchStore.getAll(context)
        assertEquals(0, WatchStore.getAll(context).size)
    }
}
