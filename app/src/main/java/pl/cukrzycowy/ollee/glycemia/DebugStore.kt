package pl.cukrzycowy.ollee.glycemia

import android.content.Context

object DebugStore {
    private const val PREFS_KEY = "debug"
    private const val KEY_DEV_OPTIONS_UNLOCKED = "dev_options_unlocked"
    private const val KEY_DEBUG_MODE_ENABLED = "debug_mode_enabled"

    fun isDeveloperOptionsUnlocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DEV_OPTIONS_UNLOCKED, false)
    }

    fun setDeveloperOptionsUnlocked(context: Context, unlocked: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DEV_OPTIONS_UNLOCKED, unlocked).apply()
    }

    fun isDebugModeEnabled(context: Context): Boolean {
        return isDeveloperOptionsUnlocked(context) && getDebugModeFlag(context)
    }

    fun setDebugModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DEBUG_MODE_ENABLED, enabled).apply()
    }

    private fun getDebugModeFlag(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DEBUG_MODE_ENABLED, false)
    }
}
