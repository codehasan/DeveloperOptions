package io.github.codehasan.developeroptions

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

// True if Developer options is actually toggled on. This is the real state, not
// "reachable": on Vivo/FuntouchOS the build-number taps only make the screen
// reachable, and this flag stays off until the user toggles it there by hand.
fun isDeveloperOptionsEnabled(context: Context): Boolean =
    Settings.Global.getInt(
        context.contentResolver,
        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
        0
    ) != 0

/**
 * Opens the system Developer options screen; returns true if it launched.
 *
 * The implicit intent is correct on stock Android. But on some OEM builds
 * (seen on Transsion/HiOS, Android 16) Settings enables the screen's component
 * before committing its intent-filter to PackageManager's resolver, so the
 * implicit intent throws ActivityNotFoundException even though the screen is
 * ready. The explicit fallback launches it by class name, bypassing resolution.
 */
fun openDeveloperOptions(context: Context): Boolean {
    if (tryStart(context, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))) {
        return true
    }
    for (activity in DEV_OPTIONS_ACTIVITIES) {
        val explicit = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            .setClassName(SETTINGS_PACKAGE, activity)
        if (tryStart(context, explicit)) return true
    }
    return false
}

/** Last-resort fallback for when the Developer options screen won't launch. */
fun openSettings(context: Context): Boolean =
    tryStart(context, Intent(Settings.ACTION_SETTINGS))

private fun tryStart(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (_: Exception) {
    false
}

private const val SETTINGS_PACKAGE = "com.android.settings"

private val DEV_OPTIONS_ACTIVITIES = listOf(
    "com.android.settings.Settings\$DevelopmentSettingsDashboardActivity", // AOSP/Pixel, recent builds
    "com.android.settings.Settings\$DevelopmentSettingsActivity",          // older AOSP, some OEM skins
)

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
