package io.github.codehasan.developeroptions

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Jump straight to the system screen only when it's enabled and actually
        // opens; otherwise hand off to the guidance screen.
        if (!isDeveloperOptionsEnabled(this) || !openDeveloperOptions(this)) {
            startActivity(Intent(this, DisabledActivity::class.java))
        }
        finish()
    }
}
