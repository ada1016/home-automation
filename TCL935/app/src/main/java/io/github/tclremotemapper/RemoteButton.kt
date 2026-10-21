package io.github.tclremotemapper

/** The six labelled shortcut buttons on the supplied TCL remote. */
enum class RemoteButton(
    val id: String,
    val label: String,
    val iconRes: Int,
    val knownKeyCode: Int? = null,
    val knownScanCode: Int? = null,
) {
    // Verified on the owner's supplied TCL remote through the Accessibility service.
    NETFLIX("netflix", "Netflix", R.drawable.ic_remote_netflix, knownKeyCode = 4062, knownScanCode = 242),
    PRIME("prime", "Prime Video", R.drawable.ic_remote_prime, knownKeyCode = 4099, knownScanCode = 752),
    GUARD("guard", "Guard", R.drawable.ic_remote_guard, knownKeyCode = 4140, knownScanCode = 748),
    MEDIA("media", "Media", R.drawable.ic_remote_media, knownKeyCode = 4057),
    YOUTUBE("youtube", "YouTube", R.drawable.ic_remote_youtube, knownKeyCode = 4065, knownScanCode = 245),
    // Verified on the owner's supplied TCL remote through the Accessibility service.
    TCL_CHANNEL("tcl_channel", "TCL Channel", R.drawable.ic_remote_tcl_channel, knownKeyCode = 4020, knownScanCode = 227),
}

enum class DestinationType { APP, INPUT }

data class ButtonMapping(
    val button: RemoteButton,
    val keyCode: Int?,
    val scanCode: Int?,
    val enabled: Boolean,
    val destinationType: DestinationType?,
    val destinationValue: String?,
) {
    val isLearned: Boolean get() = keyCode != null
    val isConfigured: Boolean get() = enabled && isLearned && destinationType != null && !destinationValue.isNullOrBlank()
}
