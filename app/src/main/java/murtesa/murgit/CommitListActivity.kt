package murtesa.murgit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import murtesa.murgit.databinding.ActivityCommitListBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommitListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommitListBinding
    private lateinit var securePrefs: SecurePreferences
    private lateinit var adapter: CommitsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommitListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        securePrefs = SecurePreferences(this)
        
        val repoName = intent.getStringExtra("repo_name") ?: ""
        val owner = intent.getStringExtra("owner") ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Commits: $repoName"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = CommitsAdapter(emptyList()) { commit ->
            val intent = Intent(this, CommitDetailActivity::class.java).apply {
                putExtra("repo_name", repoName)
                putExtra("owner", owner)
                putExtra("sha", commit.sha)
            }
            startActivity(intent)
        }

        binding.rvCommits.layoutManager = LinearLayoutManager(this)
        binding.rvCommits.adapter = adapter

        if (repoName.isNotEmpty() && owner.isNotEmpty()) {
            loadCommits(owner, repoName)
        }
    }

    private fun loadCommits(owner: String, repo: String) {
        binding.progressBar.visibility = View.VISIBLE
        val token = securePrefs.getToken() ?: return
        val authHeader = "Bearer $token"

        ApiClient.apiService.getCommits(authHeader, owner, repo).enqueue(object : Callback<List<CommitResponse>> {
            override fun onResponse(call: Call<List<CommitResponse>>, response: Response<List<CommitResponse>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    adapter.updateData(response.body() ?: emptyList())
                } else {
                    Toast.makeText(this@CommitListActivity, "Failed to load commits", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<CommitResponse>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@CommitListActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
