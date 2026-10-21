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
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var mappingContainer: GridLayout
    private lateinit var log: TextView
    private lateinit var keyLogging: SwitchCompat

    private val updates = object : BroadcastReceiver() {
        // Key events arrive while the user is navigating this screen. Rebuilding the cards
        // here discards Android TV's focused View and sends focus back to the first control.
        // Mapping/configuration changes explicitly call refresh() themselves.
        override fun onReceive(context: Context, intent: Intent) = refreshLog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MappingPreferences.ensureMigrated(this)
        status = findViewById(R.id.status)
        mappingContainer = findViewById(R.id.mapping_container)
        log = findViewById(R.id.log)
        keyLogging = findViewById(R.id.key_logging)
        findViewById<Button>(R.id.refresh).setOnClickListener { refresh() }
        findViewById<Button>(R.id.clear).setOnClickListener { DiagnosticLog.clear(this); refreshLog() }
        keyLogging.setOnCheckedChangeListener { _, enabled ->
            DiagnosticLog.setKeyEventLoggingEnabled(this, enabled)
            updateLoggingUi()
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
        val diagnostics = if (DiagnosticLog.isKeyEventLoggingEnabled(this)) "ON" else "OFF"
        status.text = if (isServiceEnabled()) {
            "Accessibility service: ENABLED · Diagnose mode: $diagnostics"
        } else {
            "Accessibility service: DISABLED · Enable TCL Remote Mapper in TV Accessibility settings."
        }
        renderMappings()
        updateLoggingUi()
        refreshLog()
    }

    private fun renderMappings() {
        mappingContainer.removeAllViews()
        RemoteButton.entries.forEach { button ->
            val mapping = MappingPreferences.mapping(this, button)
            mappingContainer.addView(mappingCard(mapping), gridParams())
        }
    }

    private fun mappingCard(mapping: ButtonMapping): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = roundedBackground("#1B2630", "#486273")
        }
        card.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(this@MainActivity).apply {
                setImageResource(mapping.button.iconRes)
                contentDescription = mapping.button.label
                layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
            })
            addView(TextView(this@MainActivity).apply {
                text = mapping.button.label
                setTextColor(Color.WHITE)
                textSize = 20f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(12)
                }
            })
            addView(TextView(this@MainActivity).apply {
                text = "→"
                setTextColor(Color.parseColor("#80DEEA"))
                textSize = 28f
            })
            addView(destinationIcon(mapping).apply {
                layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)).apply { marginStart = dp(8) }
            })
        })
        card.addView(TextView(this).apply {
            text = destinationTitle(mapping)
            setTextColor(Color.WHITE)
            textSize = 17f
            setPadding(0, dp(12), 0, 0)
        })
        card.addView(TextView(this).apply {
            text = if (mapping.enabled) "Mapping enabled" else "Destination not enabled"
            setTextColor(Color.parseColor("#B9C5CE"))
            textSize = 14f
            setPadding(0, dp(4), 0, 0)
        })
        card.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
            addView(SwitchCompat(this@MainActivity).apply {
                text = if (mapping.enabled) "Mapping enabled" else "Mapping disabled"
                setTextColor(Color.WHITE)
                textSize = 16f
                background = getDrawable(R.drawable.tv_switch_focus_background)
                minimumHeight = dp(52)
                setPadding(dp(12), 0, dp(12), 0)
                isChecked = mapping.enabled
                setOnCheckedChangeListener { _, enabled ->
                    MappingPreferences.setEnabled(this@MainActivity, mapping.button, enabled)
                    DiagnosticLog.append(this@MainActivity, "${mapping.button.label}: mapping ${if (enabled) "enabled" else "disabled"}")
                    refresh()
                }
            })
            addView(Button(this@MainActivity).apply {
                text = "Configure"
                isAllCaps = false
                background = getDrawable(R.drawable.tv_button_background)
                setTextColor(resources.getColorStateList(R.color.tv_button_text, theme))
                minimumHeight = dp(52)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginStart = dp(20)
                }
                setOnClickListener { showConfigure(mapping.button) }
            })
        })
        return card
    }

    private fun gridParams() = GridLayout.LayoutParams(
        GridLayout.spec(GridLayout.UNDEFINED, 1f),
        GridLayout.spec(GridLayout.UNDEFINED, 1f),
    ).apply {
        width = 0
        height = GridLayout.LayoutParams.WRAP_CONTENT
        setMargins(0, 0, dp(16), dp(16))
    }

    private fun destinationIcon(mapping: ButtonMapping): ImageView = ImageView(this).apply {
        when (mapping.destinationType) {
            DestinationType.APP -> runCatching {
                setImageDrawable(packageManager.getApplicationIcon(mapping.destinationValue!!))
            }.getOrElse { setImageResource(R.drawable.ic_destination_empty) }
            DestinationType.INPUT -> setImageResource(R.drawable.ic_hdmi)
            null -> setImageResource(R.drawable.ic_destination_empty)
        }
        contentDescription = "Destination: ${destinationDescription(mapping)}"
    }

    private fun destinationTitle(mapping: ButtonMapping): String = when (mapping.destinationType) {
        DestinationType.APP -> "Opens ${appLabel(mapping.destinationValue).substringBefore(" (")}" 
        DestinationType.INPUT -> "Switches to ${inputLabel(mapping.destinationValue).substringBefore(" (")}" 
        null -> "Choose a destination"
    }

    private fun showConfigure(button: RemoteButton) {
        val choices = arrayOf(
            "Choose app",
            "Choose TV input",
            "Learn this physical button",
            "Clear destination",
            "Clear learned key",
        )
        AlertDialog.Builder(this)
            .setTitle("Configure ${button.label}")
            .setItems(choices) { _, which ->
                when (which) {
                    0 -> chooseApp(button)
                    1 -> chooseInput(button)
                    2 -> learnButton(button)
                    3 -> {
                        MappingPreferences.clearDestination(this, button)
                        MappingPreferences.setEnabled(this, button, false)
                        DiagnosticLog.append(this, "${button.label}: destination cleared")
                        refresh()
                    }
                    4 -> {
                        MappingPreferences.clearLearnedKey(this, button)
                        MappingPreferences.setEnabled(this, button, false)
                        DiagnosticLog.append(this, "${button.label}: learned key cleared")
                        refresh()
                    }
                }
            }
            .show()
    }

    private fun learnButton(button: RemoteButton) {
        MappingPreferences.startLearning(this, button)
        DiagnosticLog.append(this, "Learning ${button.label}: press its physical remote button once.")
        AlertDialog.Builder(this)
            .setTitle("Learning ${button.label}")
            .setMessage("Press ${button.label} once on the physical remote. The mapper will save the key and prevent its original app from opening during this learning press. Return here afterward to select a destination.")
            .setPositiveButton("Ready") { _, _ -> refresh() }
            .setNegativeButton("Cancel") { _, _ -> MappingPreferences.finishLearning(this); refresh() }
            .show()
    }

    private fun chooseApp(button: RemoteButton) {
        val apps = tvLauncherApps()
        if (apps.isEmpty()) {
            DiagnosticLog.append(this, "No visible launcher apps for ${button.label}.")
            refreshLog()
            return
        }
        val labels = apps.map { "${it.loadLabel(packageManager)}\n${it.activityInfo.packageName}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select app for ${button.label}")
            .setItems(labels) { _, which ->
                val packageName = apps[which].activityInfo.packageName
                MappingPreferences.setDestination(this, button, DestinationType.APP, packageName)
                MappingPreferences.setEnabled(this, button, true)
                DiagnosticLog.append(this, "${button.label}: app destination=$packageName")
                refresh()
            }
            .show()
    }

    private fun chooseInput(button: RemoteButton) {
        val manager = getSystemService(TV_INPUT_SERVICE) as? TvInputManager ?: return
        val inputs = manager.tvInputList.filter { it.isPassthroughInput }.sortedBy { it.loadLabel(this).toString() }
        if (inputs.isEmpty()) {
            DiagnosticLog.append(this, "No passthrough TV inputs visible for ${button.label}.")
            refreshLog()
            return
        }
        val labels = inputs.map { "${it.loadLabel(this)}\n${it.id}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select input for ${button.label}")
            .setItems(labels) { _, which ->
                MappingPreferences.setDestination(this, button, DestinationType.INPUT, inputs[which].id)
                MappingPreferences.setEnabled(this, button, true)
                DiagnosticLog.append(this, "${button.label}: input destination=${inputs[which].id}")
                refresh()
            }
            .show()
    }

    private fun tvLauncherApps(): List<ResolveInfo> {
        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
        )
        return intents.flatMap { packageManager.queryIntentActivities(it, 0) }
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
    }

    private fun destinationDescription(mapping: ButtonMapping): String = when (mapping.destinationType) {
        DestinationType.APP -> appLabel(mapping.destinationValue)
        DestinationType.INPUT -> inputLabel(mapping.destinationValue)
        null -> "not selected"
    }

    private fun appLabel(packageName: String?): String {
        if (packageName.isNullOrBlank()) return "not selected"
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            "${packageManager.getApplicationLabel(info)} ($packageName)"
        } catch (_: Exception) {
            "$packageName (not installed)"
        }
    }

    private fun inputLabel(inputId: String?): String {
        if (inputId.isNullOrBlank()) return "not selected"
        val manager = getSystemService(TV_INPUT_SERVICE) as? TvInputManager ?: return inputId
        return manager.tvInputList.firstOrNull { it.id == inputId }?.let { "${it.loadLabel(this)} ($inputId)" } ?: "$inputId (not currently visible)"
    }

    private fun refreshLog() { log.text = DiagnosticLog.read(this) }

    private fun updateLoggingUi() {
        val enabled = DiagnosticLog.isKeyEventLoggingEnabled(this)
        keyLogging.setOnCheckedChangeListener(null)
        keyLogging.isChecked = enabled
        keyLogging.setOnCheckedChangeListener { _, checked ->
            DiagnosticLog.setKeyEventLoggingEnabled(this, checked)
            updateLoggingUi()
        }
    }

    private fun isServiceEnabled(): Boolean {
        val expected = ComponentName(this, RemoteKeyAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun roundedBackground(color: String, stroke: String) = GradientDrawable().apply {
        setColor(Color.parseColor(color))
        cornerRadius = dp(12).toFloat()
        setStroke(dp(2), Color.parseColor(stroke))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
