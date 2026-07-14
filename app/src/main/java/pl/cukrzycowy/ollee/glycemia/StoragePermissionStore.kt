package pl.cukrzycowy.ollee.glycemia

import android.content.Context

object StoragePermissionStore {

    fun hasUserDeclinedPrompt(context: Context): Boolean {
        val prefs = context.getSharedPreferences("storage_permission", Context.MODE_PRIVATE)
        return prefs.getBoolean("user_declined_prompt", false)
    }

    fun setUserDeclinedPrompt(context: Context, declined: Boolean) {
        val prefs = context.getSharedPreferences("storage_permission", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("user_declined_prompt", declined).apply()
    }
}
