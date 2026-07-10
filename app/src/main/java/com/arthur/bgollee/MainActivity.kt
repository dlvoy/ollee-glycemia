package com.arthur.bgollee

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.arthur.bgollee.ui.nav.AppNavHost
import com.arthur.bgollee.ui.theme.OlleeTheme

/**
 * Entry point. Sets the Compose content tree via [setContent], auto-starts
 * the BLE service, and delegates all navigation/state concerns to
 * [AppNavHost] and [AppState].
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_BleTest)
        super.onCreate(savedInstanceState)

        setContent {
            OlleeTheme {
                AppNavHost()
            }
        }

        startBleServiceSafe()
    }

    override fun onResume() {
        super.onResume()
        // Sync the watch list in case it changed while the app was not running
        val intent = android.content.Intent(this, BleService::class.java).apply {
            action = BleService.ACTION_SYNC_WATCHES
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to sync watches: ${e.message}")
        }
    }

    private fun startBleServiceSafe() {
        val prefs = getSharedPreferences("data", MODE_PRIVATE)
        val hasWatches = WatchStore.getAll(this).isNotEmpty()
        val selectedProvider = GlycemiaProviderManager.getSelected(this)

        if (hasWatches || selectedProvider.id != "xdrip") {
            startBleServiceWithIntent(android.content.Intent(this, BleService::class.java))
        }
    }

    private fun startBleServiceWithIntent(intent: android.content.Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start service: ${e.message}")
        }
    }
}
