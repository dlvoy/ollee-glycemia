package pl.cukrzycowy.ollee.glycemia

import android.content.Context
import android.os.Environment
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale

object PreferencesBackupManager {

    fun getBackupFolder(context: Context): File {
        return try {
            // Use Downloads folder for persistence - accessible without restricted permissions
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            Log.d("BackupManager", "Downloads folder: ${downloadsDir?.absolutePath}")

            if (downloadsDir != null && downloadsDir.exists()) {
                val backupDir = File(downloadsDir, "OlleeGlycemia")
                if (!backupDir.exists()) {
                    val mkdirResult = backupDir.mkdirs()
                    Log.d("BackupManager", "mkdirs result: $mkdirResult")
                }
                Log.d("BackupManager", "Using Downloads folder: ${backupDir.absolutePath}, can write: ${backupDir.canWrite()}")
                backupDir
            } else {
                Log.w("BackupManager", "Downloads folder not accessible, using app files dir")
                val appFilesDir = context.getExternalFilesDir(null) ?: context.filesDir
                val backupDir = File(appFilesDir, "OlleeGlycemia")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }
                backupDir
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Error getting backup folder: ${e.message}", e)
            val appFilesDir = context.getExternalFilesDir(null) ?: context.filesDir
            val backupDir = File(appFilesDir, "OlleeGlycemia")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            backupDir
        }
    }

    fun exportPreferences(context: Context): File {
        Log.d("BackupManager", "Starting export...")
        val backupFolder = getBackupFolder(context)
        Log.d("BackupManager", "Backup folder: ${backupFolder.absolutePath}, exists: ${backupFolder.exists()}, canWrite: ${backupFolder.canWrite()}")

        val timestamp = SimpleDateFormat("yyyyMMddHHmm", Locale.US).format(Date())
        val fileName = "preferences-backup-$timestamp.json"
        val file = File(backupFolder, fileName)
        Log.d("BackupManager", "Export file: ${file.absolutePath}")

        val backup = JSONObject()

        val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)
        backup.put("provider", GlycemiaProviderManager.getSelected(context).id)
        backup.put("lastBg", prefs.getString("last_bg", ""))

        val lastDelta = prefs.getFloat("last_delta", Float.NaN)
        if (!lastDelta.isNaN()) {
            backup.put("lastDelta", lastDelta)
        }

        backup.put("lastTime", prefs.getLong("last_time", 0L))

        val watchesArray = JSONArray()
        WatchStore.getAll(context).forEach { watch ->
            val watchObj = JSONObject()
            watchObj.put("address", watch.address)
            watchObj.put("name", watch.name)
            watchObj.put("activityState", watch.activityState.name)
            watchesArray.put(watchObj)
        }
        backup.put("watches", watchesArray)

        val labels = JSONObject()
        labels.put("pauseLabel", WatchActivityLabelStore.getPauseLabel(context))
        labels.put("stopLabel", WatchActivityLabelStore.getStopLabel(context))
        backup.put("activityLabels", labels)

        val nightPause = JSONObject()
        nightPause.put("enabled", NightAutoPauseStore.isEnabled(context))
        nightPause.put("startTime", NightAutoPauseStore.getStartTime(context))
        nightPause.put("endTime", NightAutoPauseStore.getEndTime(context))
        backup.put("nightAutoPause", nightPause)

        val graphSettings = JSONObject()
        graphSettings.put("displayRange", prefs.getInt("graph_display_range", 2))
        backup.put("graphSettings", graphSettings)

        val historyArray = JSONArray()
        GlycemiaHistoryStore.getRecentEntries(context, 24 * 60 * 60 * 1000L).forEach { entry ->
            val entryObj = JSONObject()
            entryObj.put("timestampMs", entry.timestampMs)
            entryObj.put("valueMgDl", entry.valueMgDl)
            entryObj.put("delta", entry.delta)
            historyArray.put(entryObj)
        }
        backup.put("glycemiaHistory", historyArray)

        file.writeText(backup.toString(2))
        Log.d("BackupManager", "Export complete: ${file.absolutePath}, size: ${file.length()} bytes")
        return file
    }

    fun findLatestBackup(context: Context): File? {
        // First try Downloads folder (persistent and accessible without restricted permissions)
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            Log.d("BackupManager", "Searching Downloads: ${downloadsDir?.absolutePath}")
            if (downloadsDir != null && downloadsDir.exists()) {
                val backupDir = File(downloadsDir, "OlleeGlycemia")
                Log.d("BackupManager", "Backup dir exists: ${backupDir.exists()}, can read: ${backupDir.canRead()}")
                if (backupDir.exists()) {
                    // First list ALL files to see what's there
                    val allFiles = backupDir.listFiles()
                    Log.d("BackupManager", "All files in backup dir: ${allFiles?.joinToString { it.name } ?: "null"}")

                    val files = backupDir.listFiles { file ->
                        val matches = file.name.startsWith("preferences-backup-") && file.name.endsWith(".json")
                        Log.d("BackupManager", "File: ${file.name}, matches pattern: $matches")
                        matches
                    }
                    Log.d("BackupManager", "Found ${files?.size ?: 0} backup files in Downloads")
                    if (!files.isNullOrEmpty()) {
                        return files.maxByOrNull { it.lastModified() }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Error searching Downloads: ${e.message}", e)
        }

        // Fallback to app files dir
        val backupFolder = getBackupFolder(context)
        Log.d("BackupManager", "Fallback folder: ${backupFolder.absolutePath}, exists: ${backupFolder.exists()}")
        if (!backupFolder.exists()) return null

        // List all files in fallback folder
        val allFiles = backupFolder.listFiles()
        Log.d("BackupManager", "All files in fallback dir: ${allFiles?.joinToString { it.name } ?: "null"}")

        val files = backupFolder.listFiles { file ->
            val matches = file.name.startsWith("preferences-backup-") && file.name.endsWith(".json")
            Log.d("BackupManager", "Fallback file: ${file.name}, matches pattern: $matches")
            matches
        }
        Log.d("BackupManager", "Found ${files?.size ?: 0} backup files in app folder")
        return files?.maxByOrNull { it.lastModified() }
    }

    fun importPreferences(context: Context, file: File) {
        val jsonText = file.readText()
        val backup = JSONObject(jsonText)

        val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        if (backup.has("provider")) {
            GlycemiaProviderManager.setSelected(context, backup.getString("provider"))
        }

        if (backup.has("lastDelta")) {
            editor.putFloat("last_delta", backup.getDouble("lastDelta").toFloat())
        }

        if (backup.has("lastTime")) {
            editor.putLong("last_time", backup.getLong("lastTime"))
        }

        if (backup.has("watches")) {
            val watchesArray = backup.getJSONArray("watches")
            for (i in 0 until watchesArray.length()) {
                val watchObj = watchesArray.getJSONObject(i)
                val address = watchObj.getString("address")
                val name = watchObj.getString("name")
                val activityState = WatchActivityState.valueOf(watchObj.getString("activityState"))

                WatchStore.add(context, address)
                WatchStore.rename(context, address, name)
                if (activityState != WatchActivityState.ACTIVE) {
                    WatchStore.setActivityState(context, address, activityState)
                }
            }
        }

        if (backup.has("activityLabels")) {
            val labels = backup.getJSONObject("activityLabels")
            if (labels.has("pauseLabel")) {
                WatchActivityLabelStore.setPauseLabel(context, labels.getString("pauseLabel"))
            }
            if (labels.has("stopLabel")) {
                WatchActivityLabelStore.setStopLabel(context, labels.getString("stopLabel"))
            }
        }

        if (backup.has("nightAutoPause")) {
            val nightPause = backup.getJSONObject("nightAutoPause")
            NightAutoPauseStore.setEnabled(context, nightPause.getBoolean("enabled"))
            try {
                val startTime = LocalTime.parse(nightPause.getString("startTime"))
                NightAutoPauseStore.setStartTime(context, startTime)
            } catch (e: Exception) {
                // Use default if parsing fails
            }
            try {
                val endTime = LocalTime.parse(nightPause.getString("endTime"))
                NightAutoPauseStore.setEndTime(context, endTime)
            } catch (e: Exception) {
                // Use default if parsing fails
            }
        }

        if (backup.has("graphSettings")) {
            val graphSettings = backup.getJSONObject("graphSettings")
            if (graphSettings.has("displayRange")) {
                editor.putInt("graph_display_range", graphSettings.getInt("displayRange"))
            }
        }

        editor.apply()

        if (backup.has("glycemiaHistory")) {
            GlycemiaHistoryStore.clear(context)
            val historyArray = backup.getJSONArray("glycemiaHistory")
            for (i in 0 until historyArray.length()) {
                val entryObj = historyArray.getJSONObject(i)
                val entry = GlycemiaHistoryEntry(
                    timestampMs = entryObj.getLong("timestampMs"),
                    valueMgDl = entryObj.getInt("valueMgDl"),
                    delta = entryObj.getDouble("delta")
                )
                GlycemiaHistoryStore.append(context, entry)
            }
        }
    }
}
