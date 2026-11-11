package pl.cukrzycowy.ollee.glycemia.ui.screens

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WatchOff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import pl.cukrzycowy.ollee.glycemia.R
import pl.cukrzycowy.ollee.glycemia.WatchActivityState
import pl.cukrzycowy.ollee.glycemia.WatchConnState
import pl.cukrzycowy.ollee.glycemia.WatchStatus
import pl.cukrzycowy.ollee.glycemia.ui.theme.OlleeColors

internal val WatchStatusGreen = Color(0xFF00AA00)
internal val WatchStatusBlue = Color(0xFF0099CC)
internal val WatchStatusRed = Color(0xFFCC0000)
internal val WatchStatusGray = Color(0xFF999999)

/** The watchface icon of a row; absent when the watch has no Forge service. */
data class WatchFaceBadge(
    val icon: ImageVector,
    val tint: Color,
    @StringRes val descriptionRes: Int
)

/** How a watch row looks: label, status icon, colors and the optional watchface badge. */
data class WatchStatusStyle(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val color: Color,
    val pillBg: Color,
    val faceBadge: WatchFaceBadge?
)

/** The watch reports synced, but what it got is the "no data" or "error" tag. */
val WatchStatus.isStaleData: Boolean
    get() = state == WatchConnState.SYNCED && (lastSentValue == "--- --" || lastSentValue == "Err   ")

/**
 * The label (and its icon) and the color come from separate rules that disagree on purpose,
 * e.g. a SYNCED watch that is offline by timeout reads "Synced" in gray. Keep their order.
 */
fun watchStatusStyle(status: WatchStatus): WatchStatusStyle {
    val activity = status.watch.activityState
    val labelRes: Int
    val icon: ImageVector
    val color: Color
    val pillBg: Color

    if (activity != WatchActivityState.ACTIVE) {
        if (activity == WatchActivityState.STOPPED) {
            labelRes = R.string.watch_state_stopped
            icon = Icons.Filled.StopCircle
        } else {
            labelRes = R.string.watch_state_paused
            icon = Icons.Filled.PauseCircle
        }
        color = WatchStatusGray
        pillBg = OlleeColors.SurfaceDisabled
    } else {
        color = when {
            status.state == WatchConnState.ERROR -> WatchStatusRed
            status.isStaleData -> WatchStatusGray
            status.isOfflineByTimeout -> WatchStatusGray
            status.state == WatchConnState.SYNCED -> WatchStatusGreen
            status.state == WatchConnState.CONNECTING -> WatchStatusBlue
            else -> WatchStatusGray
        }
        when {
            status.state == WatchConnState.CONNECTING -> {
                labelRes = R.string.watch_state_connecting
                icon = Icons.Filled.Sync
            }
            status.state == WatchConnState.SYNCED -> {
                labelRes = R.string.watch_state_synced
                icon = Icons.Filled.CheckCircle
            }
            status.state == WatchConnState.ERROR -> {
                labelRes = R.string.watch_state_error
                icon = Icons.Filled.Error
            }
            else -> {
                labelRes = R.string.watch_state_offline
                icon = Icons.Filled.LinkOff
            }
        }
        pillBg = OlleeColors.SurfaceCard
    }

    val faceBadge = when {
        !status.watch.forgeSupported -> null
        status.watch.faceDetected -> WatchFaceBadge(Icons.Filled.Watch, color, R.string.watch_face_detected)
        else -> WatchFaceBadge(Icons.Filled.WatchOff, WatchStatusGray, R.string.watch_face_not_detected)
    }

    return WatchStatusStyle(labelRes, icon, color, pillBg, faceBadge)
}
