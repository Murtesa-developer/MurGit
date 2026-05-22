package murtesa.murgit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ReposAdapter(
    private var repos: List<Repository>,
    private val securePrefs: SecurePreferences,
    private val onRepoClick: (Repository) -> Unit,
    private val onMonitorToggle: (Repository, Boolean) -> Unit
) : RecyclerView.Adapter<ReposAdapter.ViewHolder>() {

    private var filteredRepos: List<Repository> = repos

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvRepoName)
        val tvDescription: TextView = view.findViewById(R.id.tvRepoDescription)
        val tvPrivateBadge: TextView = view.findViewById(R.id.tvPrivateBadge)
        val btnMonitor: Button = view.findViewById(R.id.btnMonitor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_repo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val repo = filteredRepos[position]
        holder.tvName.text = repo.fullName
        holder.tvDescription.text = repo.description ?: "No description"
        
        holder.tvPrivateBadge.visibility = if (repo.isPrivate) View.VISIBLE else View.GONE

        updateMonitorButton(holder.btnMonitor, securePrefs.isRepoMonitored(repo.fullName))

        holder.btnMonitor.setOnClickListener {
            val currentlyMonitored = securePrefs.isRepoMonitored(repo.fullName)
            val newState = !currentlyMonitored
            onMonitorToggle(repo, newState)
            updateMonitorButton(holder.btnMonitor, newState)
        }

        holder.itemView.setOnClickListener { onRepoClick(repo) }
    }

    private fun updateMonitorButton(button: Button, isMonitored: Boolean) {
        if (isMonitored) {
            button.text = "Unmonitor"
            button.alpha = 0.6f
        } else {
            button.text = "Monitor"
            button.alpha = 1.0f
        }
    }

    override fun getItemCount() = filteredRepos.size

    fun updateData(newRepos: List<Repository>) {
        repos = newRepos
        filteredRepos = newRepos
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        filteredRepos = if (lowerCaseQuery.isEmpty()) {
            repos
        } else {
            repos.filter {
                it.fullName.lowercase(Locale.getDefault()).contains(lowerCaseQuery) ||
                (it.description?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true)
            }
        }
        notifyDataSetChanged()
    }
}
