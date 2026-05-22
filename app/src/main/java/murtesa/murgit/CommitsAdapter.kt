package murtesa.murgit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CommitsAdapter(
    private var commits: List<CommitResponse>,
    private val onCommitClick: (CommitResponse) -> Unit
) : RecyclerView.Adapter<CommitsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_commit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val commit = commits[position]
        holder.tvMessage.text = commit.commitDetail.message
        holder.tvAuthor.text = "${commit.commitDetail.commitAuthor.name} on ${commit.commitDetail.commitAuthor.date}"
        holder.itemView.setOnClickListener { onCommitClick(commit) }
    }

    override fun getItemCount() = commits.size

    fun updateData(newCommits: List<CommitResponse>) {
        commits = newCommits
        notifyDataSetChanged()
    }
}
