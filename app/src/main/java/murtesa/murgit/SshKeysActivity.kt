package murtesa.murgit

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SshKeysActivity : AppCompatActivity() {

    private lateinit var rvSshKeys: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var fabAdd: ExtendedFloatingActionButton
    private lateinit var fabLogout: FloatingActionButton
    private lateinit var btnViewRepos: Button
    private lateinit var btnViewLogs: Button
    private lateinit var securePrefs: SecurePreferences
    private lateinit var adapter: SshKeysAdapter
    private var sshKeys = mutableListOf<SshKey>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ssh_keys)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        securePrefs = SecurePreferences(this)
        NotificationHelper.createNotificationChannel(this)

        rvSshKeys = findViewById(R.id.rvSshKeys)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        fabAdd = findViewById(R.id.fabAdd)
        fabLogout = findViewById(R.id.fabLogout)
        btnViewRepos = findViewById(R.id.btnViewRepos)
        btnViewLogs = findViewById(R.id.btnViewLogs)

        adapter = SshKeysAdapter(sshKeys) { key ->
            showDeleteConfirmationDialog(key)
        }
        rvSshKeys.layoutManager = LinearLayoutManager(this)
        rvSshKeys.adapter = adapter

        fabAdd.setOnClickListener { showAddKeyDialog() }
        fabLogout.setOnClickListener { showLogoutDialog() }
        
        btnViewRepos.setOnClickListener {
            startActivity(Intent(this, RepoListActivity::class.java))
        }

        btnViewLogs.setOnClickListener {
            startActivity(Intent(this, EventsActivity::class.java))
        }

        // Start Background Monitor Service
        val serviceIntent = Intent(this, GithubMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        loadSshKeys()
    }

    private fun loadSshKeys() {
        progressBar.visibility = View.VISIBLE
        val token = securePrefs.getToken() ?: return
        val authHeader = "Bearer $token"

        ApiClient.apiService.getSshKeys(authHeader).enqueue(object : Callback<List<SshKey>> {
            override fun onResponse(call: Call<List<SshKey>>, response: Response<List<SshKey>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val keys = response.body() ?: emptyList()
                    sshKeys.clear()
                    sshKeys.addAll(keys)
                    securePrefs.saveSshKeysCount(sshKeys.size)
                    adapter.updateData(sshKeys)
                    tvEmpty.visibility = if (sshKeys.isEmpty()) View.VISIBLE else View.GONE
                }
            }
            override fun onFailure(call: Call<List<SshKey>>, t: Throwable) {
                progressBar.visibility = View.GONE
            }
        })
    }

    private fun showAddKeyDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_ssh_key, null)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etTitle)
        val etPublicKey = dialogView.findViewById<TextInputEditText>(R.id.etPublicKey)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add SSH Key")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val title = etTitle.text.toString().trim()
                val publicKey = etPublicKey.text.toString().trim()
                if (title.isNotEmpty() && publicKey.isNotEmpty()) {
                    addSshKey(title, publicKey, dialog)
                }
            }
        }
        dialog.show()
    }

    private fun addSshKey(title: String, publicKey: String, dialog: Dialog) {
        progressBar.visibility = View.VISIBLE
        val token = securePrefs.getToken() ?: return
        val authHeader = "Bearer $token"
        val request = CreateSshKeyRequest(title, publicKey)

        ApiClient.apiService.createSshKey(authHeader, request).enqueue(object : Callback<SshKey> {
            override fun onResponse(call: Call<SshKey>, response: Response<SshKey>) {
                if (response.isSuccessful) {
                    loadSshKeys()
                    dialog.dismiss()
                }
            }
            override fun onFailure(call: Call<SshKey>, t: Throwable) {
                progressBar.visibility = View.GONE
            }
        })
    }

    private fun showDeleteConfirmationDialog(key: SshKey) {
        AlertDialog.Builder(this)
            .setTitle("Delete SSH Key")
            .setMessage("Delete \"${key.title ?: "Key"}\"?")
            .setPositiveButton("Delete") { _, _ -> deleteSshKey(key) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSshKey(key: SshKey) {
        progressBar.visibility = View.VISIBLE
        val token = securePrefs.getToken() ?: return
        val authHeader = "Bearer $token"
        ApiClient.apiService.deleteSshKey(authHeader, key.id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) loadSshKeys()
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                progressBar.visibility = View.GONE
            }
        })
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure?")
            .setPositiveButton("Logout") { _, _ -> logout() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logout() {
        securePrefs.clear()
        stopService(Intent(this, GithubMonitorService::class.java))
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
