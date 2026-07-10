package com.arthur.bgollee.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.arthur.bgollee.AppState
import com.arthur.bgollee.GlycemiaGraphView
import com.arthur.bgollee.GlycemiaProviderManager
import com.arthur.bgollee.WatchConnState
import com.arthur.bgollee.ui.components.FoldableSection
import com.arthur.bgollee.ui.components.OlleeHeader
import com.arthur.bgollee.ui.components.PillButton
import com.arthur.bgollee.ui.components.PillButtonStyle
import com.arthur.bgollee.ui.components.RichSelectorRow
import com.arthur.bgollee.ui.components.RowActionIcon
import com.arthur.bgollee.ui.components.SectionLabel
import com.arthur.bgollee.ui.components.SimpleSelector
import com.arthur.bgollee.ui.components.StatusBanner
import com.arthur.bgollee.ui.components.StatusBannerTone
import com.arthur.bgollee.ui.nav.AppNavController
import com.arthur.bgollee.ui.nav.Route
import com.arthur.bgollee.ui.theme.OlleeColors
import com.arthur.bgollee.ui.theme.OlleeSpacing

@Composable
fun MainScreen(nav: AppNavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)

    val permsOk = arePermissionsGranted(context)
    val lastBg = prefs.getString("last_bg", "--") ?: "--"
    val lastDelta = prefs.getFloat("last_delta", Float.NaN)
    val lastTime = prefs.getLong("last_time", 0L)
    val timeAgoStr = formatTimeAgo(lastTime)

    var selectedProvider by remember { mutableStateOf(GlycemiaProviderManager.getSelected(context)) }
    val watchStatuses by AppState.watchStatuses.collectAsState()

    var graphExpanded by remember { mutableStateOf(true) }
    var showProviderPicker by remember { mutableStateOf(false) }
    var showGraphOptions by remember { mutableStateOf(false) }
    var selectedGraphRange by remember { mutableStateOf(2) }
    var refreshGraphTrigger by remember { mutableStateOf(0) }

    if (showProviderPicker) {
        ProviderPickerDialog(
            context = context,
            currentProviderId = selectedProvider.id,
            onSelected = { newProviderId ->
                GlycemiaProviderManager.setSelected(context, newProviderId)
                selectedProvider = GlycemiaProviderManager.getSelected(context)
                showProviderPicker = false
                // Restart service with new provider
                val intent = Intent(context, com.arthur.bgollee.BleService::class.java).apply {
                    action = com.arthur.bgollee.BleService.ACTION_SWITCH_PROVIDER
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        @Suppress("DEPRECATION")
                        context.startService(intent)
                    }
                } catch (e: Exception) {
                    Log.e("MainScreen", "Failed to restart service: ${e.message}")
                }
            },
            onDismiss = { showProviderPicker = false }
        )
    }

    Scaffold(
        topBar = { OlleeHeader(permissionsOk = permsOk, onSettingsClick = { nav.navigate(Route.Settings) }) },
        containerColor = OlleeColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OlleeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OlleeSpacing.lg)
        ) {
            StatusBanner(
                text = "Initializing...",
                tone = StatusBannerTone.POSITIVE,
                modifier = Modifier.padding(bottom = OlleeSpacing.sm)
            )

            SectionLabel(text = "Glycemia Sources")

            RichSelectorRow(
                title = selectedProvider.displayName,
                subtitle = "$lastBg" + (if (!lastDelta.isNaN()) " (${String.format("%+.1f", lastDelta)})" else "") + " @ $timeAgoStr",
                leadingIcon = Icons.Filled.Tune,
                highlighted = true,
                trailingActions = {
                    RowActionIcon(
                        icon = Icons.Filled.SwapHoriz,
                        contentDescription = "Switch provider",
                        onClick = { showProviderPicker = true }
                    )
                    RowActionIcon(
                        icon = Icons.Filled.Edit,
                        contentDescription = "Configure",
                        onClick = { nav.navigate(Route.ProviderConfig(selectedProvider.id)) }
                    )
                }
            )

            if (showGraphOptions) {
                GraphRangeDialog(
                    currentRange = selectedGraphRange,
                    onRangeSelected = { selectedGraphRange = it; showGraphOptions = false },
                    onClearHistory = {
                        com.arthur.bgollee.GlycemiaHistoryStore.clear(context)
                        showGraphOptions = false
                        refreshGraphTrigger++
                    },
                    onDismiss = { showGraphOptions = false }
                )
            }

            FoldableSection(
                title = "Glycemia History",
                expanded = graphExpanded,
                onToggle = { graphExpanded = it }
            ) {
                AndroidView(
                    factory = { ctx ->
                        GlycemiaGraphView(ctx).apply {
                            setDisplayRange(selectedGraphRange)
                            setOnLongClickListener {
                                showGraphOptions = true
                                true
                            }
                        }
                    },
                    update = { view ->
                        refreshGraphTrigger
                        view.setDisplayRange(selectedGraphRange)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(top = OlleeSpacing.md)
                )
            }

            SectionLabel(text = "Watches")

            if (watchStatuses.isEmpty()) {
                Text("No paired watches", color = OlleeColors.TextSecondary)
            } else {
                var firstSyncedSeen = false
                watchStatuses.forEach { status ->
                    val stateStr = when (status.state) {
                        WatchConnState.SYNCED -> "Synced"
                        WatchConnState.OFFLINE -> "Offline"
                        WatchConnState.CONNECTING -> "Connecting..."
                        WatchConnState.ERROR -> "Error"
                    }
                    val isSynced = status.state == WatchConnState.SYNCED
                    val shouldHighlight = isSynced && !firstSyncedSeen
                    if (isSynced) firstSyncedSeen = true

                    RichSelectorRow(
                        title = status.watch.name,
                        subtitle = "$stateStr  ·  ${status.watch.address}",
                        highlighted = shouldHighlight,
                        trailingActions = {
                            RowActionIcon(
                                icon = Icons.Filled.Edit,
                                contentDescription = "Rename",
                                onClick = { /* Phase 5: edit dialog */ }
                            )
                            RowActionIcon(
                                icon = Icons.Filled.Delete,
                                contentDescription = "Remove",
                                onClick = { /* Phase 5: remove watch */ }
                            )
                        }
                    )
                }
            }

            PillButton(
                text = "Pair Ollee Watch",
                style = PillButtonStyle.SUCCESS,
                onClick = { nav.navigate(Route.WatchPairing) }
            )

            Spacer(Modifier.height(OlleeSpacing.lg))
        }
    }
}

@Composable
private fun ProviderPickerDialog(
    context: Context,
    currentProviderId: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val providers = GlycemiaProviderManager.allProviders

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Provider") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)) {
                providers.forEach { provider ->
                    SimpleSelector(
                        text = provider.displayName,
                        selected = provider.id == currentProviderId,
                        onClick = { onSelected(provider.id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun GraphRangeDialog(
    currentRange: Int,
    onRangeSelected: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val ranges = listOf(2, 3, 4, 6, 12, 24)
    val rangeLabels = listOf("2 hours", "3 hours", "4 hours", "6 hours", "12 hours", "24 hours")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Graph Range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)) {
                ranges.zip(rangeLabels).forEach { (range, label) ->
                    SimpleSelector(
                        text = label,
                        selected = range == currentRange,
                        onClick = { onRangeSelected(range) }
                    )
                }
                PillButton(
                    text = "Clear History",
                    style = PillButtonStyle.SECONDARY_DARK,
                    onClick = onClearHistory,
                    modifier = Modifier.padding(top = OlleeSpacing.lg)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun formatTimeAgo(timestampMs: Long): String {
    if (timestampMs == 0L) return "--"
    val now = System.currentTimeMillis()
    val diffMs = now - timestampMs
    val diffSecs = diffMs / 1000
    val diffMins = diffSecs / 60
    val diffHours = diffMins / 60

    return when {
        diffSecs < 60 -> "$diffSecs seconds ago"
        diffMins < 60 -> "$diffMins minute${if (diffMins > 1) "s" else ""} ago"
        diffHours < 24 -> "$diffHours hour${if (diffHours > 1) "s" else ""} ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestampMs))
        }
    }
}

private fun arePermissionsGranted(context: Context): Boolean {
    val perms = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        perms.add(Manifest.permission.BLUETOOTH_SCAN)
    }

    if (Build.VERSION.SDK_INT >= 34) {
        perms.add(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        perms.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    return perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
}
