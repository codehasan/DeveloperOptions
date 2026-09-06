package io.github.codehasan.developeroptions

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Opening succeeds when the screen is reachable (enabled on stock, or just
        // reachable-but-off on Vivo — either way, take the user there).
        if (!isDeveloperOptionsEnabled(this) || !openDeveloperOptions(this)) {
            startActivity(Intent(this, DisabledActivity::class.java))
        }
        finish()
    }
}
