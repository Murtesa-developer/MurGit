// SshSigningKey.kt - Data model for SSH signing keys
package murtesa.murgit

import com.google.gson.annotations.SerializedName

data class SshSigningKey(
    @SerializedName("id")
    val id: Int,

    @SerializedName("key")
    val key: String,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("created_at")
    val createdAt: String
)
