package pl.cukrzycowy.ollee.glycemia

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Manages the BLE link to every paired watch (see [WatchStore]). Each watch
 * gets its own [WatchConnection] so one being out of range never blocks
 * delivery to the others; every glycemia reading is fanned out to all
 * currently-known connections.
 */
class BleService : Service() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var prefs: SharedPreferences

    private val connections = mutableMapOf<String, WatchConnection>()
    private var currentProvider: GlycemiaProvider? = null
    private var isInErrorState = false
    private var lastKnownReadingTimestamp: Long = 0L

    companion object {
        const val CHANNEL_ID = "ble_service_channel"
        private const val TIMEOUT_MS = 15 * 60 * 1000L
        const val ACTION_SWITCH_PROVIDER = "pl.cukrzycowy.ollee.glycemia.SWITCH_PROVIDER"
        const val ACTION_SYNC_WATCHES = "pl.cukrzycowy.ollee.glycemia.SYNC_WATCHES"
        const val ACTION_SEND_CLEAR_GLYCEMIA = "pl.cukrzycowy.ollee.glycemia.SEND_CLEAR_GLYCEMIA"
        const val ACTION_RESEND_GLYCEMIA = "pl.cukrzycowy.ollee.glycemia.RESEND_GLYCEMIA"
        const val ACTION_MANUAL_SYNC_WATCH = "pl.cukrzycowy.ollee.glycemia.MANUAL_SYNC_WATCH"
        const val ACTION_SET_WATCH_ACTIVITY = "pl.cukrzycowy.ollee.glycemia.SET_WATCH_ACTIVITY"
        const val EXTRA_WATCH_ADDRESS = "watch_address"
        const val EXTRA_ACTIVITY_STATE = "activity_state"
    }

    // ========================
    // LIFECYCLE
    // ========================

    override fun onCreate() {
        super.onCreate()

        prefs = getSharedPreferences("data", MODE_PRIVATE)
        notificationManager = getSystemService(NotificationManager::class.java)
        lastKnownReadingTimestamp = prefs.getLong("last_time", 0L)

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val foregroundServiceType = if (BlePermissionHelper.canStartConnectedDeviceForegroundService(this)) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            startForeground(1, createNotification(getString(R.string.notification_initializing)), foregroundServiceType)
        } else {
            startForeground(1, createNotification(getString(R.string.notification_initializing)))
        }

        registerReceiver(btReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        log("Service created")

        currentProvider = GlycemiaProviderManager.getSelected(this).also {
            it.start(this, ::onGlycemiaReading)
        }

        syncConnectionsWithStore()
        startTimeoutWatcher()

        if (NightAutoPauseStore.isEnabled(this)) {
            NightAutoPauseScheduler.scheduleNextCheck(this)
        }
    }

    override fun onDestroy() {
        currentProvider?.stop(this)
        connections.values.forEach { it.teardown() }
        connections.clear()
        super.onDestroy()
        unregisterReceiver(btReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SWITCH_PROVIDER -> {
                currentProvider?.stop(this)
                lastKnownReadingTimestamp = 0L
                currentProvider = GlycemiaProviderManager.getSelected(this).also {
                    it.start(this, ::onGlycemiaReading)
                }
                return START_STICKY
            }

            ACTION_SYNC_WATCHES -> {
                syncConnectionsWithStore()
                return START_STICKY
            }

            ACTION_SEND_CLEAR_GLYCEMIA -> {
                sendClearGlycemiaToAllWatches()
                return START_STICKY
            }

            ACTION_RESEND_GLYCEMIA -> {
                resendGlycemiaToAllWatches()
                return START_STICKY
            }

            ACTION_MANUAL_SYNC_WATCH -> {
                val watchAddress = intent?.getStringExtra(EXTRA_WATCH_ADDRESS)
                if (watchAddress != null) {
                    manualSyncWatch(watchAddress)
                }
                return START_STICKY
            }

            ACTION_SET_WATCH_ACTIVITY -> {
                val watchAddress = intent?.getStringExtra(EXTRA_WATCH_ADDRESS)
                val stateName = intent?.getStringExtra(EXTRA_ACTIVITY_STATE)
                if (watchAddress != null && stateName != null) {
                    try {
                        val newState = WatchActivityState.valueOf(stateName)
                        setWatchActivity(watchAddress, newState)
                    } catch (e: Exception) {
                        log("Invalid activity state: $stateName")
                    }
                }
                return START_STICKY
            }
        }

        // Back-compat: a caller may still pass a bare device address (legacy
        // single-device flow) - fold it into the watch list instead.
        intent?.getStringExtra("device_address")?.let { address ->
            WatchStore.add(this, address)
            syncConnectionsWithStore()
        }

        return START_STICKY
    }

    // ========================
    // WATCH CONNECTION MANAGEMENT
    // ========================

    private fun syncConnectionsWithStore() {
        val paired = WatchStore.getAll(this)
        val pairedAddresses = paired.map { it.address }.toSet()

        connections.keys.filterNot { it in pairedAddresses }.forEach { address ->
            connections.remove(address)?.teardown()
        }

        val lastSent = prefs.getString("last_sent", null)?.takeIf { it.isNotBlank() }

        paired.forEachIndexed { index, watch ->
            val existing = connections[watch.address]
            if (existing != null) {
                existing.updateWatch(watch)
            } else {
                val connection = WatchConnection(this, watch, ::onConnectionStateChanged)
                connections[watch.address] = connection
                connection.connect(staggerIndex = index)
                val toSubmit = when {
                    watch.activityState == WatchActivityState.ACTIVE -> lastSent
                    !watch.activityLabelSent -> labelFor(watch.activityState)
                    else -> null
                }
                toSubmit?.let { connection.submitReading(it) }
            }
        }

        publishStatuses()
        updateNotification()
    }

    private fun onConnectionStateChanged(connection: WatchConnection, state: WatchConnState) {
        publishStatuses()
        updateNotification()
    }

    private fun publishStatuses() {
        val now = System.currentTimeMillis()
        AppState.publishWatchStatuses(connections.values.map { connection ->
            val isValidDataFromProvider = connection.lastSentValue.isNotEmpty() &&
                connection.lastSentValue != "--- --" && connection.lastSentValue != "Err   "
            val isOfflineByTimeout = connection.watch.activityState == WatchActivityState.ACTIVE && isValidDataFromProvider && (
                connection.watch.lastSuccessfulSyncTimeMs == 0L ||
                (now - connection.watch.lastSuccessfulSyncTimeMs) > (30 * 60 * 1000) // 30 minutes
            )
            WatchStatus(
                watch = connection.watch,
                state = connection.state,
                lastSyncTimeMs = connection.lastSyncTimeMs,
                lastConnectionAttemptTimeMs = connection.lastConnectionAttemptTimeMs,
                lastSentValue = connection.lastSentValue,
                isOfflineByTimeout = isOfflineByTimeout
            )
        })
    }

    // ========================
    // BG MANAGEMENT
    // ========================

    private fun onGlycemiaReading(reading: GlycemiaReading) {
        val isNewerReading = reading.timestamp > lastKnownReadingTimestamp

        reading.bg.toIntOrNull()?.let { valueMgDl ->
            GlycemiaHistoryStore.append(
                context = this,
                entry = GlycemiaHistoryEntry(
                    timestampMs = reading.timestamp,
                    valueMgDl = valueMgDl,
                    delta = reading.delta ?: 0.0
                )
            )
            sendBroadcast(Intent("GLYCEMIA_HISTORY_UPDATED"))
        }

        if (isNewerReading) {
            lastKnownReadingTimestamp = reading.timestamp
            prefs.edit()
                .putString("last_bg", reading.bg)
                .putFloat("last_delta", reading.delta?.toFloat() ?: Float.NaN)
                .putLong("last_time", reading.timestamp)
                .apply()

            handleBg(reading.bg, reading.trend, reading.delta)
            sendBroadcast(Intent("BG_UPDATED"))
        } else {
            log("Ignoring archival SGV reading (timestamp: ${reading.timestamp}, current: $lastKnownReadingTimestamp)")
        }

        sendBroadcast(Intent("GLYCEMIA_UPDATED"))
    }

    private fun handleBg(bg: String, trend: String?, delta: Double? = null) {
        val formatted = formatBg(bg, trend, delta)

        isInErrorState = false

        prefs.edit()
            .putString("last_sent", formatted)
            .apply()

        val activeWatches = connections.values.filter { it.watch.activityState == WatchActivityState.ACTIVE }

        activeWatches.forEach {
            if (it.state == WatchConnState.OFFLINE) {
                it.connect()
            }
        }

        activeWatches.forEach { it.submitReading(formatted) }
        publishStatuses()
        sendBroadcast(Intent("GLYCEMIA_UPDATED"))
    }

    private fun formatBg(bg: String?, trend: String?, delta: Double? = null): String {

        if (bg.isNullOrBlank()) return "Err   "

        val clean = bg.replace(",", ".")

        val isMmol = clean.contains(".")

        // ========================
        // mg/dL: 3 chars glucose + 3 chars delta (left-padded), no trend arrow
        // ========================
        if (!isMmol) {
            val mgdl = clean.replace("[^0-9]".toRegex(), "")
                .toIntOrNull() ?: return "Err   "
            val clampedMgdl = mgdl.coerceIn(0, 999)

            val glucoseStr = clampedMgdl.toString().padStart(3, ' ')

            val deltaStr = if (delta != null) {
                val deltaInt = Math.round(delta).toInt().coerceIn(-99, 99)
                deltaInt.toString().padStart(3, ' ')
            } else {
                "   "
            }

            return glucoseStr + deltaStr
        }

        // ========================
        // mmol/L: legacy 6-char format (1 trend arrow + 5 value chars)
        // ========================
        val mmol = clean.toFloatOrNull() ?: return "Err   "
        val valueStr = String.format("%.1f", mmol.coerceIn(0f, 99.9f))

        val arrow = when (trend) {
            "UP" -> "+"
            "DOWN" -> "-"
            "FLAT" -> " "
            else -> " "
        }

        val valueAligned = valueStr.take(5).padStart(5, ' ')
        return (arrow + valueAligned).take(6)
    }

    private fun sendClearGlycemiaToAllWatches() {
        val clearValue = "--- --"
        connections.values.filter { it.watch.activityState == WatchActivityState.ACTIVE }.forEach { it.submitReading(clearValue) }
        publishStatuses()
    }

    private fun resendGlycemiaToAllWatches() {
        val lastSent = prefs.getString("last_sent", null)?.takeIf { it.isNotBlank() }
        if (lastSent != null) {
            connections.values.filter { it.watch.activityState == WatchActivityState.ACTIVE }.forEach { it.submitReading(lastSent) }
            publishStatuses()
        }
    }

    private fun manualSyncWatch(watchAddress: String) {
        val connection = connections[watchAddress] ?: return
        val lastSent = prefs.getString("last_sent", null)?.takeIf { it.isNotBlank() } ?: return

        // Update last successful sync time to now to prevent immediate offline timeout
        val now = System.currentTimeMillis()
        WatchStore.updateLastSyncTime(this, watchAddress, now)
        connection.updateWatch(connection.watch.copy(lastSuccessfulSyncTimeMs = now))

        connection.connect()
        connection.submitReading(lastSent)
        publishStatuses()
    }

    // ========================
    // TIMEOUT
    // ========================

    private fun startTimeoutWatcher() {

        val handler = Handler(Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {

                val lastTime = prefs.getLong("last_time", 0L)
                val now = System.currentTimeMillis()

                if (now - lastTime > TIMEOUT_MS) {

                    if (!isInErrorState) {
                        log("Timeout -> ERROR")

                        isInErrorState = true

                        prefs.edit().putString("last_sent", "Err   ").apply()
                        connections.values.filter { it.watch.activityState == WatchActivityState.ACTIVE }.forEach { it.submitReading("Err   ") }
                        publishStatuses()
                    }
                }

                handler.postDelayed(this, 60_000)
            }
        }

        handler.post(runnable)
    }

    // ========================
    // BLUETOOTH ADAPTER STATE
    // ========================

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {

                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {

                    BluetoothAdapter.STATE_OFF -> {
                        log("Bluetooth OFF")
                        connections.values.forEach { it.disconnectSoft() }
                        publishStatuses()
                        updateNotification()
                    }

                    BluetoothAdapter.STATE_ON -> {
                        log("Bluetooth ON -> reconnecting")
                        connections.values.forEachIndexed { index, connection ->
                            connection.connect(staggerIndex = index)
                        }
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ========================
    // NOTIFICATION
    // ========================

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.ble_service_name),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotification() {
        val activeConnections = connections.values.filter { it.watch.activityState == WatchActivityState.ACTIVE }
        val text = when {
            connections.isEmpty() -> getString(R.string.notification_no_watches)
            activeConnections.isEmpty() -> getString(R.string.notification_all_watches_inactive)
            else -> {
                val synced = activeConnections.count { it.state == WatchConnState.SYNCED }
                getString(R.string.notification_watch_status_format, synced, activeConnections.size)
            }
        }
        notificationManager.notify(1, createNotification(text))
    }

    private fun labelFor(state: WatchActivityState): String {
        val label = when (state) {
            WatchActivityState.PAUSED -> WatchActivityLabelStore.getPauseLabel(this)
            WatchActivityState.STOPPED -> WatchActivityLabelStore.getStopLabel(this)
            WatchActivityState.ACTIVE -> ""
        }
        return label.take(6).padEnd(6, ' ')
    }

    private fun setWatchActivity(watchAddress: String, newState: WatchActivityState) {
        WatchStore.setActivityState(this, watchAddress, newState)
        syncConnectionsWithStore()
        connections[watchAddress]?.let { conn ->
            conn.connect()
            val toSubmit = if (newState == WatchActivityState.ACTIVE) {
                prefs.getString("last_sent", null)?.takeIf { it.isNotBlank() }
            } else {
                labelFor(newState)
            }
            toSubmit?.let { conn.submitReading(it) }
        }
        publishStatuses()
    }

    private fun log(msg: String) {
        Log.d("BleService", msg)
    }
}
