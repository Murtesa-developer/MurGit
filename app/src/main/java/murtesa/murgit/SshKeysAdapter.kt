package murtesa.murgit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class SshKeysAdapter(
    private var keys: List<SshKey>,
    private val onDeleteClick: (SshKey) -> Unit
) : RecyclerView.Adapter<SshKeysAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvKeyPreview: TextView = itemView.findViewById(R.id.tvKeyPreview)
        val tvCreatedAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
        val tvLastUsed: TextView = itemView.findViewById(R.id.tvLastUsed)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ssh_key, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val key = keys[position]

        holder.tvTitle.text = key.title ?: "Unnamed Key"
        holder.tvKeyPreview.text = if (key.key.length > 40) key.key.take(40) + "..." else key.key
        
        holder.tvCreatedAt.text = "Added: ${formatDate(key.createdAt)}"

        if (!key.lastUsed.isNullOrEmpty()) {
            holder.tvLastUsed.text = "Last used: ${formatDate(key.lastUsed)}"
            holder.tvLastUsed.visibility = View.VISIBLE
        } else {
            holder.tvLastUsed.text = "Never used"
            holder.tvLastUsed.visibility = View.VISIBLE
        }

        holder.btnDelete.setOnClickListener { onDeleteClick(key) }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(dateString)
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            dateString
        }
    }

    override fun getItemCount(): Int = keys.size

    fun updateData(newKeys: List<SshKey>) {
        keys = newKeys
        notifyDataSetChanged()
    }
}
