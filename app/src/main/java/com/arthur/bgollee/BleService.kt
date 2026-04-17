package com.arthur.bgollee

import android.Manifest
import android.app.*
import android.bluetooth.*
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import java.util.*

class BleService : Service() {

    private var gatt: BluetoothGatt? = null
    private var isReady = false
    private var pendingBg: String? = null
    private var deviceAddress: String? = null // 🔥 FIX IMPORTANT

    private val SERVICE_UUID =
        UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")

    private val WRITE_UUID =
        UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

    // ========================
    // LIFECYCLE
    // ========================

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate() {
        super.onCreate()

        startForeground(1, notif())
        send("Service démarré 🚀")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // 🔥 récup adresse directe si présente
        intent?.getStringExtra("device_address")?.let {
            deviceAddress = it
            send("Adresse reçue: $it")
            connect()
        }

        val rawBg = intent?.getStringExtra("bg")

        if (rawBg != null) {

            send("🩸 BG brut: $rawBg")

            val cleanBg = rawBg
                .replace(",", ".")
                .replace("[^0-9.]".toRegex(), "")
                .split(".")[0]
                .take(3)

            if (cleanBg.isEmpty()) {
                send("❌ BG invalide")
                return START_STICKY
            }

            // 🔥 FORMAT MONTRE (CRITIQUE)
            val formattedBg = cleanBg.padStart(4, ' ')

            send("✅ BG formaté: '$formattedBg'")

            pendingBg = formattedBg

            // sauvegarde UI
            val prefs = getSharedPreferences("data", MODE_PRIVATE)
            prefs.edit()
                .putString("last_bg", cleanBg)
                .putLong("last_time", System.currentTimeMillis())
                .apply()

            sendBroadcast(Intent("BG_UPDATED").setPackage(packageName))

            if (gatt != null && isReady) {

                send("📤 Envoi direct")

                Handler(Looper.getMainLooper()).postDelayed({
                    sendNow()
                }, 300)

            } else {
                send("En attente BLE ⏳")
            }
        }

        return START_STICKY
    }

    // ========================
    // CONNECTION
    // ========================

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connect() {

        isReady = false

        val addr = deviceAddress
            ?: getSharedPreferences("data", MODE_PRIVATE)
                .getString("device_address", null)

        if (addr == null) {
            send("Pas de montre ❌")
            return
        }

        send("Connexion à $addr")
        val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(addr)

        gatt = device.connectGatt(this, false, gattCallback)
    }

    // ========================
    // GATT CALLBACK
    // ========================

    private val gattCallback = object : BluetoothGattCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(
            g: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                send("Connecté ✅")
                gatt = g
                g.requestMtu(247)

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                send("Déconnecté ❌")

                isReady = false
                gatt?.close()
                gatt = null

                Handler(Looper.getMainLooper()).postDelayed({
                    connect()
                }, 3000)
            }
        }

        override fun onMtuChanged(
            g: BluetoothGatt,
            mtu: Int,
            status: Int
        ) {
            send("MTU OK = $mtu")
            g.discoverServices()
        }

        override fun onServicesDiscovered(
            g: BluetoothGatt,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                send("Services OK ✅")
                isReady = true

                Handler(Looper.getMainLooper()).postDelayed({
                    sendNow()
                }, 500)

            } else {
                send("Erreur services ❌")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                send("✅ Write confirmé")
            } else {
                send("❌ Write échoué: $status")
            }
        }
    }

    // ========================
    // WRITE
    // ========================

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendNow() {
        val value = pendingBg ?: return

        gatt?.let {
            writeNow(it, value)
            pendingBg = null
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun writeNow(gatt: BluetoothGatt, value: String) {

        val service = gatt.getService(SERVICE_UUID) ?: run {
            send("Service introuvable ❌")
            return
        }

        val charac = service.getCharacteristic(WRITE_UUID) ?: run {
            send("Charac introuvable ❌")
            return
        }

        val payload = byteArrayOf(
            0x02, 0x2f,
            0x20, 0x20
        ) + value.toByteArray()

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

        val success = gatt.writeCharacteristic(charac)

        send("📤 Envoi: '$value' / OK=$success")
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

    // ========================
    // UTILS
    // ========================

    private fun notif(): Notification {
        val id = "ble"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(id, "BLE", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(chan)
        }

        return NotificationCompat.Builder(this, id)
            .setContentTitle("BLE actif")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }

    private fun send(msg: String) {
        val intent = Intent("BLE_STATUS")
        intent.setPackage(packageName)
        intent.putExtra("status", msg)
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}