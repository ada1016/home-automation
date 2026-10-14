package io.github.tclremotemapper

import android.content.Context

object MappingPreferences {
    private const val PREFS = "mapping"
    private const val ENABLED = "enabled"
    private const val INPUT_ID = "input_id"
    private const val MEDIA_ENABLED = "media_enabled"
    private const val MEDIA_PACKAGE = "media_package"

    fun enabled(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, enabled).apply()
    }

    fun inputId(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(INPUT_ID, null)

    fun setInputId(context: Context, inputId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(INPUT_ID, inputId).apply()
    }

    /**
     * This mapping is deliberately independent of Prime -> input.  It defaults
     * on only for the diagnostic build that first proved the user's IR Media
     * key reaches Accessibility; the toggle in the TV UI can turn it off.
     */
    fun mediaEnabled(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(MEDIA_ENABLED, true)

    fun setMediaEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(MEDIA_ENABLED, enabled).apply()
    }

    fun mediaPackage(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(MEDIA_PACKAGE, DEFAULT_MEDIA_PACKAGE)

    fun setMediaPackage(context: Context, packageName: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(MEDIA_PACKAGE, packageName).apply()
    }

    const val DEFAULT_MEDIA_PACKAGE = "com.spotify.tv.android"
}
