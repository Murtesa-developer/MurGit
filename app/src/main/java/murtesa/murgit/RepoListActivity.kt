package murtesa.murgit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RepoListActivity : AppCompatActivity() {

    private lateinit var rvRepos: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAddRepo: FloatingActionButton
    private lateinit var searchView: SearchView
    private lateinit var securePrefs: SecurePreferences
    private lateinit var adapter: ReposAdapter
    
    private var userRepos: List<Repository> = emptyList()
    private var externalMonitoredRepos: MutableList<Repository> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repo_list)

        securePrefs = SecurePreferences(this)
        
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvRepos = findViewById(R.id.rvRepos)
        progressBar = findViewById(R.id.progressBar)
        fabAddRepo = findViewById(R.id.fabAddRepo)
        searchView = findViewById(R.id.searchView)

        adapter = ReposAdapter(
            repos = emptyList(),
            securePrefs = securePrefs,
            onRepoClick = { repo ->
                val intent = Intent(this, CommitListActivity::class.java)
                intent.putExtra("repo_name", repo.name)
                intent.putExtra("owner", repo.owner.login)
                startActivity(intent)
            },
            onMonitorToggle = { repo, isMonitored ->
                if (isMonitored) {
                    securePrefs.addMonitoredRepo(repo.fullName)
                    Toast.makeText(this, "Monitoring ${repo.name}", Toast.LENGTH_SHORT).show()
                } else {
                    securePrefs.removeMonitoredRepo(repo.fullName)
                    Toast.makeText(this, "Stopped monitoring ${repo.name}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        rvRepos.layoutManager = LinearLayoutManager(this)
        rvRepos.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })

        fabAddRepo.setOnClickListener { showAddRepoDialog() }

        loadRepos()
    }

    private fun loadRepos() {
        progressBar.visibility = View.VISIBLE
        val token = securePrefs.getToken() ?: return
        val authHeader = "Bearer $token"

        ApiClient.apiService.getRepositories(authHeader).enqueue(object : Callback<List<Repository>> {
            override fun onResponse(call: Call<List<Repository>>, response: Response<List<Repository>>) {
                if (response.isSuccessful) {
                    userRepos = response.body() ?: emptyList()
                    loadExternalMonitoredRepos()
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@RepoListActivity, "Failed to load repos: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@RepoListActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadExternalMonitoredRepos() {
        val externalNames = securePrefs.getExternalRepos()
        
        if (externalNames.isEmpty()) {
            updateFullList()
            return
        }

        val token = securePrefs.getToken() ?: return
        val authHeader = "Bearer $token"
        var loadedCount = 0
        externalMonitoredRepos.clear()

        externalNames.forEach { fullName ->
            val parts = fullName.split("/")
            if (parts.size == 2) {
                ApiClient.apiService.getRepository(authHeader, parts[0], parts[1]).enqueue(object : Callback<Repository> {
                    override fun onResponse(call: Call<Repository>, response: Response<Repository>) {
                        if (response.isSuccessful) {
                            response.body()?.let { externalMonitoredRepos.add(it) }
                        }
                        checkComplete()
                    }

                    override fun onFailure(call: Call<Repository>, t: Throwable) {
                        checkComplete()
                    }

                    private fun checkComplete() {
                        loadedCount++
                        if (loadedCount == externalNames.size) {
                            updateFullList()
                        }
                    }
                })
            } else {
                loadedCount++
                if (loadedCount == externalNames.size) updateFullList()
            }
        }
    }

    private fun updateFullList() {
        progressBar.visibility = View.GONE
        val fullList = (userRepos + externalMonitoredRepos).distinctBy { it.fullName }
        adapter.updateData(fullList)
        adapter.filter(searchView.query.toString())
    }

    private fun showAddRepoDialog() {
        val editText = EditText(this)
        editText.hint = "owner/repository or full link"
        
        AlertDialog.Builder(this)
            .setTitle("Add External Repository")
            .setMessage("Monitoring for external repositories is enabled by default once added.")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val input = editText.text.toString().trim()
                val (owner, repoName) = parseGithubInput(input)
                if (owner != null && repoName != null) {
                    fetchAndAddExternalRepo(owner, repoName)
                } else {
                    Toast.makeText(this, "Invalid format. Use owner/repo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun parseGithubInput(input: String): Pair<String?, String?> {
        var clean = input.removePrefix("https://").removePrefix("http://").removePrefix("www.")
        if (clean.startsWith("github.com/")) {
            clean = clean.removePrefix("github.com/")
        }
        val parts = clean.split("/")
        return if (parts.size >= 2) Pair(parts[0], parts[1]) else Pair(null, null)
    }

    private fun fetchAndAddExternalRepo(owner: String, repoName: String) {
        progressBar.visibility = View.VISIBLE
        val token = securePrefs.getToken() ?: return
        val authHeader = "Bearer $token"

        ApiClient.apiService.getRepository(authHeader, owner, repoName).enqueue(object : Callback<Repository> {
            override fun onResponse(call: Call<Repository>, response: Response<Repository>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val repo = response.body()
                    if (repo != null) {
                        securePrefs.addExternalRepo(repo.fullName)
                        if (!userRepos.any { it.fullName == repo.fullName }) {
                            if (!externalMonitoredRepos.any { it.fullName == repo.fullName }) {
                                externalMonitoredRepos.add(repo)
                            }
                        }
                        updateFullList()
                        Toast.makeText(this@RepoListActivity, "Monitoring ${repo.fullName}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@RepoListActivity, "Repository not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Repository>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@RepoListActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
