package murtesa.murgit

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GithubMonitorWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val securePrefs = SecurePreferences(context)
    private val TAG = "GithubMonitorWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val token = securePrefs.getToken()
        val username = securePrefs.getUsername()

        if (token.isNullOrEmpty() || username.isNullOrEmpty()) {
            return@withContext Result.failure()
        }

        val authHeader = "Bearer $token"

        try {
            // 1. Monitor SSH Keys for changes
            monitorSshKeys(authHeader)

            // 2. Monitor User Events (Audit Log equivalent)
            monitorUserEvents(authHeader, username)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in background monitoring", e)
            Result.retry()
        }
    }

    private fun monitorSshKeys(authHeader: String) {
        val response = ApiClient.apiService.getSshKeys(authHeader).execute()
        if (response.isSuccessful) {
            val currentKeys = response.body() ?: emptyList()
            val savedKeysCount = securePrefs.getSshKeysCount()

            if (savedKeysCount != -1 && currentKeys.size != savedKeysCount) {
                val message = if (currentKeys.size > savedKeysCount) {
                    "A new SSH key was added to your account."
                } else {
                    "An SSH key was removed from your account."
                }
                
                NotificationHelper.sendNotification(
                    applicationContext,
                    "SSH Key Change Detected",
                    message,
                    Intent(applicationContext, SshKeysActivity::class.java)
                )
            }
            securePrefs.saveSshKeysCount(currentKeys.size)
        }
    }

    private fun monitorUserEvents(authHeader: String, username: String) {
        val response = ApiClient.apiService.getUserEvents(authHeader, username).execute()
        if (response.isSuccessful) {
            val events = response.body() ?: emptyList()
            if (events.isNotEmpty()) {
                val lastSeenEventId = securePrefs.getLastSeenEventId()
                val latestEvent = events[0]

                if (lastSeenEventId != null && latestEvent.id != lastSeenEventId) {
                    // New event detected
                    val intent = Intent(applicationContext, RepoListActivity::class.java) // Default to RepoList
                    
                    NotificationHelper.sendNotification(
                        applicationContext,
                        "New GitHub Activity: ${latestEvent.type}",
                        "Activity detected in ${latestEvent.repo?.name ?: "your account"}",
                        intent
                    )
                }
                securePrefs.saveLastSeenEventId(latestEvent.id)
            }
        }
    }
}
