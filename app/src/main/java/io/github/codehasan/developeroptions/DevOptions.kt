package io.github.codehasan.developeroptions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import java.util.Locale

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
    // Vivo ships Developer options behind its own activity, not the AOSP one.
    if ("vivo".equals(Build.MANUFACTURER, ignoreCase = true)) {
        if (tryStart(context, componentIntent(VIVO_DEV_SETTINGS))) return true
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        launchOnDashboardEra(context)
    } else {
        tryStart(context, componentIntent(DEVELOPMENT_SETTINGS)) ||
                tryStart(context, Intent(ACTION_DEV_SETTINGS))
    }
}

// Android 9+ (Settings dashboard era): try the modern dashboard activity, then the
// legacy and OEM-specific components, then the implicit action.
private fun launchOnDashboardEra(context: Context): Boolean {
    if (tryStart(context, componentIntent(DEVELOPMENT_DASHBOARD))) return true
    if (tryStart(context, componentIntent(DEVELOPMENT_SETTINGS))) return true
    if (tryStart(context, componentIntent(TRANSSION_DEV_SETTINGS))) return true
    val action = Intent(ACTION_DEV_SETTINGS)
    val resolvers = context.packageManager?.queryIntentActivities(action, 0).orEmpty()
    if (resolvers.isEmpty()) return false
    // A lone DevelopmentSettingsDisabledActivity means the screen is the "disabled"
    // stub — the real one isn't enabled yet, so don't treat it as launchable.
    if (resolvers.size == 1 &&
        resolvers[0].activityInfo?.name.orEmpty().endsWith(DISABLED_ACTIVITY_SUFFIX)
    ) {
        return false
    }
    return tryStart(context, action)
}

/** Opens the About phone screen where Build number lives; OEM-aware for Xiaomi. */
fun openAboutPhone(context: Context): Boolean {
    if (isXiaomi()) {
        if (tryStart(context, componentIntent(XIAOMI_DEVICE_INFO))) return true
        if (tryStart(context, Intent(XIAOMI_DEVICE_INFO_ACTION))) return true
    }
    if (tryStart(context, Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))) return true
    if (tryStart(context, Intent(Settings.ACTION_SETTINGS))) return true
    Toast.makeText(context, R.string.cannot_open_settings, Toast.LENGTH_LONG).show()
    return false
}

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

// Fires only if the intent resolves to an activity. Callers pass Activity contexts,
// so the launch stays in-task and the returning onResume detection keeps working.
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

private fun componentIntent(activity: String): Intent =
    Intent().setComponent(ComponentName(SETTINGS_PACKAGE, activity))

private fun isXiaomi(): Boolean = "xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)

// Xiaomi's ROM is keyed as "hyperos" or "miui" (not "xiaomi"), matching the guide maps.
private fun oemKey(): String = when {
    isXiaomi() && isHyperOs() -> "hyperos"
    isXiaomi() -> "miui"
    else -> Build.MANUFACTURER.orEmpty().lowercase(Locale.ROOT)
}

private fun isHyperOs(): Boolean = try {
    val get = Class.forName("android.os.SystemProperties")
        .getMethod("get", String::class.java)
    !(get.invoke(null, "ro.mi.os.version.name") as? String).isNullOrEmpty()
} catch (_: Exception) {
    false
}

private fun isTablet(context: Context): Boolean {
    val size = context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    return size >= Configuration.SCREENLAYOUT_SIZE_LARGE
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
private const val DISABLED_ACTIVITY_SUFFIX = ".DevelopmentSettingsDisabledActivity"
private const val TRANSSION_DEV_SETTINGS =
    $$"com.android.settings.Settings$DevelopmentSettingsActivity"
private const val VIVO_DEV_SETTINGS = "com.vivo.settings.DevelpmentSettingsActivity2"
private const val XIAOMI_DEVICE_INFO = $$"com.android.settings.Settings$MyDeviceInfoActivity"
private const val XIAOMI_DEVICE_INFO_ACTION = "miui.intent.action.DEVICE_INFO_SETTINGS"
