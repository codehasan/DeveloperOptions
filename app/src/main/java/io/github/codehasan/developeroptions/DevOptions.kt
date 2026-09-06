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
 * Returns true only if that screen actually launched, false if no attempt could
 * resolve it.
 *
 * The normal path is an implicit intent, which works on stock Android. But on
 * some OEM builds (seen on Transsion/HiOS, Android 16) the DEVELOPMENT_SETTINGS_ENABLED
 * flag flips and the screen's component is enabled, yet Settings hasn't committed
 * its intent-filter into PackageManager's resolver until the Settings process next
 * runs — so the implicit intent throws ActivityNotFoundException even though the
 * screen is ready. In that window we fall back to launching the (already enabled)
 * Settings activity explicitly by class name, which bypasses intent resolution.
 */
fun openDeveloperOptions(context: Context): Boolean {
    // 1. Implicit intent: correct on stock Android and once the resolver is fresh.
    if (tryStart(context, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))) {
        return true
    }
    // 2. Explicit fallback for OEM builds whose resolver lags behind the flag.
    for (activity in DEV_OPTIONS_ACTIVITIES) {
        val explicit = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            .setClassName(SETTINGS_PACKAGE, activity)
        if (tryStart(context, explicit)) return true
    }
    return false
}

private fun tryStart(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (_: Exception) {
    false
}

private const val SETTINGS_PACKAGE = "com.android.settings"

/** Known class names for the Developer options screen across Android builds. */
private val DEV_OPTIONS_ACTIVITIES = listOf(
    // AOSP / Pixel and most recent builds.
    "com.android.settings.Settings\$DevelopmentSettingsDashboardActivity",
    // Older AOSP and some OEM skins (e.g. Transsion/HiOS).
    "com.android.settings.Settings\$DevelopmentSettingsActivity",
)

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
