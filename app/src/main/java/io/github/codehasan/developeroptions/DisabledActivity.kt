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
            if (isDeveloperOptionsEnabled(this)) {
                if (openDeveloperOptions(this) || openSettings(this)) finish()
            } else {
                showDevOptionsGuide(this)
            }
        }
    }

    private fun updateUiForState() {
        val enabled = isDeveloperOptionsEnabled(this)
        findViewById<TextView>(R.id.titleText).text =
            coloredAnnotations(if (enabled) R.string.enabled_title else R.string.disabled_title)
        findViewById<TextView>(R.id.messageText).text =
            getText(if (enabled) R.string.enabled_message else R.string.disabled_message)
        findViewById<Button>(R.id.startButton).text =
            getText(if (enabled) R.string.open_dev_options else R.string.start)
    }

    /** Turns each <annotation color="error|success"> span into a theme-aware colored span. */
    private fun coloredAnnotations(resId: Int): CharSequence {
        // Markup-free strings (e.g. a translation dropping the tag) aren't SpannedString.
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

    // e.g. "Google Pixel 7". MODEL sometimes already includes the manufacturer, so
    // avoid repeating it; MANUFACTURER is often lower-case (e.g. "samsung").
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
        // Fires when returning from About phone. Once the screen is reachable
        // (Vivo: after the taps, before the toggle), opening it takes over.
        if (isDeveloperOptionsEnabled(this) && openDeveloperOptions(this)) {
            finish()
            return
        }
        updateUiForState()
    }
}
