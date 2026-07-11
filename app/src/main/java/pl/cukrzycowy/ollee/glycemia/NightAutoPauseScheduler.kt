package pl.cukrzycowy.ollee.glycemia

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import java.time.LocalTime

object NightAutoPauseScheduler {
    private const val TAG = "NightAutoPauseScheduler"
    private const val ACTION_CHECK_AUTO_PAUSE = "pl.cukrzycowy.ollee.glycemia.CHECK_AUTO_PAUSE"

    fun scheduleNextCheck(context: Context) {
        if (!NightAutoPauseStore.isEnabled(context)) {
            cancel(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NightAutoPauseReceiver::class.java).apply {
            action = ACTION_CHECK_AUTO_PAUSE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule to check every minute using RTC_WAKEUP for wall-clock time
        val nextCheckTime = System.currentTimeMillis() + 60_000
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextCheckTime,
            pendingIntent
        )

        Log.d(TAG, "Next auto-pause check scheduled in 1 minute")
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NightAutoPauseReceiver::class.java).apply {
            action = ACTION_CHECK_AUTO_PAUSE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Auto-pause check cancelled")
    }

    fun checkAndApply(context: Context) {
        if (!NightAutoPauseStore.isEnabled(context)) {
            return
        }

        val startTime = NightAutoPauseStore.getStartTime(context)
        val endTime = NightAutoPauseStore.getEndTime(context)
        val now = LocalTime.now()

        val shouldBePaused = isInPauseWindow(now, startTime, endTime)

        Log.d(TAG, "Auto-pause check: now=$now, window=$startTime-$endTime, shouldBePaused=$shouldBePaused")

        val watches = WatchStore.getAll(context)
        val activeWatches = watches.filter { it.activityState == WatchActivityState.ACTIVE }
        val pausedWatches = watches.filter { it.activityState == WatchActivityState.PAUSED }

        if (shouldBePaused && activeWatches.isNotEmpty()) {
            Log.d(TAG, "Entering pause window - pausing ${activeWatches.size} active watches")
            activeWatches.forEach { watch ->
                val intent = Intent(context, BleService::class.java).apply {
                    action = BleService.ACTION_SET_WATCH_ACTIVITY
                    putExtra(BleService.EXTRA_WATCH_ADDRESS, watch.address)
                    putExtra(BleService.EXTRA_ACTIVITY_STATE, WatchActivityState.PAUSED.name)
                }
                context.startService(intent)
            }
        } else if (!shouldBePaused && pausedWatches.isNotEmpty()) {
            Log.d(TAG, "Exiting pause window - resuming ${pausedWatches.size} paused watches")
            pausedWatches.forEach { watch ->
                val intent = Intent(context, BleService::class.java).apply {
                    action = BleService.ACTION_SET_WATCH_ACTIVITY
                    putExtra(BleService.EXTRA_WATCH_ADDRESS, watch.address)
                    putExtra(BleService.EXTRA_ACTIVITY_STATE, WatchActivityState.ACTIVE.name)
                }
                context.startService(intent)
            }
        }

        scheduleNextCheck(context)
    }

    private fun isInPauseWindow(now: LocalTime, startTime: LocalTime, endTime: LocalTime): Boolean {
        return if (startTime < endTime) {
            // Normal case: window doesn't cross midnight (e.g., 16:00-23:00)
            now >= startTime && now < endTime
        } else {
            // Crosses midnight (e.g., 23:00-6:00)
            now >= startTime || now < endTime
        }
    }
}
