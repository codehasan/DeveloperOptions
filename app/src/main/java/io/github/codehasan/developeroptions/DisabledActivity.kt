package io.github.codehasan.developeroptions

import android.os.Build
import android.os.Bundle
import android.text.Annotation
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class DisabledActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_disabled)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<TextView>(R.id.deviceText).text = deviceName()

        findViewById<Button>(R.id.startButton).setOnClickListener {
            // The button adapts to the current state. If Developer options is
            // already on (we're only still here because its screen wouldn't open),
            // retry opening it; otherwise send the user to About phone to turn it on.
            if (isDeveloperOptionsEnabled(this)) {
                // Try the Developer options screen; if it won't open on this
                // build, fall back to the main Settings app so the user can
                // still get there manually.
                if (openDeveloperOptions(this) || openSettings(this)) finish()
            } else {
                openAboutPhone(this)
            }
        }
    }

    /** Points the title, message and button at the copy for the current state. */
    private fun updateUiForState() {
        val enabled = isDeveloperOptionsEnabled(this)
        findViewById<TextView>(R.id.titleText).text =
            coloredAnnotations(if (enabled) R.string.enabled_title else R.string.disabled_title)
        findViewById<TextView>(R.id.messageText).text =
            getText(if (enabled) R.string.enabled_message else R.string.disabled_message)
        findViewById<Button>(R.id.startButton).text =
            getText(if (enabled) R.string.open_dev_options else R.string.start)
    }

    /**
     * Reads a string resource as markup and turns each <annotation color="..."> span
     * into a theme-aware colored span ("error" -> red, "success" -> green).
     */
    private fun coloredAnnotations(resId: Int): CharSequence {
        // A string without markup comes back as a plain String, not SpannedString
        // (e.g. a translation that drops the <annotation> tag) — fall back safely.
        val text = getText(resId) as? SpannedString ?: return getText(resId)
        val builder = SpannableStringBuilder(text)
        for (annotation in text.getSpans(0, text.length, Annotation::class.java)) {
            if (annotation.key != "color") continue
            val color = when (annotation.value) {
                "error" -> R.color.text_error
                "success" -> R.color.text_success
                else -> continue
            }
            builder.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, color)),
                text.getSpanStart(annotation),
                text.getSpanEnd(annotation),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return builder
    }

    /**
     * A human-readable name for the current device, e.g. "Google Pixel 7".
     * MODEL sometimes already starts with the manufacturer (some brands set it
     * that way), so we avoid repeating it — and capitalize the manufacturer,
     * which Build.MANUFACTURER often reports lower-case (e.g. "samsung").
     */
    private fun deviceName(): String {
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
        val model = Build.MODEL?.trim().orEmpty()
        return when {
            model.startsWith(manufacturer, ignoreCase = true) -> model
            manufacturer.isEmpty() -> model
            else -> "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
        }.trim()
    }

    override fun onResume() {
        super.onResume()
        // Runs every time this screen becomes visible again, including when the
        // user presses back to close the About phone page after tapping the build
        // number. If Developer options is now on and its screen opens, we're done.
        // Otherwise stay put (doing nothing else) and just reflect the current
        // state in the UI — the user drives the next step via the button.
        if (isDeveloperOptionsEnabled(this) && openDeveloperOptions(this)) {
            finish()
            return
        }
        updateUiForState()
    }
}
