package pl.cukrzycowy.ollee.glycemia

import android.content.Context

object DisclaimerStore {

    fun hasUserAcceptedDisclaimer(context: Context): Boolean {
        val prefs = context.getSharedPreferences("disclaimer", Context.MODE_PRIVATE)
        return prefs.getBoolean("user_accepted_disclaimer", false)
    }

    fun setUserAcceptedDisclaimer(context: Context, accepted: Boolean) {
        val prefs = context.getSharedPreferences("disclaimer", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("user_accepted_disclaimer", accepted).apply()
    }
}
