package murtesa.murgit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommitDetailActivity : AppCompatActivity() {

    private lateinit var tvCommitMessage: TextView
    private lateinit var tvAuthorInfo: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvSha: TextView
    private lateinit var btnViewOnGithub: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var securePrefs: SecurePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_commit_detail)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tvCommitMessage = findViewById(R.id.tvCommitMessage)
        tvAuthorInfo = findViewById(R.id.tvAuthorInfo)
        tvDate = findViewById(R.id.tvDate)
        tvSha = findViewById(R.id.tvSha)
        btnViewOnGithub = findViewById(R.id.btnViewOnGithub)
        progressBar = findViewById(R.id.progressBar)
        securePrefs = SecurePreferences(this)

        val owner = intent.getStringExtra("owner")
        val repo = intent.getStringExtra("repo_name")
        val sha = intent.getStringExtra("sha")

        if (owner != null && repo != null && sha != null) {
            loadCommitDetails(owner, repo, sha)
        } else {
            Toast.makeText(this, "Error: Missing commit info", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadCommitDetails(owner: String, repo: String, sha: String) {
        progressBar.visibility = View.VISIBLE
        val token = securePrefs.getToken() ?: return
        val authHeader = "Bearer $token"

        // Reuse existing service or add specific method if needed
        // For now, we'll fetch the commit via a generic call if possible or assume we might need a new API method
        // Let's check GitHubApiService first.
        
        ApiClient.apiService.getCommits(authHeader, owner, repo, 50).enqueue(object : Callback<List<CommitResponse>> {
            override fun onResponse(call: Call<List<CommitResponse>>, response: Response<List<CommitResponse>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val commit = response.body()?.find { it.sha == sha }
                    if (commit != null) {
                        displayCommit(commit)
                    } else {
                        Toast.makeText(this@CommitDetailActivity, "Commit not found in latest history", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<List<CommitResponse>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@CommitDetailActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayCommit(commit: CommitResponse) {
        tvCommitMessage.text = commit.commitDetail.message
        tvAuthorInfo.text = "Author: ${commit.commitDetail.commitAuthor.name} (${commit.commitDetail.commitAuthor.email})"
        tvDate.text = "Date: ${commit.commitDetail.commitAuthor.date}"
        tvSha.text = "SHA: ${commit.sha}"
        
        btnViewOnGithub.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(commit.htmlUrl))
            startActivity(intent)
        }
    }
}
