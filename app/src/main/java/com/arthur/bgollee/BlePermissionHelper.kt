package com.arthur.bgollee

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BlePermissionHelper {
    fun canStartConnectedDeviceForegroundService(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true
        }

        val hasForegroundServicePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE
        ) == PackageManager.PERMISSION_GRANTED

        val hasBluetoothPermission = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        ).any { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        return hasForegroundServicePermission && hasBluetoothPermission
    }
}