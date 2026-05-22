// MainActivity.kt - Updated with navigation logic
package murtesa.murgit

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var securePrefs: SecurePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        securePrefs = SecurePreferences(this)

        // Check if user is already logged in
        val token = securePrefs.getToken()

        if (token != null && token.isNotEmpty()) {
            // User is logged in, go to SSH Keys screen
            startActivity(Intent(this, SshKeysActivity::class.java))
            finish()
        } else {
            // User not logged in, show login screen
            enableEdgeToEdge()
            setContentView(R.layout.activity_login)
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }
}