package com.arthur.bgollee.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.arthur.bgollee.R
import com.arthur.bgollee.WatchStore
import com.arthur.bgollee.ui.components.FullScreenScaffold
import com.arthur.bgollee.ui.theme.OlleeColors
import com.arthur.bgollee.ui.theme.OlleeShapes
import com.arthur.bgollee.ui.theme.OlleeSpacing

@SuppressLint("MissingPermission")
@Composable
fun WatchPairingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val bondedDevices = remember { mutableStateOf<List<android.bluetooth.BluetoothDevice>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val manager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = manager.adapter
            if (adapter?.isEnabled == true) {
                bondedDevices.value = adapter.bondedDevices
                    .filter { it.name?.contains("Ollee", ignoreCase = true) == true }
                    .sortedBy { it.name }
            }
        } catch (e: Exception) {
            // Bluetooth unavailable or permission denied
        }
    }

    FullScreenScaffold(title = stringResource(R.string.pair_watch_title), onBack = onBack) {
        if (bondedDevices.value.isEmpty()) {
            Text(stringResource(R.string.no_ollee_watches))
        } else {
            bondedDevices.value.forEach { device ->
                val pairedWatch = WatchStore.getAll(context).find { it.address == device.address }
                val alreadyPaired = pairedWatch != null
                val (statusColor, statusLabel) = if (alreadyPaired) {
                    Pair(Color(0xFF00AA00), stringResource(R.string.watch_status_paired))
                } else {
                    Pair(Color(0xFF0099CC), stringResource(R.string.watch_status_new))
                }

                // Use user-assigned name if paired, otherwise use device name
                val displayName = pairedWatch?.name ?: (device.name ?: "Unknown")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OlleeColors.SurfaceCard, OlleeShapes.Pill)
                        .border(1.5.dp, statusColor, OlleeShapes.Pill)
                        .padding(horizontal = OlleeSpacing.xl, vertical = OlleeSpacing.md)
                        .clickable {
                            WatchStore.add(context, device.address)
                            refreshWatchListInPairing(context)
                            onBack()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OlleeSpacing.md)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
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
                                text = statusLabel,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = device.address,
                                color = OlleeColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun refreshWatchListInPairing(context: android.content.Context) {
    val watches = WatchStore.getAll(context)
    val currentStatuses = com.arthur.bgollee.AppState.watchStatuses.value

    val updatedStatuses = watches.map { watch ->
        currentStatuses.find { it.watch.address == watch.address }?.copy(watch = watch)
            ?: com.arthur.bgollee.WatchStatus(watch = watch, state = com.arthur.bgollee.WatchConnState.OFFLINE)
    }

    com.arthur.bgollee.AppState.publishWatchStatuses(updatedStatuses)
}
