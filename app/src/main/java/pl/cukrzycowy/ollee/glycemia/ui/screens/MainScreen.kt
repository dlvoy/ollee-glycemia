package pl.cukrzycowy.ollee.glycemia.ui.screens

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.cukrzycowy.ollee.glycemia.R
import kotlinx.coroutines.delay
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import pl.cukrzycowy.ollee.glycemia.AppState
import pl.cukrzycowy.ollee.glycemia.GlycemiaGraphView
import pl.cukrzycowy.ollee.glycemia.GlycemiaProviderManager
import pl.cukrzycowy.ollee.glycemia.WatchConnState
import pl.cukrzycowy.ollee.glycemia.WatchStatus
import pl.cukrzycowy.ollee.glycemia.ui.components.FoldableSection
import pl.cukrzycowy.ollee.glycemia.ui.components.OlleeHeader
import pl.cukrzycowy.ollee.glycemia.ui.components.PillButton
import pl.cukrzycowy.ollee.glycemia.ui.components.PillButtonStyle
import pl.cukrzycowy.ollee.glycemia.ui.components.SectionLabel
import pl.cukrzycowy.ollee.glycemia.ui.components.SimpleSelector
import pl.cukrzycowy.ollee.glycemia.ui.components.StatusBanner
import pl.cukrzycowy.ollee.glycemia.ui.components.StatusBannerTone
import pl.cukrzycowy.ollee.glycemia.ui.nav.AppNavController
import pl.cukrzycowy.ollee.glycemia.ui.nav.Route
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeColors
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeShapes
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeSpacing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

enum class ProviderDataStatus {
    AWAITING, CURRENT, LATE, STALE, NO_DATA
}

data class ProviderDataStatusInfo(
    val status: ProviderDataStatus,
    val labelResId: Int,
    val color: Color
)

private fun calculateProviderStatus(context: Context, lastTimeMs: Long): ProviderDataStatusInfo {
    val now = System.currentTimeMillis()
    val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)
    val providerSwitchTimeMs = prefs.getLong("provider_switch_time", 0L)

    val isAwaitingWindow = (now - providerSwitchTimeMs) < (5.5 * 60 * 1000)

    if (lastTimeMs == 0L) {
        return if (isAwaitingWindow) {
            ProviderDataStatusInfo(
                ProviderDataStatus.AWAITING,
                R.string.provider_status_awaiting,
                Color(0xFF0099CC)
            )
        } else {
            ProviderDataStatusInfo(
                ProviderDataStatus.NO_DATA,
                R.string.provider_status_no_data,
                Color(0xFFCC0000)
            )
        }
    }

    val ageMs = now - lastTimeMs
    val ageMins = ageMs / 60000

    return when {
        ageMins < 5 + 0.5 -> ProviderDataStatusInfo(
            ProviderDataStatus.CURRENT,
            R.string.provider_status_current,
            Color(0xFF00AA00)
        )
        ageMins < 10 + 0.5 -> ProviderDataStatusInfo(
            ProviderDataStatus.LATE,
            R.string.provider_status_late,
            Color(0xFFCC8800)
        )
        ageMins < 30 -> ProviderDataStatusInfo(
            ProviderDataStatus.STALE,
            R.string.provider_status_stale,
            Color(0xFFCC0000)
        )
        else -> ProviderDataStatusInfo(
            ProviderDataStatus.NO_DATA,
            R.string.provider_status_no_data,
            Color(0xFFCC0000)
        )
    }
}

@Composable
fun MainScreen(nav: AppNavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)

    val permsOk = arePermissionsGranted(context)
    var lastBg by remember { mutableStateOf(prefs.getString("last_bg", "--") ?: "--") }
    var lastDelta by remember { mutableStateOf(prefs.getFloat("last_delta", Float.NaN)) }
    var lastTime by remember { mutableStateOf(prefs.getLong("last_time", 0L)) }
    val timeStr = formatExactTime(lastTime)

    var selectedProvider by remember { mutableStateOf(GlycemiaProviderManager.getSelected(context)) }
    val watchStatuses by AppState.watchStatuses.collectAsState()

    var graphExpanded by remember { mutableStateOf(true) }
    var showProviderPicker by remember { mutableStateOf(false) }
    var showGraphOptions by remember { mutableStateOf(false) }
    var selectedGraphRange by remember { mutableStateOf(2) }
    var refreshGraphTrigger by remember { mutableStateOf(0) }
    var providerStatus by remember { mutableStateOf(calculateProviderStatus(context, lastTime)) }
    var previousStatus by remember { mutableStateOf(providerStatus.status) }

    LaunchedEffect(Unit) {
        val glycemiaReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "GLYCEMIA_UPDATED") {
                    val newLastBg = prefs.getString("last_bg", "--") ?: "--"
                    val newLastDelta = prefs.getFloat("last_delta", Float.NaN)
                    val newLastTime = prefs.getLong("last_time", 0L)

                    if (newLastBg != lastBg || newLastTime != lastTime) {
                        lastBg = newLastBg
                        lastDelta = newLastDelta
                        lastTime = newLastTime
                        refreshGraphTrigger++
                    }
                }
            }
        }

        context.registerReceiver(glycemiaReceiver, IntentFilter("GLYCEMIA_UPDATED"), Context.RECEIVER_NOT_EXPORTED)

        try {
            while (true) {
                delay(2000)
                val newLastBg = prefs.getString("last_bg", "--") ?: "--"
                val newLastDelta = prefs.getFloat("last_delta", Float.NaN)
                val newLastTime = prefs.getLong("last_time", 0L)

                if (newLastTime != lastTime || newLastBg != lastBg) {
                    lastBg = newLastBg
                    lastDelta = newLastDelta
                    lastTime = newLastTime
                    refreshGraphTrigger++
                }

                val newStatus = calculateProviderStatus(context, newLastTime)
                if (newStatus.status != providerStatus.status) {
                    previousStatus = providerStatus.status
                    providerStatus = newStatus
                    if ((previousStatus == ProviderDataStatus.CURRENT || previousStatus == ProviderDataStatus.LATE) &&
                        (newStatus.status == ProviderDataStatus.STALE || newStatus.status == ProviderDataStatus.NO_DATA || newStatus.status == ProviderDataStatus.AWAITING)) {
                        sendClearGlycemiaToWatches(context)
                    }
                }

                syncWatchListWithStore(context)
            }
        } finally {
            context.unregisterReceiver(glycemiaReceiver)
        }
    }

    if (showProviderPicker) {
        ProviderPickerDialog(
            context = context,
            currentProviderId = selectedProvider.id,
            onSelected = { newProviderId ->
                prefs.edit()
                    .putLong("provider_switch_time", System.currentTimeMillis())
                    .putString("last_bg", "--")
                    .putFloat("last_delta", Float.NaN)
                    .putLong("last_time", 0L)
                    .apply()
                GlycemiaProviderManager.setSelected(context, newProviderId)
                selectedProvider = GlycemiaProviderManager.getSelected(context)
                showProviderPicker = false

                // Reset UI to show "Initializing..."
                lastBg = "--"
                lastDelta = Float.NaN
                lastTime = 0L

                // Restart service with new provider
                val intent = Intent(context, pl.cukrzycowy.ollee.glycemia.BleService::class.java).apply {
                    action = pl.cukrzycowy.ollee.glycemia.BleService.ACTION_SWITCH_PROVIDER
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

                // Resend glycemia to watches after a short delay
                Handler(Looper.getMainLooper()).postDelayed({
                    val resendIntent = Intent(context, pl.cukrzycowy.ollee.glycemia.BleService::class.java).apply {
                        action = pl.cukrzycowy.ollee.glycemia.BleService.ACTION_RESEND_GLYCEMIA
                    }
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(resendIntent)
                        } else {
                            @Suppress("DEPRECATION")
                            context.startService(resendIntent)
                        }
                    } catch (e: Exception) {
                        Log.e("MainScreen", "Failed to resend glycemia: ${e.message}")
                    }
                }, 100)
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
            val (bannerText, bannerTone, bannerOnClick) = getBannerState(permsOk, providerStatus, watchStatuses, nav)
            StatusBanner(
                text = bannerText,
                tone = bannerTone,
                onClick = bannerOnClick,
                modifier = Modifier.padding(bottom = OlleeSpacing.sm)
            )

            SectionLabel(text = stringResource(R.string.glycemia_source))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OlleeColors.SurfaceCard, OlleeShapes.Pill)
                    .border(1.5.dp, providerStatus.color, OlleeShapes.Pill)
                    .padding(horizontal = OlleeSpacing.xl, vertical = OlleeSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.md)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getLocalizedProviderName(selectedProvider.id),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OlleeColors.TextPrimary,
                        lineHeight = 22.sp
                    )
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)
                    ) {
                        Text(
                            text = stringResource(providerStatus.labelResId),
                            color = providerStatus.color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "$lastBg" + (if (!lastDelta.isNaN()) " (${String.format("%+.1f", lastDelta)})" else "") + " @ $timeStr",
                            color = OlleeColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showProviderPicker = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = stringResource(R.string.switch_provider),
                            tint = OlleeColors.TextPrimary
                        )
                    }
                    IconButton(onClick = { nav.navigate(Route.ProviderConfig(selectedProvider.id)) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.configure),
                            tint = OlleeColors.TextPrimary
                        )
                    }
                }
            }

            if (showGraphOptions) {
                GraphRangeDialog(
                    currentRange = selectedGraphRange,
                    onRangeSelected = { selectedGraphRange = it; showGraphOptions = false },
                    onClearHistory = {
                        pl.cukrzycowy.ollee.glycemia.GlycemiaHistoryStore.clear(context)
                        showGraphOptions = false
                        refreshGraphTrigger++
                    },
                    onDismiss = { showGraphOptions = false }
                )
            }

            FoldableSection(
                title = stringResource(R.string.glycemia_history),
                expanded = graphExpanded,
                onToggle = { graphExpanded = it }
            ) {
                val graphViewRef = remember { mutableStateOf<GlycemiaGraphView?>(null) }

                LaunchedEffect(refreshGraphTrigger, selectedGraphRange) {
                    graphViewRef.value?.let { view ->
                        view.setDisplayRange(selectedGraphRange)
                        view.invalidate()
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        GlycemiaGraphView(ctx).apply {
                            graphViewRef.value = this
                            setDisplayRange(selectedGraphRange)
                            setOnLongClickListener {
                                showGraphOptions = true
                                true
                            }
                        }
                    },
                    update = { view ->
                        view.setDisplayRange(selectedGraphRange)
                        view.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(top = OlleeSpacing.md)
                )
            }

            SectionLabel(text = stringResource(R.string.watches))

            if (watchStatuses.isEmpty()) {
                Text(stringResource(R.string.no_paired_watches), color = OlleeColors.TextSecondary)
            } else {
                var editingWatchId by remember { mutableStateOf<String?>(null) }
                var editingWatchName by remember { mutableStateOf("") }
                var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

                watchStatuses.forEach { status ->
                    val isStaleData = status.state == WatchConnState.SYNCED && (status.lastSentValue == "--- --" || status.lastSentValue == "Err   ")
                    val effectiveState = if (status.isOfflineByTimeout) WatchConnState.OFFLINE else status.state
                    val stateColor = when {
                        status.state == WatchConnState.ERROR -> Color(0xFFCC0000)
                        isStaleData -> Color(0xFF999999)
                        status.isOfflineByTimeout -> Color(0xFF999999)
                        status.state == WatchConnState.SYNCED -> Color(0xFF00AA00)
                        status.state == WatchConnState.CONNECTING -> Color(0xFF0099CC)
                        status.state == WatchConnState.OFFLINE -> Color(0xFF999999)
                        else -> Color(0xFF999999)
                    }
                    val stateLabel = when {
                        status.state == WatchConnState.CONNECTING -> stringResource(R.string.watch_state_connecting)
                        status.state == WatchConnState.SYNCED -> stringResource(R.string.watch_state_synced)
                        status.state == WatchConnState.ERROR -> stringResource(R.string.watch_state_error)
                        status.isOfflineByTimeout -> stringResource(R.string.watch_state_offline)
                        status.state == WatchConnState.OFFLINE -> stringResource(R.string.watch_state_offline)
                        else -> stringResource(R.string.watch_state_offline)
                    }

                    if (editingWatchId == status.watch.address) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(OlleeColors.SurfaceCard, OlleeShapes.Pill)
                                .border(1.5.dp, stateColor, OlleeShapes.Pill)
                                .padding(horizontal = OlleeSpacing.xl, vertical = OlleeSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.md)
                        ) {
                            TextField(
                                value = editingWatchName,
                                onValueChange = { editingWatchName = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                            )
                            IconButton(
                                onClick = {
                                    pl.cukrzycowy.ollee.glycemia.WatchStore.rename(context, status.watch.address, editingWatchName)
                                    editingWatchId = null
                                    refreshWatchList(context)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = stringResource(R.string.confirm),
                                    tint = Color(0xFF00AA00)
                                )
                            }
                            IconButton(
                                onClick = { editingWatchId = null },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = Color(0xFF999999)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(OlleeColors.SurfaceCard, OlleeShapes.Pill)
                                .border(1.5.dp, stateColor, OlleeShapes.Pill)
                                .padding(horizontal = OlleeSpacing.xl, vertical = OlleeSpacing.md)
                                .clickable(enabled = status.isOfflineByTimeout) {
                                    val intent = Intent(context, pl.cukrzycowy.ollee.glycemia.BleService::class.java).apply {
                                        action = pl.cukrzycowy.ollee.glycemia.BleService.ACTION_MANUAL_SYNC_WATCH
                                        putExtra(pl.cukrzycowy.ollee.glycemia.BleService.EXTRA_WATCH_ADDRESS, status.watch.address)
                                    }
                                    try {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            context.startForegroundService(intent)
                                        } else {
                                            @Suppress("DEPRECATION")
                                            context.startService(intent)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainScreen", "Failed to trigger manual sync: ${e.message}")
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.md)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = status.watch.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OlleeColors.TextPrimary,
                                    lineHeight = 22.sp
                                )
                                Row(
                                    modifier = Modifier.padding(top = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)
                                ) {
                                    Text(
                                        text = stateLabel,
                                        color = stateColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    val detailText = when {
                                        status.isOfflineByTimeout && status.watch.lastSuccessfulSyncTimeMs > 0 ->
                                            "@ " + formatExactTime(status.watch.lastSuccessfulSyncTimeMs)
                                        status.state == WatchConnState.SYNCED && status.lastSyncTimeMs > 0 ->
                                            "@ " + formatExactTime(status.lastSyncTimeMs)
                                        status.state == WatchConnState.CONNECTING && status.lastSyncTimeMs > 0 ->
                                            "since @ " + formatExactTime(status.lastSyncTimeMs)
                                        status.state == WatchConnState.ERROR -> ""
                                        else -> status.watch.address
                                    }
                                    Text(
                                        text = detailText,
                                        color = OlleeColors.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    if (status.lastSentValue.isNotEmpty() && status.state != WatchConnState.CONNECTING && status.state != WatchConnState.ERROR) {
                                        Text(
                                            text = "\"${status.lastSentValue}\"",
                                            color = OlleeColors.TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showDeleteConfirm = status.watch.address },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.remove),
                                        tint = OlleeColors.TextPrimary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingWatchId = status.watch.address
                                        editingWatchName = status.watch.name
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.rename),
                                        tint = OlleeColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                if (showDeleteConfirm != null) {
                    val watchToDelete = watchStatuses.find { it.watch.address == showDeleteConfirm }?.watch
                    val deleteMessage = if (watchToDelete != null) {
                        "${watchToDelete.name}\n${watchToDelete.address}"
                    } else {
                        showDeleteConfirm ?: ""
                    }

                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = null },
                        title = { Text(stringResource(R.string.watch_delete_confirm_title)) },
                        text = { Text(deleteMessage) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    pl.cukrzycowy.ollee.glycemia.WatchStore.remove(context, showDeleteConfirm!!)
                                    showDeleteConfirm = null
                                    refreshWatchList(context)
                                }
                            ) {
                                Text(stringResource(R.string.delete))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = null }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }

            PillButton(
                text = stringResource(R.string.pair_watch),
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
        title = { Text(stringResource(R.string.select_provider)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OlleeSpacing.sm)) {
                providers.forEach { provider ->
                    SimpleSelector(
                        text = getLocalizedProviderName(provider.id),
                        selected = provider.id == currentProviderId,
                        onClick = { onSelected(provider.id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
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
        title = { Text(stringResource(R.string.graph_range)) },
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
                    text = stringResource(R.string.graph_clear_history),
                    style = PillButtonStyle.SECONDARY_DARK,
                    onClick = onClearHistory,
                    modifier = Modifier.padding(top = OlleeSpacing.lg)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

private fun sendClearGlycemiaToWatches(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(Intent(context, pl.cukrzycowy.ollee.glycemia.BleService::class.java).apply {
                action = pl.cukrzycowy.ollee.glycemia.BleService.ACTION_SEND_CLEAR_GLYCEMIA
            })
        } else {
            @Suppress("DEPRECATION")
            context.startService(Intent(context, pl.cukrzycowy.ollee.glycemia.BleService::class.java).apply {
                action = pl.cukrzycowy.ollee.glycemia.BleService.ACTION_SEND_CLEAR_GLYCEMIA
            })
        }
    } catch (e: Exception) {
        Log.e("MainScreen", "Failed to send clear glycemia: ${e.message}")
    }
}

@Composable
private fun getBannerState(
    permsOk: Boolean,
    providerStatus: ProviderDataStatusInfo,
    watchStatuses: List<WatchStatus>,
    nav: AppNavController
): Triple<String, StatusBannerTone, (() -> Unit)?> {
    return when {
        !permsOk -> Triple(
            stringResource(R.string.banner_permissions_required),
            StatusBannerTone.NEGATIVE,
            { nav.navigate(Route.Settings) }
        )
        watchStatuses.isEmpty() -> Triple(
            stringResource(R.string.banner_no_watch_paired),
            StatusBannerTone.NEGATIVE,
            { nav.navigate(Route.WatchPairing) }
        )
        providerStatus.status == ProviderDataStatus.NO_DATA -> Triple(
            stringResource(R.string.banner_no_glycemia),
            StatusBannerTone.NEGATIVE,
            null
        )
        providerStatus.status == ProviderDataStatus.STALE -> Triple(
            stringResource(R.string.banner_no_glycemia),
            StatusBannerTone.NEGATIVE,
            null
        )
        watchStatuses.all { it.state == WatchConnState.SYNCED } -> {
            val count = watchStatuses.size
            Triple(
                stringResource(R.string.banner_watches_synced, count, count),
                StatusBannerTone.POSITIVE,
                null
            )
        }
        watchStatuses.any { it.state == WatchConnState.SYNCED } -> {
            val synced = watchStatuses.count { it.state == WatchConnState.SYNCED }
            val total = watchStatuses.size
            Triple(
                stringResource(R.string.banner_watches_synced, synced, total),
                StatusBannerTone.POSITIVE,
                null
            )
        }
        else -> Triple(
            stringResource(R.string.banner_initializing),
            StatusBannerTone.POSITIVE,
            null
        )
    }
}

@Composable
private fun getLocalizedProviderName(providerId: String): String {
    return when (providerId) {
        "xdrip" -> stringResource(R.string.provider_xdrip)
        "constant" -> stringResource(R.string.provider_constant)
        "virtual_human" -> stringResource(R.string.provider_virtual_human)
        else -> providerId
    }
}

private fun formatExactTime(timestampMs: Long): String {
    if (timestampMs == 0L) return "--"
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestampMs))
}

private fun refreshWatchList(context: Context) {
    val watches = pl.cukrzycowy.ollee.glycemia.WatchStore.getAll(context)
    val currentStatuses = AppState.watchStatuses.value

    val updatedStatuses = watches.map { watch ->
        currentStatuses.find { it.watch.address == watch.address }?.copy(watch = watch)
            ?: WatchStatus(watch = watch, state = WatchConnState.OFFLINE)
    }

    AppState.publishWatchStatuses(updatedStatuses)
}

private fun syncWatchListWithStore(context: Context) {
    val storeWatches = pl.cukrzycowy.ollee.glycemia.WatchStore.getAll(context)
    val currentStatuses = AppState.watchStatuses.value

    val needsSync = storeWatches.size != currentStatuses.size ||
        storeWatches.any { storeWatch ->
            val currentWatch = currentStatuses.find { it.watch.address == storeWatch.address }?.watch
            currentWatch == null || currentWatch.name != storeWatch.name
        }

    if (needsSync) {
        val updatedStatuses = storeWatches.map { watch ->
            currentStatuses.find { it.watch.address == watch.address }?.copy(watch = watch)
                ?: WatchStatus(watch = watch, state = WatchConnState.OFFLINE)
        }
        AppState.publishWatchStatuses(updatedStatuses)
    }
}

private fun arePermissionsGranted(context: Context): Boolean {
    val perms = mutableListOf<String>()

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
