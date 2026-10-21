package io.github.tclremotemapper

import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticLog {
    private const val PREFS = "diagnostics"
    private const val KEY = "events"
    private const val KEY_EVENT_LOGGING = "key_event_logging"
    private const val MAX_EVENTS = 250

    fun append(context: Context, entry: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stamped = "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}  $entry"
        val events = (prefs.getString(KEY, "") ?: "").lineSequence()
            .filter { it.isNotBlank() }
            .toMutableList()
        events.add(0, stamped)
        prefs.edit().putString(KEY, events.take(MAX_EVENTS).joinToString("\n")).apply()
        context.sendBroadcast(Intent(KeyEvents.ACTION_UPDATED).setPackage(context.packageName))
    }

    /** Raw key events are deliberately opt-in: persisting every D-pad event adds avoidable I/O. */
    fun isKeyEventLoggingEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_EVENT_LOGGING, false)

    fun setKeyEventLoggingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EVENT_LOGGING, enabled)
            .apply()
        append(context, "Raw key-event logging ${if (enabled) "enabled" else "disabled"}.")
    }

    fun appendKeyEvent(context: Context, entry: String) {
        if (isKeyEventLoggingEnabled(context)) append(context, entry)
    }

    fun read(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY, "No events recorded yet.") ?: "No events recorded yet."

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
        context.sendBroadcast(Intent(KeyEvents.ACTION_UPDATED).setPackage(context.packageName))
    }
}

object KeyEvents {
    const val ACTION_UPDATED = "io.github.tclremotemapper.KEY_EVENTS_UPDATED"
}
