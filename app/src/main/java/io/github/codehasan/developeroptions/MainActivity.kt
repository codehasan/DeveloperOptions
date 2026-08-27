package io.github.codehasan.developeroptions

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isDeveloperOptionsEnabled(this)) {
            openDeveloperOptions(this)
        } else {
            startActivity(Intent(this, DisabledActivity::class.java))
        }
        finish()
    }
}
