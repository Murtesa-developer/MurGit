package murtesa.murgit

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import murtesa.murgit.databinding.ActivityEventsBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EventsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventsBinding
    private lateinit var securePrefs: SecurePreferences
    private lateinit var adapter: EventsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        securePrefs = SecurePreferences(this)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = EventsAdapter(emptyList())
        binding.rvEvents.layoutManager = LinearLayoutManager(this)
        binding.rvEvents.adapter = adapter

        loadEvents()
    }

    private fun loadEvents() {
        binding.progressBar.visibility = View.VISIBLE
        val token = securePrefs.getToken() ?: return
        val username = securePrefs.getUsername() ?: return
        val authHeader = "Bearer $token"

        ApiClient.apiService.getUserEvents(authHeader, username).enqueue(object : Callback<List<GithubEvent>> {
            override fun onResponse(call: Call<List<GithubEvent>>, response: Response<List<GithubEvent>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    adapter.updateData(response.body() ?: emptyList())
                } else {
                    Toast.makeText(this@EventsActivity, "Failed to load events", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<GithubEvent>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@EventsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
