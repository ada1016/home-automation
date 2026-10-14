package io.github.tclremotemapper

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var inputs: TextView
    private lateinit var log: TextView
    private lateinit var mappingEnabled: SwitchCompat
    private lateinit var mappingStatus: TextView
    private lateinit var mediaMappingEnabled: SwitchCompat
    private lateinit var mediaMappingStatus: TextView

    private val updates = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshLog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        inputs = findViewById(R.id.inputs)
        log = findViewById(R.id.log)
        mappingEnabled = findViewById(R.id.mapping_enabled)
        mappingStatus = findViewById(R.id.mapping_status)
        mediaMappingEnabled = findViewById(R.id.media_mapping_enabled)
        mediaMappingStatus = findViewById(R.id.media_mapping_status)
        findViewById<Button>(R.id.open_accessibility).setOnClickListener {
            openAccessibilitySettings()
        }
        findViewById<Button>(R.id.refresh).setOnClickListener { refresh() }
        findViewById<Button>(R.id.clear).setOnClickListener { DiagnosticLog.clear(this); refreshLog() }
        findViewById<Button>(R.id.select_input).setOnClickListener { chooseInput() }
        findViewById<Button>(R.id.select_media_app).setOnClickListener { chooseMediaApp() }
        mappingEnabled.setOnCheckedChangeListener { _, checked ->
            MappingPreferences.setEnabled(this, checked)
            updateMappingStatus()
        }
        mediaMappingEnabled.setOnCheckedChangeListener { _, checked ->
            MappingPreferences.setMediaEnabled(this, checked)
            updateMappingStatus()
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(updates, IntentFilter(KeyEvents.ACTION_UPDATED), Context.RECEIVER_NOT_EXPORTED)
        refresh()
    }

    override fun onStop() {
        unregisterReceiver(updates)
        super.onStop()
    }

    private fun refresh() {
        val enabled = isServiceEnabled()
        status.text = if (enabled) {
            "Status: accessibility service ENABLED. Android may still withhold system-reserved or app-launch keys."
        } else {
            "Status: accessibility service DISABLED. Select Enable diagnostics, then enable TCL Remote Diagnostics."
        }
        inputs.text = enumerateInputs()
        mappingEnabled.setOnCheckedChangeListener(null)
        mappingEnabled.isChecked = MappingPreferences.enabled(this)
        mappingEnabled.setOnCheckedChangeListener { _, checked ->
            MappingPreferences.setEnabled(this, checked)
            updateMappingStatus()
        }
        mediaMappingEnabled.setOnCheckedChangeListener(null)
        mediaMappingEnabled.isChecked = MappingPreferences.mediaEnabled(this)
        mediaMappingEnabled.setOnCheckedChangeListener { _, checked ->
            MappingPreferences.setMediaEnabled(this, checked)
            updateMappingStatus()
        }
        updateMappingStatus()
        refreshLog()
    }

    private fun refreshLog() { log.text = DiagnosticLog.read(this) }

    private fun updateMappingStatus() {
        val selected = currentInput()?.let { "${it.loadLabel(this)} (${it.id})" } ?: "no input selected"
        val enabled = if (MappingPreferences.enabled(this)) "ON" else "OFF"
        mappingStatus.text = "Prime mapping: $enabled · Destination: $selected"
        val mediaEnabled = if (MappingPreferences.mediaEnabled(this)) "ON" else "OFF"
        val app = mediaAppLabel()
        mediaMappingStatus.text = "Media mapping: $mediaEnabled · Destination: $app"
    }

    private fun chooseInput() {
        val manager = getSystemService(TV_INPUT_SERVICE) as? TvInputManager ?: return
        val candidates = manager.tvInputList.filter { it.isPassthroughInput }
        if (candidates.isEmpty()) {
            DiagnosticLog.append(this, "No passthrough inputs visible for selection.")
            refreshLog()
            return
        }
        val labels = candidates.map { "${it.loadLabel(this)}\n${it.id}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select destination input")
            .setItems(labels) { _, which ->
                MappingPreferences.setInputId(this, candidates[which].id)
                updateMappingStatus()
                DiagnosticLog.append(this, "Selected destination inputId=${candidates[which].id}")
            }
            .show()
    }

    private fun chooseMediaApp() {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val apps = packageManager.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        if (apps.isEmpty()) {
            DiagnosticLog.append(this, "No TV launcher apps are visible for Media mapping.")
            refreshLog()
            return
        }
        val labels = apps.map { "${it.loadLabel(packageManager)}\n${it.activityInfo.packageName}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Media destination app")
            .setItems(labels) { _, which ->
                val selected = apps[which].activityInfo.packageName
                MappingPreferences.setMediaPackage(this, selected)
                DiagnosticLog.append(this, "Selected Media destination app=$selected")
                updateMappingStatus()
            }
            .show()
    }

    private fun mediaAppLabel(): String {
        val packageName = MappingPreferences.mediaPackage(this) ?: return "no app selected"
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            "${packageManager.getApplicationLabel(info)} ($packageName)"
        } catch (_: Exception) {
            "$packageName (not installed)"
        }
    }

    private fun currentInput(): TvInputInfo? {
        val id = MappingPreferences.inputId(this) ?: return null
        val manager = getSystemService(TV_INPUT_SERVICE) as? TvInputManager ?: return null
        return manager.tvInputList.firstOrNull { it.id == id }
    }

    private fun openAccessibilitySettings() {
        // This Android 11 TCL Google TV resolves the generic Settings action to a
        // framework stub. Its OEM TV action opens the real settings activity.
        val tclTvAction = Intent("android.settings.ACCESSIBILITY_TV_OEM_LINK")
        try {
            startActivity(tclTvAction)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun isServiceEnabled(): Boolean {
        val expected = ComponentName(this, RemoteKeyAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun enumerateInputs(): String {
        val manager = getSystemService(TV_INPUT_SERVICE) as? TvInputManager
            ?: return "TV inputs: TvInputManager unavailable on this device."
        return try {
            val rows = manager.tvInputList.sortedBy { it.loadLabel(this).toString() }.map { input ->
                val state = when (manager.getInputState(input.id)) {
                    TvInputManager.INPUT_STATE_CONNECTED -> "connected"
                    TvInputManager.INPUT_STATE_CONNECTED_STANDBY -> "standby"
                    TvInputManager.INPUT_STATE_DISCONNECTED -> "disconnected"
                    else -> "unknown"
                }
                val type = if (input.isPassthroughInput) "passthrough" else inputType(input.type)
                "• ${input.loadLabel(this)}\n  id=${input.id}\n  type=$type, passthrough=${input.isPassthroughInput}, state=$state"
            }
            if (rows.isEmpty()) "TV inputs: none visible to this ordinary app." else "TV inputs visible to this app:\n${rows.joinToString("\n")}" 
        } catch (e: SecurityException) {
            "TV inputs: access denied: ${e.message}"
        }
    }

    private fun inputType(type: Int) = when (type) {
        TvInputInfo.TYPE_TUNER -> "tuner"
        else -> "other($type)"
    }
}
