package io.github.codehasan.developeroptions

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat

// True if Developer options is actually toggled on. This is the real state, not
// "reachable": on Vivo/FuntouchOS the build-number taps only make the screen
// reachable, and this flag stays off until the user toggles it there by hand.
//
// Some OEMs record the flag under Settings.Secure instead of Settings.Global, so
// treat it as enabled if either says so (ported from g.dvz.bw).
fun isDeveloperOptionsEnabled(context: Context): Boolean = try {
    val resolver = context.contentResolver
    val onGlobal = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
            Settings.Global.getInt(resolver, DEV_SETTINGS_KEY, 0) == 1
    @Suppress("DEPRECATION")
    onGlobal || Settings.Secure.getInt(resolver, DEV_SETTINGS_KEY, 0) == 1
} catch (_: Exception) {
    false
}

/**
 * Opens the system Developer options screen; returns true if it launched.
 *
 * Ported from g.dvz.cb: the entry point differs by OEM and API level, so it walks
 * a tiered set of intents, verifying each resolves before firing it.
 */
fun openDeveloperOptions(context: Context): Boolean {
    // Vivo ships Developer options behind its own activity, not the AOSP one.
    if ("vivo".equals(Build.MANUFACTURER, ignoreCase = true)) {
        if (tryStartResolvable(context, componentIntent(VIVO_DEV_SETTINGS))) return true
    }
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> launchOnDashboardEra(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ->
            tryStartResolvable(context, componentIntent(DEVELOPMENT_SETTINGS)) ||
                    tryStartResolvable(context, Intent(ACTION_DEV_SETTINGS))

        else ->
            tryStartResolvable(context, Intent(ACTION_DEV_SETTINGS)) ||
                    tryStartResolvable(context, componentIntent(DEVELOPMENT_SETTINGS))
    }
}

// Android 9+ (Settings dashboard era): prefer the modern dashboard activity, fall
// back to the legacy component, then the implicit action.
private fun launchOnDashboardEra(context: Context): Boolean {
    val pm = context.packageManager ?: return false
    val dashboard = componentIntent(DEVELOPMENT_DASHBOARD)
    if (pm.queryIntentActivities(dashboard, 0).isNotEmpty()) {
        return tryStartResolvable(context, dashboard)
    }
    val legacy = componentIntent(DEVELOPMENT_SETTINGS)
    if (pm.queryIntentActivities(legacy, 0).isNotEmpty()) {
        return tryStartResolvable(context, legacy)
    }
    val action = Intent(ACTION_DEV_SETTINGS)
    val resolvers = pm.queryIntentActivities(action, 0)
    if (resolvers.isEmpty()) return false
    // A lone DevelopmentSettingsDisabledActivity means the screen is the "disabled"
    // stub — the real one isn't enabled yet, so don't treat it as launchable.
    if (resolvers.size == 1 &&
        resolvers[0].activityInfo?.name.orEmpty().endsWith(DISABLED_ACTIVITY_SUFFIX)
    ) {
        return false
    }
    return tryStartResolvable(context, action)
}

/**
 * Shows the OEM-specific "how to enable Developer options" dialog and, on OK,
 * sends the user to the right About screen. While it's up it watches for the flag
 * flipping on and takes over automatically (ported from g.dvz.bo).
 */
fun showDevOptionsGuide(activity: Activity) {
    val tablet = isTablet(activity)
    val key = oemKey()
    val page = activity.getString(pageResFor(key, tablet))
    val field = activity.getString(pathResFor(key))
    val html = activity.getString(R.string.dev_options_guide_step_1, page) +
            "<br/><br/>" + activity.getString(R.string.dev_options_guide_step_2, field) +
            "<br/><br/>" + activity.getString(R.string.dev_options_guide_suffix)

    val body = TextView(activity).apply {
        text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        setLineSpacing(0f, 1.4f)
        val h = dp(activity, 24f)
        setPadding(h, dp(activity, 16f), h, 0)
        textSize = 16f
    }

    val dialog = AlertDialog.Builder(activity)
        .setTitle(R.string.dev_options_guide_title)
        .setView(body)
        .setPositiveButton(R.string.ok, null)
        .create()
    dialog.show()

    // Set the listener directly so tapping OK doesn't auto-dismiss: the dialog
    // stays up as a reference while the user is in Settings.
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
        if (isDeveloperOptionsEnabled(activity)) {
            openDeveloperOptions(activity)
            dialog.dismiss()
        } else {
            openAboutPhone(activity)
        }
    }

    val handler = Handler(Looper.getMainLooper())
    val poll = object : Runnable {
        override fun run() {
            if (isDeveloperOptionsEnabled(activity)) {
                if (openDeveloperOptions(activity)) dialog.dismiss()
            } else {
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
    }
    handler.postDelayed(poll, POLL_INTERVAL_MS)
    dialog.setOnDismissListener { handler.removeCallbacks(poll) }
}

/** Opens the About phone screen where Build number lives; OEM-aware for Xiaomi. */
fun openAboutPhone(context: Context): Boolean {
    if (isXiaomi()) {
        if (tryStartResolvable(context, componentIntent(XIAOMI_DEVICE_INFO))) return true
        if (tryStartResolvable(context, Intent(XIAOMI_DEVICE_INFO_ACTION))) return true
    }
    if (tryStart(context, Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))) return true
    if (tryStart(context, Intent(Settings.ACTION_SETTINGS))) return true
    Toast.makeText(context, R.string.cannot_open_settings, Toast.LENGTH_LONG).show()
    return false
}

/** Last-resort fallback for when the Developer options screen won't launch. */
fun openSettings(context: Context): Boolean = tryStart(context, Intent(Settings.ACTION_SETTINGS))

// Fires only if the intent resolves to an activity (mirrors g.aby.g's guarded launch).
private fun tryStartResolvable(context: Context, intent: Intent): Boolean = try {
    val pm = context.packageManager
    if (pm != null && pm.queryIntentActivities(intent, 0).isEmpty()) {
        false
    } else {
        startFrom(context, intent)
        true
    }
} catch (_: Exception) {
    false
}

private fun tryStart(context: Context, intent: Intent): Boolean = try {
    startFrom(context, intent)
    true
} catch (_: Exception) {
    false
}

private fun startFrom(context: Context, intent: Intent) {
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun componentIntent(activity: String): Intent =
    Intent().setComponent(ComponentName(SETTINGS_PACKAGE, activity))

private fun isXiaomi(): Boolean = "xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)

// Xiaomi's ROM is keyed as "hyperos" or "miui" (not "xiaomi"), matching the guide maps.
private fun oemKey(): String = when {
    isXiaomi() && isHyperOs() -> "hyperos"
    isXiaomi() -> "miui"
    else -> Build.MANUFACTURER.orEmpty().lowercase()
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

private fun dp(context: Context, value: Float): Int =
    (value * context.resources.displayMetrics.density).toInt()

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

private const val DEV_SETTINGS_KEY = Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
private const val ACTION_DEV_SETTINGS: String = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
private const val SETTINGS_PACKAGE = "com.android.settings"
private const val DEVELOPMENT_DASHBOARD =
    "com.android.settings.Settings\$DevelopmentSettingsDashboardActivity"
private const val DEVELOPMENT_SETTINGS = "com.android.settings.DevelopmentSettings"
private const val DISABLED_ACTIVITY_SUFFIX = ".DevelopmentSettingsDisabledActivity"
private const val VIVO_DEV_SETTINGS = "com.vivo.settings.DevelpmentSettingsActivity2"
private const val XIAOMI_DEVICE_INFO = "com.android.settings.Settings\$MyDeviceInfoActivity"
private const val XIAOMI_DEVICE_INFO_ACTION = "miui.intent.action.DEVICE_INFO_SETTINGS"
private const val POLL_INTERVAL_MS = 1000L
