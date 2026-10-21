package io.github.tclremotemapper

import android.content.ContentValues
import android.content.Context
import android.content.ContentUris
import android.provider.MediaStore
import org.json.JSONObject

/** Fixed shared backup location; avoids TV file-picker compatibility problems. */
object ConfigBackup {
    const val FILE_NAME = "tcl-remote-mapper-config.json"
    const val RELATIVE_PATH = "Download/TCL_Remapp/"

    fun save(context: Context): Result<Unit> = runCatching {
        val resolver = context.contentResolver
        val uri = existingUri(context) ?: resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
            },
        ) ?: error("Android could not create the configuration file.")
        resolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
            it.write(MappingPreferences.exportConfig(context).toString(2))
        } ?: error("Android could not open the configuration file for writing.")
    }

    fun load(context: Context): Result<Unit> = runCatching {
        val uri = existingUri(context) ?: error("No saved config found at $RELATIVE_PATH$FILE_NAME")
        val source = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Android could not read the saved configuration.")
        MappingPreferences.importConfig(context, JSONObject(source)).getOrThrow()
    }

    private fun existingUri(context: Context): android.net.Uri? {
        val resolver = context.contentResolver
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            selection,
            arrayOf(FILE_NAME, RELATIVE_PATH),
            null,
        )
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return ContentUris.withAppendedId(
                        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                        cursor.getLong(0),
                    )
                }
            }
        return null
    }
}
