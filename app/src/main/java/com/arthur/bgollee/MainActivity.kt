package com.arthur.bgollee

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlertDialog


class MainActivity : AppCompatActivity() {

    private lateinit var tableLayout: TableLayout
    private lateinit var valueGlycemia: TextView
    private lateinit var valueReceivedAt: TextView
    private lateinit var valueStatus: TextView
    private lateinit var valueWatch: TextView

    private lateinit var btnPermission: Button
    private lateinit var btnSelectDevice: Button

    private var currentStatus: String = ""

    // ========================
    // RECEIVERS
    // ========================

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            currentStatus = intent.getStringExtra("status") ?: ""
            updateUI()
        }
    }

    private val bgReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateUI()
        }
    }

    // ========================
    // LIFECYCLE
    // ========================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        tableLayout = TableLayout(this).apply {
            setColumnStretchable(1, true)
            setPadding(0, 120, 0, 120)
        }

        fun createRow(icon: String, label: String): Pair<TableRow, TextView> {
            val row = TableRow(this).apply {
                setPadding(0, 10, 0, 10)
            }

            val labelTv = TextView(this).apply {
                text = "$icon $label:"
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.END
                setPadding(0, 0, 20, 0)
            }

            val valueTv = TextView(this).apply {
                textSize = 18f
                gravity = Gravity.START
            }

            row.addView(labelTv)
            row.addView(valueTv)
            return Pair(row, valueTv)
        }

        val (row1, v1) = createRow("🩸", getString(R.string.glycemia))
        valueGlycemia = v1
        tableLayout.addView(row1)

        val (row2, v2) = createRow("⌚", getString(R.string.received_at))
        valueReceivedAt = v2
        tableLayout.addView(row2)

        val (row3, v3) = createRow("🔵", getString(R.string.status))
        valueStatus = v3
        tableLayout.addView(row3)

        val (row4, v4) = createRow("⚡", getString(R.string.showing_on_watch))
        valueWatch = v4.apply {
            typeface = android.graphics.Typeface.MONOSPACE
            paint.isFakeBoldText = true
        }
        tableLayout.addView(row4)

        btnPermission = Button(this).apply {
            text = getString(R.string.permission_button)
        }

        btnSelectDevice = Button(this).apply {
            text = getString(R.string.select_device)
        }

        layout.addView(tableLayout)
        layout.addView(btnPermission)
        layout.addView(btnSelectDevice)

        setContentView(layout)

        btnPermission.setOnClickListener { requestPermissions() }
        btnSelectDevice.setOnClickListener { showPairedDevices() }

        updateUI()

        // ✅ AUTO START SERVICE
        startBleServiceSafe()
    }

    override fun onStart() {
        super.onStart()

        ContextCompat.registerReceiver(
            this, statusReceiver,
            IntentFilter("BLE_STATUS"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        ContextCompat.registerReceiver(
            this, bgReceiver,
            IntentFilter("BG_UPDATED"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(statusReceiver)
        unregisterReceiver(bgReceiver)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    // ========================
    // BLE SERVICE
    // ========================

    private fun startBleService(address: String) {
        val intent = Intent(this, BleService::class.java)
        intent.putExtra("device_address", address)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(intent)
            else
                startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("BGOllee", "Failed to start service: ${e.message}")
        }
    }

    private fun startBleServiceSafe() {
        val addr = getSharedPreferences("data", MODE_PRIVATE)
            .getString("device_address", null)

        if (addr != null) {
            startBleService(addr)
        } else {
            Toast.makeText(this, getString(R.string.no_device), Toast.LENGTH_SHORT).show()
        }
    }

    // ========================
    // UI
    // ========================

    private fun updateUI() {
        val prefs = getSharedPreferences("data", MODE_PRIVATE)

        val bg = prefs.getString("last_bg", "--")
        val deltaFloat = prefs.getFloat("last_delta", Float.NaN)
        val lastSent = prefs.getString("last_sent", "      ") // 6 chars default
        val time = prefs.getLong("last_time", 0)

        val formattedTime =
            if (time != 0L)
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))
            else "--"

        val statusText = when {
            currentStatus.isNotEmpty() -> currentStatus
            bg != "--" -> getString(R.string.last_data)
            else -> getString(R.string.inactive)
        }

        val deltaStr = if (!deltaFloat.isNaN()) {
            String.format("%+.1f", deltaFloat)
        } else {
            ""
        }

        val unit = if (bg != null && (bg.contains(".") || bg.contains(","))) "mmol/L" else "mg/dL"

        val glycemiaLabelText = if (deltaStr.isNotEmpty()) {
            "$bg ($deltaStr) $unit"
        } else {
            "$bg $unit"
        }

        valueGlycemia.text = glycemiaLabelText
        valueReceivedAt.text = formattedTime
        valueStatus.text = statusText
        valueWatch.text = "\"$lastSent\""
    }

    // ========================
    // DEVICE SELECTION
    // ========================

    private fun disableBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            val intent = Intent(
                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            )

            intent.data = android.net.Uri.parse("package:$packageName")

            startActivity(intent)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showPairedDevices() {

        if (!hasBluetoothPermission()) {
            Toast.makeText(this, getString(R.string.permission_button), Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()

        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(this, getString(R.string.bluetooth_required), Toast.LENGTH_SHORT).show()
            return
        }

        val devices = adapter.bondedDevices

        if (devices.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_paired_devices), Toast.LENGTH_SHORT).show()
            return
        }

        val list = devices.map { "${it.name} - ${it.address}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_device))
            .setItems(list) { _, which ->

                val device = devices.elementAt(which)

                saveDevice(device.address)

                Toast.makeText(this, device.name, Toast.LENGTH_SHORT).show()

                startBleService(device.address)
            }
            .show()
    }

    private fun saveDevice(address: String) {
        getSharedPreferences("data", MODE_PRIVATE)
            .edit()
            .putString("device_address", address)
            .apply()
    }

    // ========================
    // PERMISSIONS
    // ========================

    private fun requestPermissions() {

        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // BLE Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        // Android 14+
        if (Build.VERSION.SDK_INT >= 34) {
            perms.add(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE)
        }

        // 🔔 Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)

        if (results.isNotEmpty() && results.all { it == PackageManager.PERMISSION_GRANTED }) {

            Toast.makeText(this, getString(R.string.permissions_ok), Toast.LENGTH_SHORT).show()

            disableBatteryOptimization() // 🔥 HERE

            startBleServiceSafe()

        } else {
            Toast.makeText(this, getString(R.string.permissions_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        else true
    }
}