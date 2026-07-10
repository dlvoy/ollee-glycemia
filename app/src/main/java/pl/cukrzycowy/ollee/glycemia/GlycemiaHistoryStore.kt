package pl.cukrzycowy.ollee.glycemia

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object GlycemiaHistoryStore {
    private const val FILE_NAME = "glycemia_history.json"
    private const val ENTRIES_KEY = "entries"
    private const val MAX_AGE_MS = 24 * 60 * 60 * 1000L

    @Synchronized
    fun append(context: Context, entry: GlycemiaHistoryEntry) {
        val trimmedEntries = loadEntries(context)
            .filter { it.timestampMs >= cutoffTimestamp(entry.timestampMs) }
            .plus(entry)
            .sortedBy { it.timestampMs }

        writeEntries(context, trimmedEntries)
    }

    @Synchronized
    fun getRecentEntries(context: Context, maxAgeMs: Long): List<GlycemiaHistoryEntry> {
        val now = System.currentTimeMillis()
        val recentEntries = loadEntries(context)
            .filter { it.timestampMs >= now - maxAgeMs }
            .sortedBy { it.timestampMs }

        trimTo24Hours(context, recentEntries, now)
        return recentEntries
    }

    @Synchronized
    fun clear(context: Context) {
        historyFile(context).delete()
    }

    private fun trimTo24Hours(context: Context, entries: List<GlycemiaHistoryEntry>, referenceTimeMs: Long) {
        val trimmed = entries.filter { it.timestampMs >= cutoffTimestamp(referenceTimeMs) }
        if (trimmed.size != entries.size) {
            writeEntries(context, trimmed)
        }
    }

    private fun loadEntries(context: Context): List<GlycemiaHistoryEntry> {
        val file = historyFile(context)
        if (!file.exists()) return emptyList()

        return try {
            val root = JSONObject(file.readText())
            val array = root.optJSONArray(ENTRIES_KEY) ?: JSONArray()
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        GlycemiaHistoryEntry(
                            timestampMs = item.optLong("timestampMs"),
                            valueMgDl = item.optInt("valueMgDl"),
                            delta = item.optDouble("delta", 0.0)
                        )
                    )
                }
            }.filter { it.timestampMs > 0L }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeEntries(context: Context, entries: List<GlycemiaHistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("timestampMs", entry.timestampMs)
                    .put("valueMgDl", entry.valueMgDl)
                    .put("delta", entry.delta)
            )
        }
        val root = JSONObject().put(ENTRIES_KEY, array)
        historyFile(context).writeText(root.toString())
    }

    private fun historyFile(context: Context): File {
        return File(context.cacheDir, FILE_NAME)
    }

    private fun cutoffTimestamp(referenceTimeMs: Long): Long {
        return referenceTimeMs - MAX_AGE_MS
    }
}