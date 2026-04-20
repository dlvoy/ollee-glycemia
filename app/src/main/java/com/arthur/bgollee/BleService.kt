package com.arthur.bgollee

import android.app.*
import android.bluetooth.*
import android.content.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.*

class BleService : Service() {

    private lateinit var notificationManager: NotificationManager
    private var gatt: BluetoothGatt? = null
    private var deviceAddress: String? = null
    private lateinit var prefs: SharedPreferences

    private var isConnecting = false
    private var isConnected = false
    private var servicesReady = false

    private var pendingBg: String? = null

    companion object {
        const val CHANNEL_ID = "ble_service_channel"

        val SERVICE_UUID =
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")

        val CHAR_UUID =
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    }

    // ========================
    // BLUETOOTH STATE RECEIVER
    // ========================

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {

                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )

                when (state) {

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

        registerReceiver(
            btReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )

        log("🚀 Service créé")

        Handler(Looper.getMainLooper()).post {
            connect()
        }
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
            log("📥 BG reçu: $bg")

            val cleanBg = bg
                .replace(",", ".")
                .replace("[^0-9.]".toRegex(), "")
                .split(".")[0]
                .take(3)

            if (cleanBg.isEmpty()) {
                log("❌ BG invalide")
                return START_STICKY
            }

            val formatted = cleanBg.padStart(4, ' ')
            pendingBg = formatted

            prefs.edit()
                .putString("last_bg", cleanBg)
                .putLong("last_time", System.currentTimeMillis())
                .apply()

            sendBroadcast(Intent("BG_UPDATED"))

            trySend()
        }

        if (gatt == null && !isConnecting) {
            connect()
        }

        return START_STICKY
    }

    // ========================
    // CONNECTION
    // ========================

    private fun connect() {

        if (isConnecting) {
            log("⚠️ Connexion déjà en cours")
            return
        }

        val addr = deviceAddress ?: run {
            log("❌ Pas d'adresse")
            return
        }

        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter

        val device = adapter.getRemoteDevice(addr)

        // 🔥 RESET GATT IMPORTANT
        if (gatt != null) {
            log("♻️ Reset GATT")
            gatt?.close()
            gatt = null
        }

        log("🔗 Connexion à $addr")

        isConnecting = true

        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(this, false, gattCallback)
        }
    }

    // ========================
    // GATT CALLBACK
    // ========================

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {

            log("STATE → status=$status newState=$newState")

            isConnecting = false

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                log("✅ Connecté")

                isConnected = true
                servicesReady = false
                gatt = g

                // 🔥 améliore stabilité
                g.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)

                g.discoverServices()
                sendStatus("Connecté")
                updateNotification("🟢 Connecté à la montre")
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                log("❌ Déconnecté")

                isConnected = false
                servicesReady = false

                gatt?.close()
                gatt = null

                sendStatus("Déconnecté")
                updateNotification("🔴 Déconnecté (reconnexion...)")

                Handler(Looper.getMainLooper()).postDelayed({
                    connect()
                }, 3000)
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {

            log("📡 Services découverts")

            for (service in g.services) {
                log("🧩 SERVICE: ${service.uuid}")
                for (char in service.characteristics) {
                    log("   🔹 CHAR: ${char.uuid}")
                }
            }

            servicesReady = true

            // 🔥 delay critique (sinon montre ignore)
            Handler(Looper.getMainLooper()).postDelayed({
                trySend()
            }, 1000)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                log("✅ Write confirmé")
            else
                log("❌ Write échoué: $status")
        }
    }

    // ========================
    // ENVOI
    // ========================

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        notificationManager.notify(1, notification)
    }

    private fun trySend() {

        val bg = pendingBg ?: return

        if (!isConnected) {
            log("⏳ Pas encore connecté")
            return
        }

        if (!servicesReady) {
            log("⏳ Services pas prêts")
            return
        }

        sendToWatch(bg)
        pendingBg = null
    }

    private fun sendToWatch(bg: String) {

        val g = gatt ?: return

        log("📡 Envoi glycémie: '$bg'")

        val service = g.getService(SERVICE_UUID) ?: run {
            log("❌ Service introuvable")
            return
        }

        val charac = service.getCharacteristic(CHAR_UUID) ?: run {
            log("❌ Characteristic introuvable")
            return
        }

        val payload = byteArrayOf(
            0x02, 0x2f,
            0x20, 0x20
        ) + bg.toByteArray()

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

        val success = g.writeCharacteristic(charac)

        log("📤 write = $success → $bg")
    }

    // ========================
    // CRC
    // ========================

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
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("xDrip → Watch")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 🔥 IMPORTANT
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BLE Service",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun log(msg: String) {
        Log.d("BleService", msg)
    }

    private fun sendStatus(status: String) {
        val intent = Intent("BLE_STATUS")
        intent.putExtra("status", status)
        sendBroadcast(intent)
    }
}