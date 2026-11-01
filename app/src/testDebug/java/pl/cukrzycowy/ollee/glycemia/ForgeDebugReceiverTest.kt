package pl.cukrzycowy.ollee.glycemia

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ForgeDebugReceiverTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val receiver = ForgeDebugReceiver()
    private val relay = ForgeDebugRelay.instance

    @Before
    fun setUp() {
        listOf("data", "disclaimer", "storage_permission", "debug").forEach {
            context.getSharedPreferences(it, 0).edit().clear().commit()
        }
        relay.stop()
    }

    @After
    fun tearDown() = relay.stop()

    private fun run(cmd: String?, vararg extras: Pair<String, String>): String {
        val intent = Intent(ForgeDebugReceiver.ACTION).putExtra("cmd", cmd)
        extras.forEach { (k, v) -> intent.putExtra(k, v) }
        return receiver.run(context, cmd, intent)
    }

    @Test
    fun setup_acceptsDisclaimerEnablesRelayAndSelectsProvider() {
        val result = run("setup")
        assertTrue(result, result.startsWith("ok"))
        assertTrue(DisclaimerStore.hasUserAcceptedDisclaimer(context))
        assertTrue(StoragePermissionStore.hasUserDeclinedPrompt(context))
        assertTrue(DebugStore.isForgeRelayEnabled(context))
        assertTrue(relay.isRunning)
        assertEquals("virtual_human", GlycemiaProviderManager.getSelected(context).id)
    }

    @Test
    fun linkSilencePause_changeTheRelay() {
        run("setup")
        assertEquals("ok link=false", run("link", "on" to "off"))
        assertFalse(relay.link)
        assertEquals("ok link=true", run("link", "on" to "on"))
        assertEquals("ok silenced=true", run("silence", "on" to "on"))
        assertTrue(relay.silenced)
        assertEquals("ok muted=true", run("mute", "on" to "on"))
        assertTrue(relay.muted)
        assertEquals("ok paused=true", run("pause", "on" to "on"))
        assertTrue(relay.paused)
    }

    @Test
    fun provider_switchesAndStoresConstantValue() {
        assertEquals("ok provider=constant", run("provider", "id" to "constant", "value" to "180"))
        assertEquals("constant", GlycemiaProviderManager.getSelected(context).id)
        val constant = GlycemiaProviderManager.allProviders.first { it.id == "constant" } as ConfigurableGlycemiaProvider
        assertEquals("180", constant.getSavedConfig(context)["value_mgdl"])
        assertEquals(0L, context.getSharedPreferences("data", 0).getLong("last_time", -1))
    }

    @Test
    fun badInputIsReportedNotThrown() {
        assertTrue(run("link").startsWith("error"))
        assertTrue(run("provider", "id" to "nope").startsWith("error"))
        assertTrue(run("wat").startsWith("error"))
        assertTrue(run(null).startsWith("error"))
    }

    @Test
    fun teardown_turnsRelayOffAndForgetsTheSetting() {
        run("setup")
        assertEquals("ok relay off", run("teardown"))
        assertFalse(relay.isRunning)
        assertFalse(DebugStore.isForgeRelayEnabled(context))
    }

    @Test
    fun status_describesRelayState() {
        run("setup")
        assertTrue(run("status").contains("running=true"))
    }
}
