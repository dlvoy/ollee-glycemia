package pl.cukrzycowy.ollee.glycemia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Debug builds only. Scenario control for scripts/forge (see docs/watchface/TESTING.md):
 *
 *     adb shell am broadcast -n <pkg>/.ForgeDebugReceiver -a pl.cukrzycowy.ollee.glycemia.debug.FORGE \
 *         --es cmd link --es on off
 *
 * `cmd`: setup [provider], link on|off, silence on|off, mute on|off, pause on|off, provider id [value], status, teardown.
 * The result text comes back in `am broadcast` output and in logcat (tag ForgeDebug).
 */
class ForgeDebugReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION = "pl.cukrzycowy.ollee.glycemia.debug.FORGE"
        private const val TAG = "ForgeDebug"
        private const val DEFAULT_PROVIDER = "virtual_human"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val result = try {
            run(context.applicationContext, intent.getStringExtra("cmd"), intent)
        } catch (e: Exception) {
            "error: ${e.message}"
        }
        Log.i(TAG, "cmd=${intent.getStringExtra("cmd")} -> $result")
        resultData = result
    }

    /** Separate from [onReceive] so tests can call it directly. */
    fun run(context: Context, cmd: String?, intent: Intent): String {
        val relay = ForgeDebugRelay.instance
        val on = intent.getStringExtra("on")?.lowercase()?.let { it == "on" || it == "true" || it == "1" }
        return when (cmd) {
            "setup" -> {
                DisclaimerStore.setUserAcceptedDisclaimer(context, true)
                StoragePermissionStore.setUserDeclinedPrompt(context, true)
                DebugStore.setForgeRelayEnabled(context, true)
                val port = relay.start()
                relay.setLink(true)
                switchProvider(context, intent.getStringExtra("provider") ?: DEFAULT_PROVIDER, null)
                "ok relay on port $port"
            }
            "link" -> on?.let { relay.setLink(it); "ok link=$it" } ?: "error: need --es on on|off"
            "silence" -> on?.let { relay.setSilenced(it); "ok silenced=$it" } ?: "error: need --es on on|off"
            "mute" -> on?.let { relay.setMuted(it); "ok muted=$it" } ?: "error: need --es on on|off"
            "pause" -> on?.let { relay.setPaused(it); "ok paused=$it" } ?: "error: need --es on on|off"
            "provider" -> {
                val id = intent.getStringExtra("id") ?: return "error: need --es id <provider id>"
                if (GlycemiaProviderManager.allProviders.none { it.id == id }) return "error: unknown provider $id"
                switchProvider(context, id, intent.getStringExtra("value")?.toIntOrNull())
                "ok provider=$id"
            }
            "status" -> relay.status()
            "teardown" -> {
                DebugStore.setForgeRelayEnabled(context, false)
                relay.stop()
                "ok relay off"
            }
            else -> "error: unknown cmd '$cmd' (setup, link, silence, mute, pause, provider, status, teardown)"
        }
    }

    private fun switchProvider(context: Context, id: String, constantValue: Int?) {
        if (id == "constant" && constantValue != null) {
            val provider = GlycemiaProviderManager.allProviders.first { it.id == id } as ConfigurableGlycemiaProvider
            provider.saveConfig(context, provider.getSavedConfig(context) + ("value_mgdl" to constantValue.toString()))
        }
        // Same reset the provider picker in MainScreen does
        context.getSharedPreferences("data", Context.MODE_PRIVATE).edit()
            .putLong("provider_switch_time", System.currentTimeMillis())
            .putString("last_bg", "--")
            .putFloat("last_delta", Float.NaN)
            .putLong("last_time", 0L)
            .apply()
        GlycemiaProviderManager.setSelected(context, id)
        startService(context, BleService.ACTION_SWITCH_PROVIDER)
        Handler(Looper.getMainLooper()).postDelayed(
            { startService(context, BleService.ACTION_RESEND_GLYCEMIA) }, 100
        )
    }

    private fun startService(context: Context, action: String) {
        try {
            context.startForegroundService(Intent(context, BleService::class.java).setAction(action))
        } catch (e: Exception) {
            Log.w(TAG, "could not start BleService for $action: ${e.message}")
        }
    }
}
