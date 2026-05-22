package murtesa.murgit

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import com.google.gson.annotations.SerializedName

interface GitHubApiService {

    @GET("user")
    fun getUser(
        @Header("Authorization") authorization: String
    ): Call<User>

    @GET("user/keys")
    fun getSshKeys(
        @Header("Authorization") authorization: String,
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Call<List<SshKey>>

    @GET("user/ssh_signing_keys")
    fun getSshSigningKeys(
        @Header("Authorization") authorization: String,
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Call<List<SshSigningKey>>

    @POST("user/keys")
    fun createSshKey(
        @Header("Authorization") authorization: String,
        @Body request: CreateSshKeyRequest
    ): Call<SshKey>

    @DELETE("user/keys/{key_id}")
    fun deleteSshKey(
        @Header("Authorization") authorization: String,
        @Path("key_id") keyId: Int
    ): Call<Void>

    // --- Repositories ---
    @GET("user/repos")
    fun getRepositories(
        @Header("Authorization") authorization: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100
    ): Call<List<Repository>>

    @GET("repos/{owner}/{repo}")
    fun getRepository(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Call<Repository>

    // --- Commits ---
    @GET("repos/{owner}/{repo}/commits")
    fun getCommits(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 50,
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Call<List<CommitResponse>>

    @GET("repos/{owner}/{repo}/commits/{ref}")
    fun getCommit(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("ref") ref: String
    ): Call<CommitResponse>

    // --- Events (Audit Log equivalent for personal accounts) ---
    @GET("users/{username}/events")
    fun getUserEvents(
        @Header("Authorization") authorization: String,
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 30,
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Call<List<GithubEvent>>
}

data class CreateSshKeyRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("key")
    val key: String
)
