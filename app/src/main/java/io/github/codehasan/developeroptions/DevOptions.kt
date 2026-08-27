package io.github.codehasan.developeroptions

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

/** Returns true if Developer options is turned on for this device. */
fun isDeveloperOptionsEnabled(context: Context): Boolean {
    return Settings.Secure.getInt(
        context.contentResolver,
        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
        0
    ) != 0
}

/** Opens the system Developer options screen. */
fun openDeveloperOptions(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
    startSettings(context, intent, Settings.ACTION_SETTINGS)
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
