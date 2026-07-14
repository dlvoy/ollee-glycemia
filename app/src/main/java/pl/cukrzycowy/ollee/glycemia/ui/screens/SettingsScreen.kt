package pl.cukrzycowy.ollee.glycemia.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pl.cukrzycowy.ollee.glycemia.WatchActivityLabelStore
import pl.cukrzycowy.ollee.glycemia.NightAutoPauseStore
import pl.cukrzycowy.ollee.glycemia.WatchStore
import pl.cukrzycowy.ollee.glycemia.WatchActivityState
import pl.cukrzycowy.ollee.glycemia.BleService
import pl.cukrzycowy.ollee.glycemia.DebugStore
import pl.cukrzycowy.ollee.glycemia.PreferencesBackupManager
import pl.cukrzycowy.ollee.glycemia.StoragePermissionHelper
import pl.cukrzycowy.ollee.glycemia.StoragePermissionStore
import androidx.compose.material3.AlertDialog
import java.io.File
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import pl.cukrzycowy.ollee.glycemia.BuildConfig
import pl.cukrzycowy.ollee.glycemia.NightAutoPauseScheduler
import pl.cukrzycowy.ollee.glycemia.R
import pl.cukrzycowy.ollee.glycemia.ui.components.FoldableSection
import pl.cukrzycowy.ollee.glycemia.ui.components.FullScreenScaffold
import pl.cukrzycowy.ollee.glycemia.ui.components.SectionLabel
import pl.cukrzycowy.ollee.glycemia.ui.components.TimeRangeSelector
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeColors
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeSpacing

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }
    var wasAutoPauseEnabled by remember { mutableStateOf(NightAutoPauseStore.isEnabled(context)) }

    var versionClickCount by remember { mutableIntStateOf(0) }
    var devOptionsUnlocked by remember { mutableStateOf(DebugStore.isDeveloperOptionsUnlocked(context)) }
    var debugModeEnabled by remember { mutableStateOf(DebugStore.isDebugModeEnabled(context)) }
    var lastToast by remember { mutableStateOf<Toast?>(null) }

    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var latestBackupFile by remember { mutableStateOf<File?>(null) }
    var backupExpanded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            if (NightAutoPauseStore.isEnabled(context)) {
                NightAutoPauseScheduler.checkAndApply(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            refreshTrigger++
        }
    }

    val perms = listOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.MANAGE_EXTERNAL_STORAGE
    )

    val allPermissionsGranted = perms.all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    refreshTrigger
    val isIgnoringBatteryOptimization = powerManager.isIgnoringBatteryOptimizations(context.packageName)

    var permissionsExpanded by remember { mutableStateOf(!allPermissionsGranted) }
    var batteryExpanded by remember { mutableStateOf(!isIgnoringBatteryOptimization) }
    var watchLabelsExpanded by remember { mutableStateOf(true) }
    var nightAutoPauseExpanded by remember { mutableStateOf(true) }
    var aboutExpanded by remember { mutableStateOf(true) }
    var devOptionsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            permissionsExpanded = false
        }
    }

    LaunchedEffect(isIgnoringBatteryOptimization) {
        if (isIgnoringBatteryOptimization) {
            batteryExpanded = false
        }
    }

    FullScreenScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)
        ) {
            FoldableSection(
                title = stringResource(R.string.settings_permissions),
                expanded = permissionsExpanded,
                onToggle = { permissionsExpanded = it }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)) {
                    perms.forEach { perm ->
                        val label = getPermissionLabel(perm)
                        refreshTrigger
                        val hasPermission = if (perm == Manifest.permission.MANAGE_EXTERNAL_STORAGE) {
                            StoragePermissionHelper.hasAllFilesAccess()
                        } else {
                            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
                        }
                        val buttonColor = if (hasPermission) Color(0xFF00AA00) else Color(0xFFCC0000)
                        Button(
                            onClick = {
                                if (hasPermission) {
                                    openAppSettings(context)
                                } else {
                                    requestPermission(context, perm)
                                }
                                refreshTrigger++
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor,
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = (if (hasPermission) "✓ " else "✗ ") + label)
                        }
                    }
                }
            }

            FoldableSection(
                title = stringResource(R.string.battery_optimization_title),
                expanded = batteryExpanded,
                onToggle = { batteryExpanded = it }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)) {
                    if (isIgnoringBatteryOptimization) {
                        Button(
                            onClick = {
                                openAppSettings(context)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00AA00),
                                contentColor = Color.White
                            )
                        ) {
                            Text(stringResource(R.string.battery_optimization_disabled))
                        }
                    } else {
                        Text(stringResource(R.string.battery_optimization_description))
                        Button(
                            onClick = {
                                requestIgnoreBatteryOptimization(context)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFCC8800),
                                contentColor = Color.White
                            )
                        ) {
                            Text(stringResource(R.string.battery_optimization_disable))
                        }
                    }
                }
            }

            FoldableSection(
                title = stringResource(R.string.settings_watch_labels),
                expanded = watchLabelsExpanded,
                onToggle = { watchLabelsExpanded = it }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)) {
                    var pauseLabel by remember { mutableStateOf(WatchActivityLabelStore.getPauseLabel(context)) }
                    var stopLabel by remember { mutableStateOf(WatchActivityLabelStore.getStopLabel(context)) }

                    OutlinedTextField(
                        value = pauseLabel,
                        onValueChange = {
                            if (it.length <= 6) {
                                pauseLabel = it
                                WatchActivityLabelStore.setPauseLabel(context, it)
                            }
                        },
                        label = { Text(stringResource(R.string.settings_pause_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = stopLabel,
                        onValueChange = {
                            if (it.length <= 6) {
                                stopLabel = it
                                WatchActivityLabelStore.setStopLabel(context, it)
                            }
                        },
                        label = { Text(stringResource(R.string.settings_stop_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            FoldableSection(
                title = stringResource(R.string.settings_night_auto_pause),
                expanded = nightAutoPauseExpanded,
                onToggle = { nightAutoPauseExpanded = it }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(OlleeSpacing.md)) {
                    var isAutoPauseEnabled by remember { mutableStateOf(NightAutoPauseStore.isEnabled(context)) }
                    var startTime by remember { mutableStateOf(NightAutoPauseStore.getStartTime(context)) }
                    var endTime by remember { mutableStateOf(NightAutoPauseStore.getEndTime(context)) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = OlleeSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.settings_night_auto_pause_enable))
                        Switch(
                            checked = isAutoPauseEnabled,
                            onCheckedChange = { newValue ->
                                val wasEnabled = isAutoPauseEnabled
                                isAutoPauseEnabled = newValue
                                NightAutoPauseStore.setEnabled(context, newValue)
                                if (newValue) {
                                    NightAutoPauseScheduler.scheduleNextCheck(context)
                                    NightAutoPauseScheduler.checkAndApply(context)
                                } else {
                                    NightAutoPauseScheduler.cancel(context)
                                    if (wasEnabled) {
                                        resumeAllPausedWatches(context)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                uncheckedThumbColor = Color.White
                            )
                        )
                    }

                    if (isAutoPauseEnabled) {
                        TimeRangeSelector(
                            startTime = startTime,
                            endTime = endTime,
                            onStartTimeChange = { newTime ->
                                startTime = newTime
                                NightAutoPauseStore.setStartTime(context, newTime)
                                NightAutoPauseScheduler.checkAndApply(context)
                            },
                            onEndTimeChange = { newTime ->
                                endTime = newTime
                                NightAutoPauseStore.setEndTime(context, newTime)
                                NightAutoPauseScheduler.checkAndApply(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (devOptionsUnlocked && BuildConfig.DEBUG) {
                FoldableSection(
                    title = stringResource(R.string.dev_options_title),
                    expanded = devOptionsExpanded,
                    onToggle = { devOptionsExpanded = it }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)) {
                        Button(
                            onClick = {
                                debugModeEnabled = !debugModeEnabled
                                DebugStore.setDebugModeEnabled(context, debugModeEnabled)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (debugModeEnabled) Color(0xFF00AA00) else Color(0xFFCC0000),
                                contentColor = Color.White
                            )
                        ) {
                            Text(if (debugModeEnabled) stringResource(R.string.dev_mode_enable) else stringResource(R.string.dev_mode_disable))
                        }
                    }
                }
            }

            FoldableSection(
                title = stringResource(R.string.backup_title),
                expanded = backupExpanded,
                onToggle = { backupExpanded = it }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)) {
                    refreshTrigger
                    val hasAllFilesAccess = StoragePermissionHelper.hasAllFilesAccess()

                    Button(
                        enabled = hasAllFilesAccess,
                        onClick = {
                            try {
                                val file = PreferencesBackupManager.exportPreferences(context)
                                val msg = context.getString(R.string.backup_export_success, file.name)
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                Log.d("BackupExport", "Success: ${file.absolutePath}")
                            } catch (e: SecurityException) {
                                Log.e("BackupExport", "Permission denied: ${e.message}", e)
                                if (context is android.app.Activity) {
                                    ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 200)
                                }
                                Toast.makeText(context, "Storage permission required - please retry after granting", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Log.e("BackupExport", "Export failed: ${e.message}", e)
                                val msg = context.getString(R.string.backup_export_failed, e.message ?: "Unknown error")
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0099CC),
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.backup_export))
                    }

                    Button(
                        enabled = hasAllFilesAccess,
                        onClick = {
                            val latestBackup = PreferencesBackupManager.findLatestBackup(context)
                            if (latestBackup != null) {
                                latestBackupFile = latestBackup
                                showImportConfirmDialog = true
                            } else {
                                val msg = context.getString(R.string.backup_not_found)
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0099CC),
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.backup_import))
                    }
                }
            }

            FoldableSection(
                title = stringResource(R.string.settings_about),
                expanded = aboutExpanded,
                onToggle = { aboutExpanded = it }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)) {
                    Text(
                        text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        modifier = Modifier.clickable {
                            val alreadyDevStr = context.getString(R.string.dev_already_unlocked)
                            val unlockedStr = context.getString(R.string.dev_unlocked)
                            val unlockToastStr = context.getString(R.string.dev_unlock_toast)

                            if (devOptionsUnlocked) {
                                Toast.makeText(context, alreadyDevStr, Toast.LENGTH_SHORT).show()
                            } else {
                                versionClickCount++
                                val remaining = 7 - versionClickCount
                                when {
                                    remaining == 3 || (remaining in 0..2 && versionClickCount < 7) -> {
                                        lastToast?.cancel()
                                        lastToast = Toast.makeText(context, unlockToastStr.replace("%d", remaining.toString()), Toast.LENGTH_SHORT).apply { show() }
                                    }
                                    versionClickCount == 7 -> {
                                        lastToast?.cancel()
                                        DebugStore.setDeveloperOptionsUnlocked(context, true)
                                        devOptionsUnlocked = true
                                        Toast.makeText(context, unlockedStr, Toast.LENGTH_SHORT).show()
                                    }
                                    versionClickCount > 7 -> {
                                        versionClickCount = 0
                                    }
                                }
                            }
                        }
                    )
                    Text(stringResource(R.string.settings_build_commit, BuildConfig.GIT_COMMIT_HASH))
                    Text(stringResource(R.string.settings_build_date, BuildConfig.BUILD_TIME))

                    if (debugModeEnabled) {
                        Text("Debug Mode: ON 🐛", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    }
                    if (devOptionsUnlocked) {
                        Text("Developer Options: Unlocked 🔓", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (showImportConfirmDialog && latestBackupFile != null) {
                AlertDialog(
                    onDismissRequest = { showImportConfirmDialog = false },
                    title = { Text(stringResource(R.string.backup_import_title)) },
                    text = { Text(stringResource(R.string.backup_import_message, latestBackupFile!!.name)) },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showImportConfirmDialog = false
                                val successMsg = context.getString(R.string.backup_import_success)
                                try {
                                    PreferencesBackupManager.importPreferences(context, latestBackupFile!!)
                                    Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    val failMsg = context.getString(R.string.backup_import_failed, e.message ?: "Unknown error")
                                    Toast.makeText(context, failMsg, Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Text(stringResource(R.string.backup_import_button))
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { showImportConfirmDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun getPermissionLabel(permission: String): String {
    return when (permission) {
        Manifest.permission.BLUETOOTH_CONNECT -> stringResource(R.string.perm_bluetooth_connect)
        Manifest.permission.BLUETOOTH_SCAN -> stringResource(R.string.perm_bluetooth_scan)
        Manifest.permission.POST_NOTIFICATIONS -> stringResource(R.string.perm_post_notifications)
        Manifest.permission.MANAGE_EXTERNAL_STORAGE -> stringResource(R.string.perm_manage_external_storage)
        else -> permission.split(".").last()
    }
}

private fun requestPermission(context: Context, permission: String) {
    if (permission == Manifest.permission.MANAGE_EXTERNAL_STORAGE) {
        // Clear the declined flag when user manually clicks to grant
        StoragePermissionStore.setUserDeclinedPrompt(context, false)
        try {
            context.startActivity(StoragePermissionHelper.createAllFilesAccessSettingsIntent(context))
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Failed to open storage settings: ${e.message}", e)
        }
    } else if (context is android.app.Activity) {
        ActivityCompat.requestPermissions(context, arrayOf(permission), 100)
    }
}

private fun requestIgnoreBatteryOptimization(context: Context) {
    val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = android.net.Uri.parse("package:${context.packageName}")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("SettingsScreen", "Failed to open battery optimization settings: ${e.message}")
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = android.net.Uri.parse("package:${context.packageName}")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("SettingsScreen", "Failed to open app settings: ${e.message}")
    }
}

private fun resumeAllPausedWatches(context: Context) {
    val watches = WatchStore.getAll(context)
    val pausedWatches = watches.filter { it.activityState == WatchActivityState.PAUSED }

    pausedWatches.forEach { watch ->
        val intent = Intent(context, BleService::class.java).apply {
            action = BleService.ACTION_SET_WATCH_ACTIVITY
            putExtra(BleService.EXTRA_WATCH_ADDRESS, watch.address)
            putExtra(BleService.EXTRA_ACTIVITY_STATE, WatchActivityState.ACTIVE.name)
        }
        context.startService(intent)
    }
}
