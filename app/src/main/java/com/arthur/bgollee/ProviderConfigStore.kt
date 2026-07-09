package com.arthur.bgollee

import android.content.Context

object ProviderConfigStore {
    private const val PREFS_NAME = "data"
    private const val PREFIX = "provider_config"

    fun read(context: Context, providerId: String, spec: ProviderConfigSpec): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return spec.fields.associate { field ->
            val key = prefKey(providerId, field.key)
            val value = prefs.getString(key, field.defaultValue) ?: field.defaultValue
            field.key to value
        }
    }

    fun write(context: Context, providerId: String, values: Map<String, String>) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        values.forEach { (key, value) ->
            editor.putString(prefKey(providerId, key), value)
        }
        editor.apply()
    }

    private fun prefKey(providerId: String, fieldKey: String): String {
        return "${PREFIX}_${providerId}_${fieldKey}"
    }
}