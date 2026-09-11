package io.github.codehasan.developeroptions

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

// Xiaomi's ROM is keyed as "hyperos" or "miui" (not "xiaomi"), matching the guide maps.
internal fun oemKey(): String = when {
    isXiaomi() && isHyperOs() -> "hyperos"
    isXiaomi() -> "miui"
    else -> Build.MANUFACTURER.orEmpty().lowercase(Locale.ROOT)
}

internal fun isXiaomi(): Boolean = "xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)

internal fun isVivo(): Boolean = "vivo".equals(Build.MANUFACTURER, ignoreCase = true)

private fun isHyperOs(): Boolean = try {
    val get = Class.forName("android.os.SystemProperties")
        .getMethod("get", String::class.java)
    !(get.invoke(null, "ro.mi.os.version.name") as? String).isNullOrEmpty()
} catch (_: Exception) {
    false
}

internal fun isTablet(context: Context): Boolean {
    val size = context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    return size >= Configuration.SCREENLAYOUT_SIZE_LARGE
}
