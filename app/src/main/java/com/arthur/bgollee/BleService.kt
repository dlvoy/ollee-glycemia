package com.arthur.bgollee

import android.app.*
import android.bluetooth.*
import android.content.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.*
import com.arthur.bgollee.MainActivity


class BleService : Service() {

    private lateinit var notificationManager: NotificationManager
    private var gatt: BluetoothGatt? = null
    private var deviceAddress: String? = null
    private lateinit var prefs: SharedPreferences

    private var isConnecting = false
    private var isConnected = false
    private var servicesReady = false

    private var pendingBg: String? = null

    // ✅ Anti spam + état erreur
    private var lastSent: String? = null
    private var isInErrorState = false

    companion object {
        const val CHANNEL_ID = "ble_service_channel"

        private const val TIMEOUT_MS = 15 * 60 * 1000L // 15 min

        val SERVICE_UUID =
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")

        val CHAR_UUID =
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    }

    // ========================
    // RECEIVER BT
    // ========================

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {

                when (intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )) {

                    BluetoothAdapter.STATE_OFF -> {
                        log("🔴 Bluetooth OFF")
                        isConnected = false
                        servicesReady = false
                        gatt?.close()
                        gatt = null
                    }

                    BluetoothAdapter.STATE_ON -> {
                        log("🟢 Bluetooth ON → reconnexion")
                        Handler(Looper.getMainLooper()).postDelayed({
                            connect()
                        }, 1000)
                    }
                }
            }
        }
    }

    // ========================
    // LIFECYCLE
    // ========================

    override fun onCreate() {
        super.onCreate()

        prefs = getSharedPreferences("data", MODE_PRIVATE)
        deviceAddress = prefs.getString("device_address", null)

        notificationManager = getSystemService(NotificationManager::class.java)

        createNotificationChannel()
        startForeground(1, createNotification("🔄 Initialisation..."))

        registerReceiver(btReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        log("🚀 Service créé")

        Handler(Looper.getMainLooper()).post { connect() }

        startTimeoutWatcher()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(btReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val bg = intent?.getStringExtra("bg")

        intent?.getStringExtra("device_address")?.let {
            deviceAddress = it
            prefs.edit().putString("device_address", it).apply()
        }

        if (bg != null) {
            handleBg(bg)
        }

        if (gatt == null && !isConnecting) {
            connect()
        }

        return START_STICKY
    }

    // ========================
    // GESTION BG
    // ========================

    private fun handleBg(bg: String) {

        val now = System.currentTimeMillis()

        val formatted = formatBg(bg)

        pendingBg = formatted
        isInErrorState = false // reset erreur

        prefs.edit()
            .putString("last_bg", formatted.trim())
            .putLong("last_time", now)
            .apply()

        sendBroadcast(Intent("BG_UPDATED"))

        trySend()
    }

    private fun formatBg(bg: String?): String {

        if (bg.isNullOrBlank()) return "Err "

        val raw = bg.replace(",", ".")

        // ===== mmol =====
        if (raw.contains(".")) {

            val mmol = raw.toFloatOrNull() ?: return "Err "

            val safe = mmol.coerceIn(0f, 99.9f)

            return String.format("%.1f", safe)
                .take(4)
                .padStart(4, ' ')
        }

        // ===== mg/dL =====
        val mgdl = raw.replace("[^0-9]".toRegex(), "")
            .toIntOrNull() ?: return "Err "

        val safe = mgdl.coerceIn(0, 999)

        return safe.toString().padStart(4, ' ')
    }

    // ========================
    // TIMEOUT WATCHER
    // ========================

    private fun startTimeoutWatcher() {

        val handler = Handler(Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {

                val lastTime = prefs.getLong("last_time", 0L)
                val now = System.currentTimeMillis()

                if (now - lastTime > TIMEOUT_MS) {

                    if (!isInErrorState) {
                        log("⏱ Timeout → ERROR")

                        pendingBg = "Err "
                        isInErrorState = true

                        trySend()
                    }
                }

                handler.postDelayed(this, 60_000)
            }
        }

        handler.post(runnable)
    }

    // ========================
    // CONNECTION
    // ========================

    private fun connect() {

        if (isConnecting) return

        val addr = deviceAddress ?: return

        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val device = manager.adapter.getRemoteDevice(addr)

        gatt?.close()
        gatt = null

        log("🔗 Connexion à $addr")

        isConnecting = true

        gatt = device.connectGatt(
            this,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {

            isConnecting = false

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true
                servicesReady = false
                gatt = g

                g.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                g.discoverServices()

                updateNotification("🟢 Connecté")
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false
                servicesReady = false

                gatt?.close()
                gatt = null

                updateNotification("🔴 Reconnexion...")

                Handler(Looper.getMainLooper()).postDelayed({
                    connect()
                }, 3000)
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            servicesReady = true

            Handler(Looper.getMainLooper()).postDelayed({
                trySend()
            }, 1000)
        }
    }

    // ========================
    // ENVOI
    // ========================

    private fun trySend() {

        val bg = pendingBg ?: return

        if (!isConnected || !servicesReady) return

        // ✅ anti spam
        if (bg == lastSent) return

        sendToWatch(bg)

        lastSent = bg
        pendingBg = null
    }

    private fun sendToWatch(bg: String) {

        val g = gatt ?: return

        val service = g.getService(SERVICE_UUID) ?: return
        val charac = service.getCharacteristic(CHAR_UUID) ?: return

        val payload = byteArrayOf(
            0x02, 0x2f,
            0x20, 0x20
        ) + bg.toByteArray(Charsets.US_ASCII)

        val crc = crc16(payload)

        val packet = byteArrayOf(
            0x00,
            (payload.size + 4).toByte(),
            0xaa.toByte(),
            0x55,
            (crc shr 8).toByte(),
            (crc and 0xFF).toByte()
        ) + payload

        charac.value = packet
        charac.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        g.writeCharacteristic(charac)
    }

    private fun crc16(data: ByteArray): Int {
        var crc = 0xFFFF

        for (b in data) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)

            repeat(8) {
                crc = if ((crc and 0x8000) != 0)
                    (crc shl 1) xor 0x1021
                else crc shl 1

                crc = crc and 0xFFFF
            }
        }
        return crc
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("xDrip → Watch")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BLE Service",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotification(text: String) {
        notificationManager.notify(1, createNotification(text))
    }

    private fun log(msg: String) {
        Log.d("BleService", msg)
    }
}