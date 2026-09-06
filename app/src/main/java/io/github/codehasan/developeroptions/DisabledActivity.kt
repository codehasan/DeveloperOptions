package io.github.codehasan.developeroptions

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val retryHandler = Handler(Looper.getMainLooper())
    private var openAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_disabled)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<TextView>(R.id.titleText).text = buildTitleWithColoredAnnotations()

        findViewById<TextView>(R.id.deviceText).text = deviceName()

        findViewById<Button>(R.id.startButton).setOnClickListener {
            openAboutPhone(this)
        }
    }

    /**
     * Reads the disabled_title string as markup and turns each
     * <annotation color="error"> span into a theme-aware colored span.
     */
    private fun buildTitleWithColoredAnnotations(): CharSequence {
        // A string without markup comes back as a plain String, not SpannedString
        // (e.g. a translation that drops the <annotation> tag) — fall back safely.
        val title = getText(R.string.disabled_title) as? SpannedString
            ?: return getText(R.string.disabled_title)
        val builder = SpannableStringBuilder(title)
        for (annotation in title.getSpans(0, title.length, Annotation::class.java)) {
            if (annotation.key == "color" && annotation.value == "error") {
                builder.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_error)),
                    title.getSpanStart(annotation),
                    title.getSpanEnd(annotation),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
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
        // user presses back to close the About phone page. Re-run the check:
        // if Developer options is now enabled, jump straight to it; otherwise
        // stay on this screen so the user can try again.
        if (isDeveloperOptionsEnabled(this)) {
            openAttempts = 0
            attemptOpenDeveloperOptions()
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop retrying while we're not on screen (e.g. once the settings screen
        // has opened, or the user left).
        retryHandler.removeCallbacksAndMessages(null)
    }

    /**
     * Developer options can read as enabled a moment before the system registers
     * its settings screen, so a single launch attempt right after the user flips
     * it on often finds no activity to handle the intent. Retry a few times over
     * ~2s before giving up and opening the top-level Settings as a last resort.
     */
    private fun attemptOpenDeveloperOptions() {
        if (openDeveloperOptions(this)) {
            finish()
            return
        }
        if (openAttempts++ < MAX_OPEN_ATTEMPTS) {
            retryHandler.postDelayed(::attemptOpenDeveloperOptions, RETRY_DELAY_MS)
        } else {
            openSettings(this)
            finish()
        }
    }

    companion object {
        private const val MAX_OPEN_ATTEMPTS = 10
        private const val RETRY_DELAY_MS = 200L
    }
}
