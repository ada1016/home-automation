package io.github.tclremotemapper

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.media.tv.TvContract
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/** Records keys delivered by Android and consumes only explicitly enabled mappings. */
class RemoteKeyAccessibilityService : AccessibilityService() {
    private var primeDownConsumed = false
    private var mediaDownConsumed = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        DiagnosticLog.append(this, "Accessibility service connected; key filtering requested.")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val line = buildString {
            append("KEY action=").append(actionName(event.action))
            append(" keyCode=").append(event.keyCode).append("(").append(KeyEvent.keyCodeToString(event.keyCode)).append(")")
            append(" scanCode=").append(event.scanCode)
            append(" repeat=").append(event.repeatCount)
            append(" deviceId=").append(event.deviceId)
            append(" source=0x").append(event.source.toString(16))
            append(" flags=0x").append(event.flags.toString(16))
        }
        Log.i(TAG, line)
        DiagnosticLog.append(this, line)

        return when {
            event.keyCode == PRIME_KEYCODE && MappingPreferences.enabled(this) -> consumePrime(event)
            event.keyCode == MEDIA_KEYCODE && MappingPreferences.mediaEnabled(this) -> consumeMedia(event)
            else -> false
        }
    }

    private fun consumePrime(event: KeyEvent): Boolean = when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) switchToConfiguredInput()
                primeDownConsumed = true
                true // Consume DOWN and all repeats: TCL/Prime cannot launch.
            }
            KeyEvent.ACTION_UP -> {
                val consumed = primeDownConsumed
                primeDownConsumed = false
                consumed // Consume the matching UP so it cannot trigger another action.
            }
            else -> true
    }

    private fun consumeMedia(event: KeyEvent): Boolean = when (event.action) {
        KeyEvent.ACTION_DOWN -> {
            if (event.repeatCount == 0) launchConfiguredMediaApp()
            mediaDownConsumed = true
            true // Consume repeats so a hold cannot relaunch Spotify.
        }
        KeyEvent.ACTION_UP -> {
            val consumed = mediaDownConsumed
            mediaDownConsumed = false
            consumed
        }
        else -> true
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

    private fun switchToConfiguredInput() {
        val inputId = MappingPreferences.inputId(this)
        if (inputId.isNullOrBlank()) {
            DiagnosticLog.append(this, "Mapping skipped: no destination input selected.")
            return
        }
        try {
            // Verified on this 65C935 / AR2101 firmware. The standard URI alone
            // resolves to another app, so direct it to TCL's exported TV activity.
            val intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(inputId))
                .setComponent(ComponentName(TCL_TV_PACKAGE, TCL_TV_ACTIVITY))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            DiagnosticLog.append(this, "Mapping: switched to inputId=$inputId")
        } catch (e: Exception) {
            DiagnosticLog.append(this, "Mapping failed: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun launchConfiguredMediaApp() {
        val packageName = MappingPreferences.mediaPackage(this)
        if (packageName.isNullOrBlank()) {
            DiagnosticLog.append(this, "Media mapping skipped: no destination app selected.")
            return
        }
        try {
            val fallback = if (packageName == SPOTIFY_PACKAGE) {
                Intent().setComponent(ComponentName(SPOTIFY_PACKAGE, SPOTIFY_ACTIVITY))
            } else {
                null
            }
            val intent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
                ?: fallback
                ?: throw IllegalStateException("No TV launcher activity for $packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            DiagnosticLog.append(this, "Mapping: launched app=$packageName")
        } catch (e: Exception) {
            DiagnosticLog.append(this, "Media launch failed: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "TclRemoteDiag"
        const val PRIME_KEYCODE = 4099 // Observed on the user's TCL 65C935, Android 11 / AR2101.
        const val MEDIA_KEYCODE = 4057 // TCL_USB_MEDIA, observed on this TCL 65C935 remote.
        const val TCL_TV_PACKAGE = "com.tcl.tv"
        const val TCL_TV_ACTIVITY = "com.tcl.tv.TVActivity"
        const val SPOTIFY_PACKAGE = "com.spotify.tv.android"
        const val SPOTIFY_ACTIVITY = "com.spotify.tv.android.SpotifyTVActivity"
    }
}
