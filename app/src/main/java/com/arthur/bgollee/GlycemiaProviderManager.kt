package com.arthur.bgollee

import android.content.Context

object GlycemiaProviderManager {
    const val PREF_KEY = "selected_provider"
    const val DEFAULT_ID = "xdrip"

    val allProviders: List<GlycemiaProvider> = listOf(
        XdripProvider(),
        ConstantProvider()
    )

    fun getSelected(context: Context): GlycemiaProvider {
        val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)
        val selectedId = prefs.getString(PREF_KEY, DEFAULT_ID) ?: DEFAULT_ID
        return allProviders.find { it.id == selectedId } ?: allProviders.first { it.id == DEFAULT_ID }
    }

    fun setSelected(context: Context, id: String) {
        context.getSharedPreferences("data", Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY, id)
            .apply()
    }
}
