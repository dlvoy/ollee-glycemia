package com.arthur.bgollee.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.arthur.bgollee.WatchStore
import com.arthur.bgollee.ui.components.FullScreenScaffold
import com.arthur.bgollee.ui.components.RichSelectorRow
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

    FullScreenScaffold(title = "Pair Ollee Watch", onBack = onBack) {
        if (bondedDevices.value.isEmpty()) {
            Text("No Ollee watches found. Please pair one via Bluetooth settings first.")
        } else {
            bondedDevices.value.forEach { device ->
                val alreadyPaired = WatchStore.getAll(context).any { it.address == device.address }
                val status = if (alreadyPaired) "Paired" else "New"
                RichSelectorRow(
                    title = device.name ?: "Unknown",
                    subtitle = "$status  ·  ${device.address}",
                    onClick = {
                        WatchStore.add(context, device.address)
                        onBack()
                    }
                )
            }
        }
    }
}
