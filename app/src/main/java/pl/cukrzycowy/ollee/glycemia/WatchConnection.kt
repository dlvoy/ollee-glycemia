package pl.cukrzycowy.ollee.glycemia

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.UUID

/**
 * Owns the BLE link to a single paired watch: connect/reconnect lifecycle,
 * service discovery, and sending the formatted glycemia payload. Extracted
 * from the (formerly single-device) `BleService` so [BleService] can manage
 * an arbitrary number of these independently - one watch being out of range
 * never blocks delivery to the others.
 */
class WatchConnection(
    private val context: Context,
    watch: PairedWatch,
    private val onStateChanged: (WatchConnection, WatchConnState) -> Unit
) {
    companion object {
        private const val RECONNECT_DELAY_MS = 3000L
        private const val CONNECT_STAGGER_MS = 400L

        val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val CHAR_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

        fun crc16(data: ByteArray): Int {
            var crc = 0xFFFF
            for (b in data) {
                crc = crc xor ((b.toInt() and 0xFF) shl 8)
                repeat(8) {
                    crc = if ((crc and 0x8000) != 0) (crc shl 1) xor 0x1021 else crc shl 1
                    crc = crc and 0xFFFF
                }
            }
            return crc
        }
    }

    var watch: PairedWatch = watch
        private set

    var state: WatchConnState = WatchConnState.OFFLINE
        private set(value) {
            if (field == value) return
            field = value

            if (value == WatchConnState.ERROR) {
                errorTimeMs = System.currentTimeMillis()
                if (isOfflineByTimeout()) {
                    handler.postDelayed({ clearErrorIfStale() }, 5000)
                }
            } else if (value != WatchConnState.ERROR) {
                errorTimeMs = 0L
            }

            onStateChanged(this, value)
        }

    var lastSyncTimeMs: Long = 0L
        private set

    var lastConnectionAttemptTimeMs: Long = 0L
        private set

    var lastSentValue: String = ""
        private set

    private var errorTimeMs: Long = 0L

    private val handler = Handler(Looper.getMainLooper())
    private var gatt: BluetoothGatt? = null
    private var isConnecting = false
    private var isConnected = false
    private var servicesReady = false

    private var pendingBg: String? = null
    private var lastSent: String? = null

    private var torndown = false

    fun updateWatch(updated: PairedWatch) {
        watch = updated
    }

    private fun isOfflineByTimeout(): Boolean {
        val now = System.currentTimeMillis()
        return watch.lastSuccessfulSyncTimeMs == 0L ||
            (now - watch.lastSuccessfulSyncTimeMs) > (30 * 60 * 1000) // 30 minutes
    }

    private fun shouldMaintainConnection(): Boolean {
        return watch.activityState == WatchActivityState.ACTIVE || !watch.activityLabelSent
    }

    private fun clearErrorIfStale() {
        if (state != WatchConnState.ERROR) return
        if (!isOfflineByTimeout()) return
        if (isConnecting) return

        val now = System.currentTimeMillis()
        if ((now - errorTimeMs) >= 5000) {
            state = WatchConnState.OFFLINE
            errorTimeMs = 0L
        }
    }

    /** Attempts a connection after [staggerIndex] * [CONNECT_STAGGER_MS] delay,
     *  to avoid hammering the platform's BLE stack when many watches need to
     *  (re)connect at once (e.g. right after boot). */
    fun connect(staggerIndex: Int = 0) {
        if (torndown || isConnecting || isConnected) return

        if (!shouldMaintainConnection()) {
            disconnectSoft()
            return
        }

        handler.postDelayed({
            if (!torndown) connectNow()
        }, staggerIndex * CONNECT_STAGGER_MS)
    }

    fun submitReading(formattedBg: String) {
        pendingBg = formattedBg
        trySend()
    }

    /** Soft disconnect (e.g. system Bluetooth turned off) - the connection
     *  can still be [connect]ed again later, unlike [teardown]. */
    fun disconnectSoft() {
        handler.removeCallbacksAndMessages(null)
        gatt?.close()
        gatt = null
        isConnecting = false
        isConnected = false
        servicesReady = false
        state = WatchConnState.OFFLINE
    }

    /** Permanent removal (watch unpaired) - no further reconnect attempts. */
    fun teardown() {
        torndown = true
        disconnectSoft()
    }

    private fun connectNow() {
        if (!BlePermissionHelper.hasBluetoothRuntimeAccess(context)) {
            log("Bluetooth permissions missing, skipping connection to ${watch.address}")
            return
        }
        if (isConnecting) return

        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val device = try {
            manager.adapter.getRemoteDevice(watch.address)
        } catch (e: IllegalArgumentException) {
            log("Invalid address ${watch.address}: ${e.message}")
            return
        }

        gatt?.close()
        gatt = null

        log("Connecting to ${watch.address}")
        isConnecting = true
        lastConnectionAttemptTimeMs = System.currentTimeMillis()
        state = WatchConnState.CONNECTING

        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            isConnecting = false

            if (torndown) return

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true
                servicesReady = false
                gatt = g

                if (BlePermissionHelper.hasBluetoothRuntimeAccess(context)) {
                    g.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    g.discoverServices()
                }
                state = WatchConnState.CONNECTING
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false
                servicesReady = false
                gatt?.close()
                gatt = null

                state = if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    WatchConnState.OFFLINE
                } else {
                    WatchConnState.ERROR
                }

                if (shouldMaintainConnection() && !isOfflineByTimeout()) {
                    handler.postDelayed({ connectNow() }, RECONNECT_DELAY_MS)
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            servicesReady = true
            handler.postDelayed({ trySend() }, 1000)
        }
    }

    private fun trySend() {
        val bg = pendingBg ?: return
        if (!isConnected || !servicesReady) return
        if (bg == lastSent) {
            state = WatchConnState.SYNCED
            if (watch.activityState != WatchActivityState.ACTIVE) {
                WatchStore.markActivityLabelSent(context, watch.address)
                watch = watch.copy(activityLabelSent = true)
                disconnectSoft()
            }
            return
        }

        val sent = sendToWatch(bg)
        if (sent) {
            lastSent = bg
            pendingBg = null
            state = WatchConnState.SYNCED
            if (watch.activityState != WatchActivityState.ACTIVE) {
                WatchStore.markActivityLabelSent(context, watch.address)
                watch = watch.copy(activityLabelSent = true)
                disconnectSoft()
            }
        }
    }

    private fun sendToWatch(bg: String): Boolean {
        val g = gatt ?: return false
        val service = g.getService(SERVICE_UUID) ?: return false
        val charac = service.getCharacteristic(CHAR_UUID) ?: return false

        val payload = byteArrayOf(0x02, 0x2f) + bg.toByteArray(Charsets.US_ASCII)
        val crc = crc16(payload)
        val packet = byteArrayOf(
            0x00,
            (payload.size + 4).toByte(),
            0xaa.toByte(),
            0x55,
            (crc shr 8).toByte(),
            (crc and 0xFF).toByte()
        ) + payload

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeCharacteristic(charac, packet, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                @Suppress("DEPRECATION")
                run {
                    charac.value = packet
                    charac.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    g.writeCharacteristic(charac)
                }
            }
            lastSyncTimeMs = System.currentTimeMillis()
            lastSentValue = bg
            WatchStore.updateLastSyncTime(context, watch.address, lastSyncTimeMs)
            watch = watch.copy(lastSuccessfulSyncTimeMs = lastSyncTimeMs)
            log("Sent to ${watch.address} -> '$bg'")
            true
        } catch (e: SecurityException) {
            log("Missing permission to write to ${watch.address}: ${e.message}")
            false
        }
    }

    private fun log(msg: String) {
        Log.d("WatchConnection", msg)
    }
}
