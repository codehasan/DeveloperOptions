package io.github.codehasan.developeroptions

import android.os.Build
import android.os.Bundle
import android.text.Annotation
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
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

        findViewById<TextView>(R.id.deviceText).apply {
            text = deviceName()
            setOnClickListener { openAboutPhone(context) }
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (isDeveloperOptionsEnabled(this)) {
                if (openDeveloperOptions(this) ||
                    openSystem(this) ||
                    openSettings(this)
                ) finish()
            } else {
                // Unconditional: the disabled-stub name match can false-negative if an
                // OEM renames it, so About phone must not hinge on openDeveloperOptions.
                openAboutPhone(this)
                // Fix for OEMs who only enable the Activity,
                // but doesn't change the Settings value
                openDeveloperOptions(this)
            }
        }
    }

    private fun updateUiForState() {
        val enabled = isDeveloperOptionsEnabled(this)
        findViewById<TextView>(R.id.titleText).text =
            coloredAnnotations(if (enabled) R.string.enabled_title else R.string.disabled_title)

        val message = findViewById<TextView>(R.id.messageText)
        val steps = findViewById<LinearLayout>(R.id.stepsContainer)
        if (enabled) {
            message.text = getText(R.string.enabled_message)
            message.visibility = TextView.VISIBLE
            steps.visibility = LinearLayout.GONE
        } else {
            message.visibility = TextView.GONE
            steps.visibility = LinearLayout.VISIBLE
            populateSteps(steps)
        }

        findViewById<Button>(R.id.startButton).text =
            getText(if (enabled) R.string.open_dev_options else R.string.start)
        applyStateVisuals(enabled)
    }

    /** Washes the screen with a faint tint keyed to the ON/OFF state. */
    private fun applyStateVisuals(enabled: Boolean) {
        findViewById<android.view.View>(R.id.main)
            .setBackgroundColor(color(if (enabled) R.color.tint_on else R.color.tint_off))
    }

    /** Rebuilds the numbered, OEM-tailored step cards for turning Developer options on. */
    private fun populateSteps(container: LinearLayout) {
        val hint = devOptionsHint(this)
        val steps = listOf(
            getString(R.string.dev_options_step_open, hint.aboutPage),
            getString(R.string.dev_options_step_tap, hint.buildField),
            getString(R.string.dev_options_step_return),
        )
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)
        steps.forEachIndexed { i, step ->
            val row = inflater.inflate(R.layout.item_step, container, false)
            row.findViewById<TextView>(R.id.stepNumber).text = (i + 1).toString()
            row.findViewById<TextView>(R.id.stepText).text =
                HtmlCompat.fromHtml(step, HtmlCompat.FROM_HTML_MODE_LEGACY)
            container.addView(row)
        }
    }

    private fun color(resId: Int) = ContextCompat.getColor(this, resId)

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
