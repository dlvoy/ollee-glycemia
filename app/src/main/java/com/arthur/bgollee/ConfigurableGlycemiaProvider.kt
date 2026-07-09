package com.arthur.bgollee

import android.content.Context

interface ConfigurableGlycemiaProvider : GlycemiaProvider {
    fun getConfigSpec(context: Context): ProviderConfigSpec

    fun getSavedConfig(context: Context): Map<String, String>

    fun saveConfig(context: Context, values: Map<String, String>)

    fun getConfigSummary(context: Context): String
}