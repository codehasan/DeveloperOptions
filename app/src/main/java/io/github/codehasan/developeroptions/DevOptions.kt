package io.github.codehasan.developeroptions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

fun isDeveloperOptionsEnabled(context: Context): Boolean =
    Settings.Global.getInt(
        context.contentResolver,
        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
        0
    ) != 0

/**
 * Opens the system Developer options screen; returns true if it launched.
 *
 * The entry point differs by OEM and API level, so it walks a tiered set of
 * intents, verifying each resolves before firing it.
 */
fun openDeveloperOptions(context: Context): Boolean {
    if (isVivo()) {
        if (tryStart(context, VIVO_DEV_SETTINGS)) return true
    }
    if (tryStart(context, DEVELOPMENT_DASHBOARD)) return true
    if (tryStart(context, DEVELOPMENT_SETTINGS)) return true
    if (tryStart(context, TRANSSION_DEV_SETTINGS)) return true
    val devSettings = Intent(ACTION_DEV_SETTINGS)
    val resolvers = context.packageManager?.queryIntentActivities(devSettings, 0).orEmpty()
    return when {
        resolvers.isEmpty() -> false
        // A lone DevelopmentSettingsDisabledActivity means the screen is the "disabled"
        // stub — the real one isn't enabled yet, so don't treat it as launchable.
        resolvers.size == 1 && resolvers[0].activityInfo?.name.orEmpty()
            .endsWith(DEV_ACTIVITY_DISABLED_SUFFIX) -> false

        else -> tryStart(context, devSettings)
    }
}

/** Opens the About phone screen where Build number lives; OEM-aware for Xiaomi. */
fun openAboutPhone(context: Context): Boolean {
    if (isXiaomi()) {
        if (tryStart(context, XIAOMI_DEVICE_INFO)) return true
        if (tryStart(context, Intent(ACTION_XIAOMI_DEVICE_INFO))) return true
    }
    if (tryStart(context, Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))) return true
    if (tryStart(context, Intent(Settings.ACTION_SETTINGS))) return true
    Toast.makeText(context, R.string.cannot_open_settings, Toast.LENGTH_LONG).show()
    return false
}

/** Opens the System screen where Developer options lives. */
fun openSystem(context: Context): Boolean = tryStart(context, SYSTEM_DASHBOARD)

/** Last-resort fallback for when the Developer options screen won't launch. */
fun openSettings(context: Context): Boolean = tryStart(context, Intent(Settings.ACTION_SETTINGS))

/** Where Build number hides on this device: the About screen and the field to tap. */
data class DevOptionsHint(val aboutPage: String, val buildField: String)

fun devOptionsHint(context: Context): DevOptionsHint {
    val key = oemKey()
    return DevOptionsHint(
        aboutPage = context.getString(pageResFor(key, isTablet(context))),
        buildField = context.getString(pathResFor(key)),
    )
}

private fun tryStart(context: Context, activity: String): Boolean = tryStart(
    context,
    Intent().setComponent(
        ComponentName(SETTINGS_PACKAGE, activity)
    )
)

private fun tryStart(context: Context, intent: Intent): Boolean = try {
    if (context.packageManager?.queryIntentActivities(intent, 0).isNullOrEmpty()) {
        false
    } else {
        context.startActivity(intent)
        true
    }
} catch (_: Exception) {
    false
}

// Step 1 destination. Only miui/hyperos/huawei/samsung/vivo have distinct tablet
// wording; the rest reuse their phone string, and unknown OEMs fall to a default.
private fun pageResFor(key: String, tablet: Boolean): Int = when (key) {
    "miui" -> if (tablet) R.string.dev_options_page_miui_tablet else R.string.dev_options_page_miui
    "hyperos" -> if (tablet) R.string.dev_options_page_hyperos_tablet else R.string.dev_options_page_hyperos
    "huawei" -> if (tablet) R.string.dev_options_page_huawei_tablet else R.string.dev_options_page_huawei
    "samsung" -> if (tablet) R.string.dev_options_page_samsung_tablet else R.string.dev_options_page_samsung
    "vivo" -> if (tablet) R.string.dev_options_page_vivo_tablet else R.string.dev_options_page_vivo
    "honor" -> R.string.dev_options_page_honor
    "oppo" -> R.string.dev_options_page_oppo
    "oneplus" -> R.string.dev_options_page_oneplus
    "realme" -> R.string.dev_options_page_realme
    else -> if (tablet) R.string.dev_options_page_default_tablet else R.string.dev_options_page_default
}

// Step 2: the field to tap 7 times.
private fun pathResFor(key: String): Int = when (key) {
    "honor" -> R.string.dev_options_path_honor
    "huawei" -> R.string.dev_options_path_huawei
    "hyperos" -> R.string.dev_options_path_hyperos
    "miui" -> R.string.dev_options_path_miui
    "oneplus" -> R.string.dev_options_path_oneplus
    "oppo" -> R.string.dev_options_path_oppo
    "realme" -> R.string.dev_options_path_realme
    "samsung" -> R.string.dev_options_path_samsung
    "vivo" -> R.string.dev_options_path_vivo
    else -> R.string.dev_options_path_default
}

private const val ACTION_DEV_SETTINGS: String = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
private const val SETTINGS_PACKAGE = "com.android.settings"
private const val SYSTEM_DASHBOARD = $$"com.android.settings.Settings$SystemDashboardActivity"
private const val DEVELOPMENT_DASHBOARD =
    $$"com.android.settings.Settings$DevelopmentSettingsDashboardActivity"
private const val DEVELOPMENT_SETTINGS = "com.android.settings.DevelopmentSettings"
private const val TRANSSION_DEV_SETTINGS =
    $$"com.android.settings.Settings$DevelopmentSettingsActivity"
private const val VIVO_DEV_SETTINGS = "com.vivo.settings.DevelpmentSettingsActivity2"
private const val XIAOMI_DEVICE_INFO = $$"com.android.settings.Settings$MyDeviceInfoActivity"
private const val ACTION_XIAOMI_DEVICE_INFO = "miui.intent.action.DEVICE_INFO_SETTINGS"
private const val DEV_ACTIVITY_DISABLED_SUFFIX = ".DevelopmentSettingsDisabledActivity"
