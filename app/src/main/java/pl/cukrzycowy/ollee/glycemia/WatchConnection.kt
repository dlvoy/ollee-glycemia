package pl.cukrzycowy.ollee.glycemia

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.UUID

/**
 * Owns the BLE link to a single paired watch: connect/reconnect lifecycle,
 * service discovery, and sending the glycemia payload: the screensaver tag text
 * ([OapProtocol]) and, when the watch exposes the Forge service, frames for the
 * Forge watchface ([ForgeProtocol], paced by [ForgeFeed]). On Forge watches it
 * also asks whether the glycemia face is installed ([ForgeHandshake]) and stores
 * the answer in [PairedWatch.faceDetected]. Extracted from the
 * (formerly single-device) `BleService` so [BleService] can manage an arbitrary
 * number of these independently - one watch being out of range never blocks
 * delivery to the others.
 *
 * All state is confined to the main thread: public entry points called from
 * another thread (e.g. Nightscout's worker) are posted over.
 */
class WatchConnection(
    private val context: Context,
    watch: PairedWatch,
    private val onStateChanged: (WatchConnection, WatchConnState) -> Unit
) {
    companion object {
        private const val RECONNECT_DELAY_MS = 3000L
        private const val CONNECT_STAGGER_MS = 400L
        private const val KEY_OAP = "oap"
        private const val KEY_FORGE = "forge"
        private const val KEY_FORGE_NOTIFY = "forge_notify"
        private const val KEY_FORGE_QUERY = "forge_query"
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

    private var pendingOap: WatchPayload? = null
    private var lastSent: String? = null

    private var forgeRx: BluetoothGattCharacteristic? = null
    private var forgeTx: BluetoothGattCharacteristic? = null
    private val forgeFeed = ForgeFeed()
    private val handshake = ForgeHandshake()
    private val handshakeTimeout = Runnable { onHandshakeResult(handshake.onReplyTimeout()) }

    private val queue = GattWriteQueue(
        issue = ::issueWrite,
        onDrained = ::afterQueueDrained
    )

    private var torndown = false

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else handler.post(block)
    }

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
    fun connect(staggerIndex: Int = 0) = runOnMain {
        if (torndown || isConnecting || isConnected) return@runOnMain

        if (!shouldMaintainConnection()) {
            disconnectSoftNow()
            return@runOnMain
        }

        handler.postDelayed({
            if (!torndown) connectNow()
        }, staggerIndex * CONNECT_STAGGER_MS)
    }

    /** Hands the watch what it should show; sent as soon as the link is ready. */
    fun submit(payload: WatchPayload) = runOnMain {
        pendingOap = payload
        forgeFeed.submit(payload)
        flush()
    }

    /** Forces the Forge face to get a fresh frame and asks again whether it is installed (the user tapped "sync"). */
    fun resendToForge() = runOnMain {
        forgeFeed.reset()
        handshake.requestQuery(force = true)
        flush()
    }

    /** Periodic check from the service: sends the Forge keepalive when it is due. */
    fun tick() = runOnMain {
        if (isConnected && servicesReady && forgeRx != null) enqueueForgeIfDue()
    }

    /** Soft disconnect (e.g. system Bluetooth turned off) - the connection
     *  can still be [connect]ed again later, unlike [teardown]. */
    fun disconnectSoft() = runOnMain { disconnectSoftNow() }

    private fun disconnectSoftNow() {
        handler.removeCallbacksAndMessages(null)
        queue.clear()
        gatt?.close()
        gatt = null
        forgeRx = null
        forgeTx = null
        handshake.reset()
        isConnecting = false
        isConnected = false
        servicesReady = false
        state = WatchConnState.OFFLINE
    }

    /** Permanent removal (watch unpaired) - no further reconnect attempts. */
    fun teardown() = runOnMain {
        torndown = true
        disconnectSoftNow()
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

    // GATT callbacks arrive on binder threads: hop to the main thread before touching state
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            handler.post { handleConnectionStateChange(g, status, newState) }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            handler.post { handleServicesDiscovered(g) }
        }

        override fun onCharacteristicWrite(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            handler.post { queue.onWriteComplete(status == BluetoothGatt.GATT_SUCCESS) }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            handler.post { queue.onWriteComplete(status == BluetoothGatt.GATT_SUCCESS) }
        }

        // API 33+
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val uuid = characteristic.uuid
            val data = value.copyOf()
            handler.post { handleNotification(uuid, data) }
        }

        // Below API 33 only; from 33 on the overload above gets the value
        @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
            val uuid = characteristic.uuid
            val data = characteristic.value?.copyOf() ?: return
            handler.post { handleNotification(uuid, data) }
        }
    }

    private fun handleConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
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
            queue.clear()
            forgeRx = null
            forgeTx = null
            handshake.reset()
            handler.removeCallbacks(handshakeTimeout)
            gatt?.close()
            gatt = null

            state = if (status == BluetoothGatt.GATT_SUCCESS) {
                WatchConnState.OFFLINE
            } else {
                WatchConnState.ERROR
            }

            if (shouldMaintainConnection() && !isOfflineByTimeout()) {
                handler.postDelayed({ connectNow() }, RECONNECT_DELAY_MS)
            }
        }
    }

    private fun handleServicesDiscovered(g: BluetoothGatt) {
        if (torndown || !isConnected) return
        servicesReady = true
        queue.clear()
        val forge = g.getService(ForgeProtocol.SERVICE_UUID)
        forgeRx = forge?.getCharacteristic(ForgeProtocol.RX_UUID)
        forgeTx = forge?.getCharacteristic(ForgeProtocol.TX_UUID)?.takeIf {
            it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 &&
                it.getDescriptor(ForgeProtocol.CCCD_UUID) != null
        }
        forgeFeed.reset() // a new link: the face needs a frame right away
        handshake.reset()
        handler.removeCallbacks(handshakeTimeout)
        val supported = forgeRx != null
        if (supported != watch.forgeSupported) {
            log("Forge service ${if (supported) "found" else "gone"} on ${watch.address}")
            WatchStore.setForgeSupported(context, watch.address, supported)
            watch = watch.copy(forgeSupported = supported)
            onStateChanged(this, state) // republish so the UI can show the watchface icon
        }
        if (!supported) storeFaceDetected(false)
        handler.postDelayed({
            flush()
            startHandshake()
        }, 1000)
    }

    // ========================
    // FACE HANDSHAKE (spec 16)
    // ========================

    /** Turns on TX notifications; the `Q` query follows once they are on. */
    private fun startHandshake() {
        if (torndown || !isConnected || !servicesReady || forgeRx == null) return
        // A paused/stopped watch hangs up as soon as its label is out, so no reply could arrive
        if (watch.activityState != WatchActivityState.ACTIVE) return
        if (forgeTx == null) {
            log("No usable Forge TX characteristic on ${watch.address}, the face can't be detected")
            storeFaceDetected(false)
            return
        }
        queue.enqueue(
            GattWriteQueue.QueuedWrite(
                key = KEY_FORGE_NOTIFY,
                bytes = { BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE },
                onResult = { result ->
                    if (result == GattWriteQueue.Result.FAILED) {
                        log("Could not turn on Forge notifications on ${watch.address}")
                    } else {
                        handshake.onNotificationsEnabled()
                        enqueueQueryIfDue()
                    }
                }
            )
        )
    }

    private fun enqueueQueryIfDue() {
        if (!handshake.queryDue()) return
        handshake.onQueryQueued()
        queue.enqueue(
            GattWriteQueue.QueuedWrite(
                key = KEY_FORGE_QUERY,
                bytes = { ForgeProtocol.toBytes(ForgeProtocol.QUERY_FRAME) },
                onResult = { result ->
                    if (result == GattWriteQueue.Result.FAILED) {
                        handshake.onQueryFailed()
                    } else if (handshake.onQuerySent()) {
                        handler.removeCallbacks(handshakeTimeout)
                        handler.postDelayed(handshakeTimeout, ForgeHandshake.REPLY_TIMEOUT_MS)
                    }
                }
            )
        )
    }

    private fun handleNotification(uuid: UUID, data: ByteArray) {
        if (torndown || !isConnected || uuid != ForgeProtocol.TX_UUID) return
        val result = handshake.onNotification(data)
        if (result == null) {
            log("Ignored Forge notification from ${watch.address} (${data.size} bytes)")
            return
        }
        handler.removeCallbacks(handshakeTimeout)
        onHandshakeResult(result)
    }

    private fun onHandshakeResult(result: ForgeHandshake.Result?) {
        when (result) {
            ForgeHandshake.Result.DETECTED -> {
                log("Glycemia face v${handshake.faceVersion} answered on ${watch.address}")
                storeFaceDetected(true)
            }
            ForgeHandshake.Result.NOT_DETECTED -> {
                log("No glycemia face answered on ${watch.address}")
                storeFaceDetected(false)
            }
            null -> Unit
        }
    }

    private fun storeFaceDetected(detected: Boolean) {
        if (watch.faceDetected == detected) return
        WatchStore.setFaceDetected(context, watch.address, detected)
        watch = watch.copy(faceDetected = detected)
        onStateChanged(this, state) // republish so the UI can update the watchface icon
    }

    // ========================
    // SENDING
    // ========================

    private fun flush() {
        if (!isConnected || !servicesReady) return

        val payload = pendingOap
        if (payload != null) {
            if (payload.text == lastSent) {
                pendingOap = null
                state = WatchConnState.SYNCED
                markLabelSentIfInactive()
            } else {
                queue.enqueue(
                    GattWriteQueue.QueuedWrite(
                        key = KEY_OAP,
                        bytes = { OapProtocol.screensaverPacket(payload.text) },
                        onResult = { result -> onOapResult(payload, result) }
                    )
                )
            }
        }

        if (forgeRx != null) enqueueForgeIfDue()
        afterQueueDrained()
    }

    private fun enqueueForgeIfDue() {
        if (!forgeFeed.due(System.currentTimeMillis())) return
        queue.enqueue(
            GattWriteQueue.QueuedWrite(
                key = KEY_FORGE,
                bytes = {
                    forgeFeed.nextFrame(System.currentTimeMillis())?.let(ForgeProtocol::toBytes)
                },
                onResult = { result ->
                    if (result == GattWriteQueue.Result.FAILED) {
                        forgeFeed.reset() // retried on the next tick
                    } else {
                        recordSync()
                    }
                }
            )
        )
        // While the face is not detected, every frame (reading or keepalive) asks again
        handshake.requestQuery()
        enqueueQueryIfDue()
    }

    private fun onOapResult(payload: WatchPayload, result: GattWriteQueue.Result) {
        if (result == GattWriteQueue.Result.FAILED) {
            log("Write to ${watch.address} failed, keeping '${payload.text}' pending")
            return
        }
        lastSent = payload.text
        if (pendingOap === payload) pendingOap = null
        lastSentValue = payload.text
        recordSync()
        log("Sent to ${watch.address} -> '${payload.text}'")
        state = WatchConnState.SYNCED
        markLabelSentIfInactive()
    }

    private fun recordSync() {
        lastSyncTimeMs = System.currentTimeMillis()
        WatchStore.updateLastSyncTime(context, watch.address, lastSyncTimeMs)
        watch = watch.copy(lastSuccessfulSyncTimeMs = lastSyncTimeMs)
    }

    private fun markLabelSentIfInactive() {
        if (watch.activityState != WatchActivityState.ACTIVE && !watch.activityLabelSent) {
            WatchStore.markActivityLabelSent(context, watch.address)
            watch = watch.copy(activityLabelSent = true)
        }
    }

    /** A paused/stopped watch only connects to deliver its label: hang up once everything is out. */
    private fun afterQueueDrained() {
        if (isConnected && queue.isIdle &&
            watch.activityState != WatchActivityState.ACTIVE && watch.activityLabelSent
        ) {
            disconnectSoftNow()
        }
    }

    private fun issueWrite(write: GattWriteQueue.QueuedWrite, data: ByteArray): Boolean {
        val g = gatt ?: return false
        if (write.key == KEY_FORGE_NOTIFY) return writeNotifyDescriptor(g, data)
        val (characteristic, writeType) = when (write.key) {
            KEY_OAP -> {
                val c = g.getService(OapProtocol.SERVICE_UUID)?.getCharacteristic(OapProtocol.CHAR_UUID)
                    ?: return false
                c to BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }
            KEY_FORGE, KEY_FORGE_QUERY -> {
                val c = forgeRx ?: return false
                c to forgeWriteType(c)
            }
            else -> return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeCharacteristic(characteristic, data, writeType) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                run {
                    characteristic.value = data
                    characteristic.writeType = writeType
                    g.writeCharacteristic(characteristic)
                }
            }
        } catch (e: SecurityException) {
            log("Missing permission to write to ${watch.address}: ${e.message}")
            false
        }
    }

    /** Subscribes to the Forge TX characteristic: local notification flag plus the CCCD write. */
    private fun writeNotifyDescriptor(g: BluetoothGatt, value: ByteArray): Boolean {
        val tx = forgeTx ?: return false
        val cccd = tx.getDescriptor(ForgeProtocol.CCCD_UUID) ?: return false
        return try {
            if (!g.setCharacteristicNotification(tx, true)) {
                false
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeDescriptor(cccd, value) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                run {
                    cccd.value = value
                    g.writeDescriptor(cccd)
                }
            }
        } catch (e: SecurityException) {
            log("Missing permission to subscribe to ${watch.address}: ${e.message}")
            false
        }
    }

    private fun forgeWriteType(c: BluetoothGattCharacteristic): Int =
        if (c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }

    private fun log(msg: String) {
        Log.d("WatchConnection", msg)
    }
}
