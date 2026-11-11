package pl.cukrzycowy.ollee.glycemia.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WatchOff
import androidx.compose.ui.graphics.vector.ImageVector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import pl.cukrzycowy.ollee.glycemia.PairedWatch
import pl.cukrzycowy.ollee.glycemia.R
import pl.cukrzycowy.ollee.glycemia.WatchActivityState
import pl.cukrzycowy.ollee.glycemia.WatchConnState
import pl.cukrzycowy.ollee.glycemia.WatchStatus
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeColors

class WatchStatusStyleTest {

    private fun status(
        state: WatchConnState,
        activity: WatchActivityState = WatchActivityState.ACTIVE,
        lastSent: String = "120 -5",
        offlineByTimeout: Boolean = false,
        forge: Boolean = false,
        face: Boolean = false
    ) = WatchStatus(
        watch = PairedWatch(
            address = "AA:BB:CC:DD:EE:FF",
            name = "W",
            activityState = activity,
            forgeSupported = forge,
            faceDetected = face
        ),
        state = state,
        lastSentValue = lastSent,
        isOfflineByTimeout = offlineByTimeout
    )

    private fun assertStyle(s: WatchStatusStyle, label: Int, icon: ImageVector, color: androidx.compose.ui.graphics.Color) {
        assertEquals(label, s.labelRes)
        assertSame(icon, s.icon)
        assertEquals(color, s.color)
    }

    @Test
    fun activeStates() {
        assertStyle(watchStatusStyle(status(WatchConnState.SYNCED)), R.string.watch_state_synced, Icons.Filled.CheckCircle, WatchStatusGreen)
        assertStyle(watchStatusStyle(status(WatchConnState.CONNECTING)), R.string.watch_state_connecting, Icons.Filled.Sync, WatchStatusBlue)
        assertStyle(watchStatusStyle(status(WatchConnState.OFFLINE)), R.string.watch_state_offline, Icons.Filled.LinkOff, WatchStatusGray)
        assertStyle(watchStatusStyle(status(WatchConnState.ERROR)), R.string.watch_state_error, Icons.Filled.Error, WatchStatusRed)
        assertEquals(OlleeColors.SurfaceCard, watchStatusStyle(status(WatchConnState.SYNCED)).pillBg)
    }

    @Test
    fun syncedWithStaleData_isGrayCheck() {
        for (tag in listOf("--- --", "Err   ")) {
            assertStyle(watchStatusStyle(status(WatchConnState.SYNCED, lastSent = tag)), R.string.watch_state_synced, Icons.Filled.CheckCircle, WatchStatusGray)
        }
    }

    @Test
    fun offlineByTimeout_keepsLabelOfState_butGray() {
        assertStyle(watchStatusStyle(status(WatchConnState.SYNCED, offlineByTimeout = true)), R.string.watch_state_synced, Icons.Filled.CheckCircle, WatchStatusGray)
        assertStyle(watchStatusStyle(status(WatchConnState.CONNECTING, offlineByTimeout = true)), R.string.watch_state_connecting, Icons.Filled.Sync, WatchStatusGray)
        assertStyle(watchStatusStyle(status(WatchConnState.OFFLINE, offlineByTimeout = true)), R.string.watch_state_offline, Icons.Filled.LinkOff, WatchStatusGray)
        assertStyle(watchStatusStyle(status(WatchConnState.ERROR, offlineByTimeout = true)), R.string.watch_state_error, Icons.Filled.Error, WatchStatusRed)
    }

    @Test
    fun pausedAndStopped_overrideConnectionState() {
        val paused = watchStatusStyle(status(WatchConnState.SYNCED, activity = WatchActivityState.PAUSED))
        assertStyle(paused, R.string.watch_state_paused, Icons.Filled.PauseCircle, WatchStatusGray)
        assertEquals(OlleeColors.SurfaceDisabled, paused.pillBg)

        val stopped = watchStatusStyle(status(WatchConnState.ERROR, activity = WatchActivityState.STOPPED))
        assertStyle(stopped, R.string.watch_state_stopped, Icons.Filled.StopCircle, WatchStatusGray)
        assertEquals(OlleeColors.SurfaceDisabled, stopped.pillBg)
    }

    @Test
    fun faceBadge_hiddenWithoutForge() {
        assertNull(watchStatusStyle(status(WatchConnState.SYNCED)).faceBadge)
        assertNull(watchStatusStyle(status(WatchConnState.SYNCED, face = true)).faceBadge)
    }

    @Test
    fun faceBadge_notDetected_isGrayWatchOff() {
        val badge = watchStatusStyle(status(WatchConnState.SYNCED, forge = true)).faceBadge!!
        assertSame(Icons.Filled.WatchOff, badge.icon)
        assertEquals(WatchStatusGray, badge.tint)
        assertEquals(R.string.watch_face_not_detected, badge.descriptionRes)
    }

    @Test
    fun faceBadge_detected_takesStatusColor() {
        val synced = watchStatusStyle(status(WatchConnState.SYNCED, forge = true, face = true)).faceBadge!!
        assertSame(Icons.Filled.Watch, synced.icon)
        assertEquals(WatchStatusGreen, synced.tint)
        assertEquals(R.string.watch_face_detected, synced.descriptionRes)

        val error = watchStatusStyle(status(WatchConnState.ERROR, forge = true, face = true)).faceBadge!!
        assertEquals(WatchStatusRed, error.tint)
    }
}
