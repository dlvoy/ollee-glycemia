package pl.cukrzycowy.ollee.glycemia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class XdripReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        logIntent(intent)
        val reading = XdripProvider().parseIntent(intent)
        if (reading != null) {
            XdripProvider.dispatch(reading)
        } else {
            Log.d("XDRIP", "ACTION ignored: ${intent.action}")
        }
    }

    private fun logIntent(intent: Intent) {
        val sb = StringBuilder()
        sb.append("\n==================================================\n")
        sb.append("📥 RECEIVED INTENT\n")
        sb.append("Action:     ${intent.action}\n")
        sb.append("Component:  ${intent.component?.flattenToString()}\n")
        sb.append("Package:    ${intent.`package`}\n")
        sb.append("Data:       ${intent.dataString}\n")
        sb.append("Type:       ${intent.type}\n")
        sb.append("Flags:      0x${Integer.toHexString(intent.flags)}\n")
        intent.categories?.let {
            sb.append("Categories: ${it.joinToString(", ")}\n")
        }
        val extras = intent.extras
        if (extras != null) {
            sb.append("Extras keys: ${extras.keySet().joinToString(", ")}\n")
        } else {
            sb.append("Extras:     null\n")
        }
        sb.append("==================================================")
        Log.d("XDRIP", sb.toString())
    }
}