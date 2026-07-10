package com.arthur.bgollee.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthur.bgollee.ui.components.FullScreenScaffold
import com.arthur.bgollee.ui.components.SectionLabel
import com.arthur.bgollee.ui.theme.OlleeSpacing

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    FullScreenScaffold(title = "Settings", onBack = onBack) {
        SectionLabel(text = "Permissions")

        val perms = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.POST_NOTIFICATIONS
        )

        perms.forEach { perm ->
            val hasPermission = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            Button(
                onClick = { requestPermission(context, perm) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = (if (hasPermission) "✓ " else "✗ ") + perm.split(".").last())
            }
        }

        SectionLabel(text = "About", modifier = Modifier.fillMaxWidth())
        Text("Version: 1.0")
    }
}

private fun requestPermission(context: Context, permission: String) {
    if (context is android.app.Activity) {
        ActivityCompat.requestPermissions(context, arrayOf(permission), 100)
    }
}
