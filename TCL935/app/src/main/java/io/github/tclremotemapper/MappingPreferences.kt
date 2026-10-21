package io.github.tclremotemapper

import android.content.Context
import android.view.KeyEvent

/** Persistent per-button configuration and one-time migration of the v0.3 settings. */
object MappingPreferences {
    private const val PREFS = "mapping"
    private const val MIGRATED = "six_button_migration_complete"
    private const val LEGACY_PRIME_ENABLED = "enabled"
    private const val LEGACY_INPUT_ID = "input_id"
    private const val LEGACY_MEDIA_ENABLED = "media_enabled"
    private const val LEGACY_MEDIA_PACKAGE = "media_package"
    private const val DEFAULT_MEDIA_PACKAGE = "com.spotify.tv.android"

    fun ensureMigrated(context: Context) {
        val prefs = prefs(context)
        if (prefs.getBoolean(MIGRATED, false)) return

        val edit = prefs.edit()
        if (prefs.contains(LEGACY_PRIME_ENABLED) || prefs.contains(LEGACY_INPUT_ID)) {
            putKey(edit, RemoteButton.PRIME, RemoteButton.PRIME.knownKeyCode, RemoteButton.PRIME.knownScanCode)
            edit.putBoolean(enabledKey(RemoteButton.PRIME), prefs.getBoolean(LEGACY_PRIME_ENABLED, false))
            prefs.getString(LEGACY_INPUT_ID, null)?.let { putDestination(edit, RemoteButton.PRIME, DestinationType.INPUT, it) }
        }
        if (prefs.contains(LEGACY_MEDIA_ENABLED) || prefs.contains(LEGACY_MEDIA_PACKAGE)) {
            putKey(edit, RemoteButton.MEDIA, RemoteButton.MEDIA.knownKeyCode, null)
            edit.putBoolean(enabledKey(RemoteButton.MEDIA), prefs.getBoolean(LEGACY_MEDIA_ENABLED, true))
            putDestination(edit, RemoteButton.MEDIA, DestinationType.APP, prefs.getString(LEGACY_MEDIA_PACKAGE, DEFAULT_MEDIA_PACKAGE) ?: DEFAULT_MEDIA_PACKAGE)
        }
        edit.putBoolean(MIGRATED, true).apply()
    }

    fun mapping(context: Context, button: RemoteButton): ButtonMapping {
        ensureMigrated(context)
        val prefs = prefs(context)
        val type = prefs.getString(destinationTypeKey(button), null)?.let { runCatching { DestinationType.valueOf(it) }.getOrNull() }
        return ButtonMapping(
            button = button,
            keyCode = if (prefs.contains(keyCodeKey(button))) prefs.getInt(keyCodeKey(button), 0) else button.knownKeyCode,
            scanCode = if (prefs.contains(scanCodeKey(button))) prefs.getInt(scanCodeKey(button), 0) else button.knownScanCode,
            enabled = prefs.getBoolean(enabledKey(button), false),
            destinationType = type,
            destinationValue = prefs.getString(destinationValueKey(button), null),
        )
    }

    fun allMappings(context: Context): List<ButtonMapping> = RemoteButton.entries.map { mapping(context, it) }

    fun setEnabled(context: Context, button: RemoteButton, enabled: Boolean) {
        prefs(context).edit().putBoolean(enabledKey(button), enabled).apply()
    }

    fun learn(context: Context, button: RemoteButton, event: KeyEvent) {
        prefs(context).edit().also { putKey(it, button, event.keyCode, event.scanCode) }.apply()
    }

    fun clearLearnedKey(context: Context, button: RemoteButton) {
        prefs(context).edit().remove(keyCodeKey(button)).remove(scanCodeKey(button)).apply()
    }

    fun setDestination(context: Context, button: RemoteButton, type: DestinationType, value: String) {
        prefs(context).edit().also { putDestination(it, button, type, value) }.apply()
    }

    fun clearDestination(context: Context, button: RemoteButton) {
        prefs(context).edit().remove(destinationTypeKey(button)).remove(destinationValueKey(button)).apply()
    }

    fun startLearning(context: Context, button: RemoteButton) {
        prefs(context).edit().putString("learning_button", button.id).apply()
    }

    fun learningButton(context: Context): RemoteButton? {
        val id = prefs(context).getString("learning_button", null) ?: return null
        return RemoteButton.entries.firstOrNull { it.id == id }
    }

    fun finishLearning(context: Context) {
        prefs(context).edit().remove("learning_button").apply()
    }

    fun mappingForEvent(context: Context, event: KeyEvent): ButtonMapping? = allMappings(context).firstOrNull { mapping ->
        mapping.enabled && mapping.isLearned && mapping.keyCode == event.keyCode
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun prefix(button: RemoteButton) = "button.${button.id}."
    private fun keyCodeKey(button: RemoteButton) = prefix(button) + "key_code"
    private fun scanCodeKey(button: RemoteButton) = prefix(button) + "scan_code"
    private fun enabledKey(button: RemoteButton) = prefix(button) + "enabled"
    private fun destinationTypeKey(button: RemoteButton) = prefix(button) + "destination_type"
    private fun destinationValueKey(button: RemoteButton) = prefix(button) + "destination_value"

    private fun putKey(edit: android.content.SharedPreferences.Editor, button: RemoteButton, keyCode: Int?, scanCode: Int?) {
        if (keyCode != null) edit.putInt(keyCodeKey(button), keyCode)
        if (scanCode != null) edit.putInt(scanCodeKey(button), scanCode)
    }

    private fun putDestination(edit: android.content.SharedPreferences.Editor, button: RemoteButton, type: DestinationType, value: String) {
        edit.putString(destinationTypeKey(button), type.name).putString(destinationValueKey(button), value)
    }
}
