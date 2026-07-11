package pl.cukrzycowy.ollee.glycemia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NightAutoPauseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        if (intent?.action != "pl.cukrzycowy.ollee.glycemia.CHECK_AUTO_PAUSE") return

        Log.d("NightAutoPauseReceiver", "Auto-pause check triggered")
        NightAutoPauseScheduler.checkAndApply(context)
    }
}
