package pl.cukrzycowy.ollee.glycemia

import android.content.Context

interface GlycemiaProvider {
    val id: String
    val displayName: String

    fun start(context: Context, onReading: (GlycemiaReading) -> Unit)

    fun stop(context: Context)
}