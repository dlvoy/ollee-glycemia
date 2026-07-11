package pl.cukrzycowy.ollee.glycemia.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pl.cukrzycowy.ollee.glycemia.WatchActivityLabelStore
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import pl.cukrzycowy.ollee.glycemia.BuildConfig
import pl.cukrzycowy.ollee.glycemia.R
import pl.cukrzycowy.ollee.glycemia.ui.components.FoldableSection
import pl.cukrzycowy.ollee.glycemia.ui.components.FullScreenScaffold
import pl.cukrzycowy.ollee.glycemia.ui.components.SectionLabel
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeColors
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeSpacing

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            refreshTrigger++
        }
    }

    val perms = listOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.POST_NOTIFICATIONS
    )

    val allPermissionsGranted = perms.all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isIgnoringBatteryOptimization = powerManager.isIgnoringBatteryOptimizations(context.packageName)

    var permissionsExpanded by remember { mutableStateOf(!allPermissionsGranted) }
    var batteryExpanded by remember { mutableStateOf(!isIgnoringBatteryOptimization) }
    var watchLabelsExpanded by remember { mutableStateOf(true) }
    var aboutExpanded by remember { mutableStateOf(false) }

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
        FoldableSection(
            title = stringResource(R.string.settings_permissions),
            expanded = permissionsExpanded,
            onToggle = { permissionsExpanded = it }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)) {
                perms.forEach { perm ->
                    val label = getPermissionLabel(perm)
                    refreshTrigger
                    val hasPermission = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
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
            title = stringResource(R.string.settings_about),
            expanded = aboutExpanded,
            onToggle = { aboutExpanded = it }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)) {
                Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME))
                Text(stringResource(R.string.settings_build_commit, BuildConfig.GIT_COMMIT_HASH))
                Text(stringResource(R.string.settings_build_date, BuildConfig.BUILD_TIME))
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
        else -> permission.split(".").last()
    }
}

private fun requestPermission(context: Context, permission: String) {
    if (context is android.app.Activity) {
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
