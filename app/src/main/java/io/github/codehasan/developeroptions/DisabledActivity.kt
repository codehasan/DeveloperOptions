package io.github.codehasan.developeroptions

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

        findViewById<Button>(R.id.startButton).setOnClickListener {
            openAboutPhone(this)
        }
    }

    override fun onResume() {
        super.onResume()
        // Runs every time this screen becomes visible again, including when the
        // user presses back to close the About phone page. Re-run the check:
        // if Developer options is now enabled, jump straight to it; otherwise
        // stay on this screen so the user can try again.
        if (isDeveloperOptionsEnabled(this)) {
            openDeveloperOptions(this)
            finish()
        }
    }
}
