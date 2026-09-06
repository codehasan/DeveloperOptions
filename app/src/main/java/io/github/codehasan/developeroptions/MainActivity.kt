package io.github.codehasan.developeroptions

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If Developer options is on and its screen opens, we're done. Otherwise
        // (off, or on but its screen isn't resolvable yet) show DisabledActivity,
        // which guides the user and retries opening the screen.
        if (!isDeveloperOptionsEnabled(this) || !openDeveloperOptions(this)) {
            startActivity(Intent(this, DisabledActivity::class.java))
        }
        finish()
    }
}
