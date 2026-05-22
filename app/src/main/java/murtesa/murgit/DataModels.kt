package murtesa.murgit

import com.google.gson.annotations.SerializedName

data class Repository(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("owner") val owner: User,
    @SerializedName("description") val description: String?,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("stargazers_count") val stargazersCount: Int,
    @SerializedName("forks_count") val forksCount: Int,
    @SerializedName("language") val language: String?,
    @SerializedName("private") val isPrivate: Boolean
)

data class CommitResponse(
    @SerializedName("sha") val sha: String,
    @SerializedName("commit") val commitDetail: CommitDetail,
    @SerializedName("author") val author: User?,
    @SerializedName("html_url") val htmlUrl: String
)

data class CommitDetail(
    @SerializedName("author") val commitAuthor: CommitAuthor,
    @SerializedName("message") val message: String
)

data class CommitAuthor(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("date") val date: String
)

data class GithubEvent(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("actor") val actor: User,
    @SerializedName("repo") val repo: EventRepo?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("payload") val payload: com.google.gson.JsonObject?
)

data class EventRepo(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String?
)
