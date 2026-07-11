package pl.cukrzycowy.ollee.glycemia

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the list of paired target watches (address + user/default name).
 * Replaces the old single `device_address` preference; on first access after
 * an upgrade, migrates that legacy value into a single [PairedWatch] entry
 * named "Ollee Watch #1" (once - guarded by [KEY_MIGRATION_DONE] so deleting
 * the migrated watch later doesn't resurrect it).
 */
object WatchStore {
    private const val PREFS_NAME = "data"
    private const val KEY_WATCHES = "paired_watches"
    private const val KEY_NEXT_INDEX = "paired_watches_next_index"
    private const val KEY_MIGRATION_DONE = "paired_watches_migration_done"
    private const val LEGACY_KEY_DEVICE_ADDRESS = "device_address"

    @Synchronized
    fun getAll(context: Context): List<PairedWatch> {
        migrateLegacyIfNeeded(context)
        return readAll(context)
    }

    @Synchronized
    fun add(context: Context, address: String): PairedWatch {
        migrateLegacyIfNeeded(context)

        val existing = readAll(context)
        existing.find { it.address == address }?.let { return it }

        val prefs = prefs(context)
        val nextIndex = prefs.getInt(KEY_NEXT_INDEX, 1)
        val watch = PairedWatch(address = address, name = "Ollee Watch #$nextIndex")
        writeAll(context, existing + watch)
        prefs.edit().putInt(KEY_NEXT_INDEX, nextIndex + 1).apply()
        return watch
    }

    @Synchronized
    fun rename(context: Context, address: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val updated = readAll(context).map {
            if (it.address == address) it.copy(name = trimmed, isCustomName = true) else it
        }
        writeAll(context, updated)
    }

    @Synchronized
    fun remove(context: Context, address: String) {
        writeAll(context, readAll(context).filterNot { it.address == address })
    }

    @Synchronized
    fun updateLastSyncTime(context: Context, address: String, timeMs: Long) {
        val updated = readAll(context).map {
            if (it.address == address) it.copy(lastSuccessfulSyncTimeMs = timeMs) else it
        }
        writeAll(context, updated)
    }

    private fun migrateLegacyIfNeeded(context: Context) {
        val prefs = prefs(context)
        if (prefs.getBoolean(KEY_MIGRATION_DONE, false)) return

        val legacyAddress = prefs.getString(LEGACY_KEY_DEVICE_ADDRESS, null)
        if (legacyAddress != null && readAll(context).none { it.address == legacyAddress }) {
            val watch = PairedWatch(address = legacyAddress, name = "Ollee Watch #1")
            writeAll(context, readAll(context) + watch)
            prefs.edit().putInt(KEY_NEXT_INDEX, maxOf(prefs.getInt(KEY_NEXT_INDEX, 1), 2)).apply()
        }

        prefs.edit().putBoolean(KEY_MIGRATION_DONE, true).apply()
    }

    private fun readAll(context: Context): List<PairedWatch> {
        val json = prefs(context).getString(KEY_WATCHES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val address = item.optString("address").takeIf { it.isNotBlank() } ?: continue
                    add(
                        PairedWatch(
                            address = address,
                            name = item.optString("name", address),
                            isCustomName = item.optBoolean("isCustomName", false),
                            lastSuccessfulSyncTimeMs = item.optLong("lastSuccessfulSyncTimeMs", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeAll(context: Context, watches: List<PairedWatch>) {
        val array = JSONArray()
        watches.forEach { watch ->
            array.put(
                JSONObject()
                    .put("address", watch.address)
                    .put("name", watch.name)
                    .put("isCustomName", watch.isCustomName)
                    .put("lastSuccessfulSyncTimeMs", watch.lastSuccessfulSyncTimeMs)
            )
        }
        prefs(context).edit().putString(KEY_WATCHES, array.toString()).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
