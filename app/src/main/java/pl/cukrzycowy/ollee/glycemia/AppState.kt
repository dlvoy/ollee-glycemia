package pl.cukrzycowy.ollee.glycemia

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-process shared state published by [BleService] and consumed by the UI.
 * A same-process singleton StateFlow is simpler than broadcasts here since
 * it carries a `List<WatchStatus>` natively; the legacy `BLE_STATUS`/
 * `BG_UPDATED`/`GLYCEMIA_HISTORY_UPDATED` broadcasts keep working in
 * parallel until the UI migration (Phase 2) is complete.
 */
object AppState {
    private val _watchStatuses = MutableStateFlow<List<WatchStatus>>(emptyList())
    val watchStatuses: StateFlow<List<WatchStatus>> = _watchStatuses.asStateFlow()

    fun publishWatchStatuses(statuses: List<WatchStatus>) {
        _watchStatuses.value = statuses
    }
}
