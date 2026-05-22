// SshKey.kt - Make sure this file exists with proper imports
package murtesa.murgit

import com.google.gson.annotations.SerializedName

data class SshKey(
    @SerializedName("id")
    val id: Int,

    @SerializedName("key")
    val key: String,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("last_used")
    val lastUsed: String? = null
)