// LoginActivity.kt - Robust error handling, connectivity checks, and user info fetching
package murtesa.murgit

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var etToken: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnHowToGet: Button
    private lateinit var tvError: TextView
    private lateinit var securePrefs: SecurePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        securePrefs = SecurePreferences(this)

        // If already logged in, skip to main activity
        if (!securePrefs.getToken().isNullOrEmpty()) {
            navigateToMain()
            return
        }

        etToken = findViewById(R.id.etToken)
        btnLogin = findViewById(R.id.btnLogin)
        btnHowToGet = findViewById(R.id.btnHowToGet)
        tvError = findViewById(R.id.tvError)

        btnLogin.setOnClickListener {
            val token = etToken.text.toString().trim()
            if (token.isEmpty()) {
                showError("Please enter your GitHub token")
                return@setOnClickListener
            }
            
            if (!isNetworkAvailable()) {
                showError("No internet connection. Please check your network settings.")
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            validateToken(token)
        }

        btnHowToGet.setOnClickListener {
            showInstructionsDialog()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun validateToken(token: String) {
        btnLogin.isEnabled = false
        btnLogin.text = "Verifying..."

        val authHeader = "Bearer $token"

        // Fetch user info to get the username
        ApiClient.apiService.getUser(authHeader).enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                btnLogin.isEnabled = true
                btnLogin.text = "Verify & Login"

                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        securePrefs.saveToken(token)
                        securePrefs.saveUsername(user.login)
                        Toast.makeText(this@LoginActivity, "Welcome, ${user.login}!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    } else {
                        showError("Failed to retrieve user information.")
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Invalid token. Please check your token and try again."
                        403 -> "Access forbidden. Check if your token has 'read:user' scope or if rate limit is exceeded."
                        404 -> "GitHub API endpoint not found. Please check your connection."
                        else -> "GitHub Error: ${response.code()} - ${response.message()}"
                    }
                    showError(errorMsg)
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                btnLogin.isEnabled = true
                btnLogin.text = "Verify & Login"
                
                if (t is IOException) {
                    showError("Connection failed. Please check your internet connection and try again.")
                } else {
                    showError("Unexpected error: ${t.localizedMessage}")
                }
            }
        })
    }

    private fun navigateToMain() {
        startActivity(Intent(this, SshKeysActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun showInstructionsDialog() {
        val message = """
            How to get a GitHub Classic Token:
            
            1. Go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
            
            2. Click "Generate new token (classic)"
            
            3. Select scopes:
               ✓ admin:public_key (Recommended)
               OR at minimum: read:public_key AND read:user
            
            4. Copy and paste the token here.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("GitHub Token Help")
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .setNegativeButton("Open Settings") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse("https://github.com/settings/tokens")
                startActivity(intent)
            }
            .show()
    }
}
