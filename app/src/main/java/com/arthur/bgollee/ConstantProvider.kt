package com.arthur.bgollee

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

class ConstantProvider : GlycemiaProvider {

    override val id: String = "constant"
    override val displayName: String = "Constant (test, 100 mg/dL)"

    private var callback: ((GlycemiaReading) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())

    private val runnable = object : Runnable {
        override fun run() {
            callback?.invoke(
                GlycemiaReading(
                bg = "100",
                trend = "FLAT",
                delta = 0.0,
                timestamp = System.currentTimeMillis()
                )
            )
            handler.postDelayed(this, 60_000L)
        }
    }

    override fun start(context: Context, onReading: (GlycemiaReading) -> Unit) {
        Log.d("ConstantProvider", "Starting constant provider")
        callback = onReading
        handler.removeCallbacksAndMessages(null)
        handler.post(runnable)
    }

    override fun stop(context: Context) {
        Log.d("ConstantProvider", "Stopping constant provider")
        handler.removeCallbacksAndMessages(null)
        callback = null
    }
}
