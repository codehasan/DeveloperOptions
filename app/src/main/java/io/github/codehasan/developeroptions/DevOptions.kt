package io.github.codehasan.developeroptions

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

/** Returns true if Developer options is turned on for this device. */
fun isDeveloperOptionsEnabled(context: Context): Boolean {
    return Settings.Global.getInt(
        context.contentResolver,
        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
        0
    ) != 0
}

/**
 * Tries to open the system Developer options screen.
 *
 * Returns true only if that screen actually launched. It returns false — rather
 * than falling back to the generic Settings home — when the intent can't be
 * resolved yet. That happens for a short moment right after the user switches
 * Developer options on: the DEVELOPMENT_SETTINGS_ENABLED flag flips before the
 * system registers the activity that handles this action, so an immediate launch
 * throws ActivityNotFoundException. Callers should retry shortly instead.
 */
fun openDeveloperOptions(context: Context): Boolean {
    return try {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        true
    } catch (_: Exception) {
        false
    }
}

/** Opens the top-level Settings screen as a last resort. */
fun openSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    } catch (_: Exception) {
        Toast.makeText(context, R.string.cannot_open_settings, Toast.LENGTH_LONG).show()
    }
}

/** Opens the "About phone" screen so the user can tap the build number 7 times. */
fun openAboutPhone(context: Context) {
    val intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
    startSettings(context, intent, Settings.ACTION_SETTINGS)
}

private fun startSettings(context: Context, intent: Intent, fallbackAction: String) {
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(fallbackAction))
        } catch (_: Exception) {
            Toast.makeText(context, R.string.cannot_open_settings, Toast.LENGTH_LONG).show()
        }
    }
}
