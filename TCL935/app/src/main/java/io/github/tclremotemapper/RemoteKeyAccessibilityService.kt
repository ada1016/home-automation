package io.github.tclremotemapper

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.media.tv.TvContract
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/** Receives keys Android makes available and consumes only configured shortcut mappings. */
class RemoteKeyAccessibilityService : AccessibilityService() {
    private var consumedKeyCode: Int? = null
    private var learningKeyCode: Int? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        MappingPreferences.ensureMigrated(this)
        DiagnosticLog.append(this, "Accessibility service connected; six-button key filtering ready.")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (DiagnosticLog.isKeyEventLoggingEnabled(this)) {
            DiagnosticLog.appendKeyEvent(this, buildString {
                append("KEY action=").append(actionName(event.action))
                append(" keyCode=").append(event.keyCode).append("(").append(KeyEvent.keyCodeToString(event.keyCode)).append(")")
                append(" scanCode=").append(event.scanCode)
                append(" repeat=").append(event.repeatCount)
                append(" deviceId=").append(event.deviceId)
                append(" source=0x").append(event.source.toString(16))
                append(" flags=0x").append(event.flags.toString(16))
            })
        }

        handleLearning(event)?.let { return it }
        val mapping = MappingPreferences.mappingForEvent(this, event) ?: return false
        return consumeMappedButton(event, mapping)
    }

    private fun handleLearning(event: KeyEvent): Boolean? {
        val learning = MappingPreferences.learningButton(this) ?: return null
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    MappingPreferences.learn(this, learning, event)
                    MappingPreferences.finishLearning(this)
                    learningKeyCode = event.keyCode
                    DiagnosticLog.append(this, "Learned ${learning.label}: keyCode=${event.keyCode}, scanCode=${event.scanCode}")
                }
                true // Do not launch the button's original app while learning it.
            }
            KeyEvent.ACTION_UP -> {
                val consumed = learningKeyCode == event.keyCode
                if (consumed) learningKeyCode = null
                consumed
            }
            else -> true
        }
    }

    private fun consumeMappedButton(event: KeyEvent, mapping: ButtonMapping): Boolean = when (event.action) {
        KeyEvent.ACTION_DOWN -> {
            if (event.repeatCount == 0) launchDestination(mapping)
            consumedKeyCode = event.keyCode
            true
        }
        KeyEvent.ACTION_UP -> {
            val consumed = consumedKeyCode == event.keyCode
            if (consumed) consumedKeyCode = null
            consumed
        }
        else -> true
    }

    private fun launchDestination(mapping: ButtonMapping) {
        val type = mapping.destinationType
        val value = mapping.destinationValue
        if (type == null || value.isNullOrBlank()) {
            DiagnosticLog.append(this, "${mapping.button.label}: mapping skipped; select a destination first.")
            return
        }
        try {
            when (type) {
                DestinationType.INPUT -> switchToInput(value)
                DestinationType.APP -> launchApp(value)
            }
            DiagnosticLog.append(this, "${mapping.button.label}: launched ${type.name.lowercase()}=$value")
        } catch (e: Exception) {
            DiagnosticLog.append(this, "${mapping.button.label}: launch failed: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun switchToInput(inputId: String) {
        // Verified on this TCL 65C935 / AR2101 firmware. The explicit TCL activity
        // is required because the standard passthrough URI may resolve elsewhere.
        val intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(inputId))
            .setComponent(ComponentName(TCL_TV_PACKAGE, TCL_TV_ACTIVITY))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
            ?: packageManager.getLaunchIntentForPackage(packageName)
            ?: throw IllegalStateException("No launcher activity for $packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        DiagnosticLog.append(this, "Accessibility service disconnected.")
        super.onDestroy()
    }

    private fun actionName(action: Int) = when (action) {
        KeyEvent.ACTION_DOWN -> "DOWN"
        KeyEvent.ACTION_UP -> "UP"
        KeyEvent.ACTION_MULTIPLE -> "MULTIPLE"
        else -> action.toString()
    }

    private companion object {
        const val TCL_TV_PACKAGE = "com.tcl.tv"
        const val TCL_TV_ACTIVITY = "com.tcl.tv.TVActivity"
    }
}
