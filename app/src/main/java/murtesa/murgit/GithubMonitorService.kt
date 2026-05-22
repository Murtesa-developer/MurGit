package murtesa.murgit

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import retrofit2.Response
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class GithubMonitorService : Service() {

    private lateinit var securePrefs: SecurePreferences
    private val TAG = "GithubMonitorService"
    private var wakeLock: PowerManager.WakeLock? = null
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var scheduledTask: ScheduledFuture<*>? = null

    // We start with 30s. If rate limit is low, we back off.
    private var currentIntervalSeconds = 30L

    override fun onCreate() {
        super.onCreate()
        securePrefs = SecurePreferences(this)
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Murgit:MonitorWakeLock")
        // Acquire with a long timeout, we renew it in the task
        wakeLock?.acquire(24 * 60 * 60 * 1000L)
        
        startForegroundService()
    }

    private fun startForegroundService() {
        NotificationHelper.createNotificationChannel(this)
        // This is the minimized "running in background" notification
        val notification = NotificationHelper.getForegroundNotification(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduleTask()
        return START_STICKY
    }

    private fun scheduleTask() {
        scheduledTask?.cancel(false)
        scheduledTask = executor.scheduleWithFixedDelay({
            try {
                performMonitoringWork()
            } catch (e: Exception) {
                Log.e(TAG, "Critical error in monitor loop", e)
            }
        }, 0, currentIntervalSeconds, TimeUnit.SECONDS)
    }

    private fun performMonitoringWork() {
        // Ensure CPU is awake
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(24 * 60 * 60 * 1000L)
        }

        val token = securePrefs.getToken() ?: return
        val authHeader = "Bearer $token"

        // 1. Check SSH Keys
        try {
            val etag = securePrefs.getEtag("ssh_keys")
            val response = ApiClient.apiService.getSshKeys(authHeader, etag).execute()
            handleRateLimitHeaders(response)

            if (response.code() == 304) {
                Log.d(TAG, "SSH Keys: 304 Not Modified")
            } else if (response.isSuccessful) {
                val currentKeys = response.body() ?: emptyList()
                val savedCount = securePrefs.getSshKeysCount()
                
                securePrefs.saveEtag("ssh_keys", response.headers()["ETag"])
                
                if (savedCount != -1 && currentKeys.size != savedCount) {
                    val msg = if (currentKeys.size > savedCount) "New SSH key detected!" else "An SSH key was removed from your account."
                    NotificationHelper.sendNotification(this, "Security Alert", msg, Intent(this, SshKeysActivity::class.java))
                }
                securePrefs.saveSshKeysCount(currentKeys.size)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking SSH keys", e)
        }

        // 2. Fetch All Repositories & Identify Monitored Ones (Default: Enable All)
        val allRepoFullNames = mutableSetOf<String>()
        try {
            val response = ApiClient.apiService.getRepositories(authHeader).execute()
            handleRateLimitHeaders(response)
            
            if (response.isSuccessful) {
                val repos = response.body() ?: emptyList()
                val currentRepoFullNames = repos.map { it.fullName }.toSet()
                allRepoFullNames.addAll(currentRepoFullNames)
                
                val knownRepoFullNames = securePrefs.getKnownRepos()
                if (knownRepoFullNames.isNotEmpty()) {
                    val newRepos = currentRepoFullNames.filter { !knownRepoFullNames.contains(it) }
                    for (repoFullName in newRepos) {
                        Log.i(TAG, "New repository detected: $repoFullName. Monitored by default.")
                        NotificationHelper.sendNotification(
                            this,
                            "New Repository Detected",
                            "$repoFullName is now being monitored.",
                            Intent(this, RepoListActivity::class.java)
                        )
                    }
                }
                securePrefs.saveKnownRepos(currentRepoFullNames)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for repositories", e)
        }

        // 3. Check Monitored Repos
        val monitoredRepos = securePrefs.getMonitoredRepos(allRepoFullNames)

        monitoredRepos.forEach { repoFullName ->
            try {
                val parts = repoFullName.split("/")
                if (parts.size == 2) {
                    val owner = parts[0]
                    val repoName = parts[1]
                    val etag = securePrefs.getEtag(repoFullName)
                    
                    val response = ApiClient.apiService.getCommits(authHeader, owner, repoName, 1, etag).execute()
                    handleRateLimitHeaders(response)

                    if (response.code() == 304) {
                        Log.d(TAG, "Repo $repoName: 304 Not Modified")
                    } else if (response.isSuccessful) {
                        securePrefs.saveEtag(repoFullName, response.headers()["ETag"])
                        val latestCommit = response.body()?.firstOrNull()
                        if (latestCommit != null) {
                            val lastSha = securePrefs.getLastCommitSha(repoFullName)
                            if (lastSha != null && lastSha != latestCommit.sha) {
                                val intent = Intent(this, CommitDetailActivity::class.java).apply {
                                    putExtra("repo_name", repoName)
                                    putExtra("owner", owner)
                                    putExtra("sha", latestCommit.sha)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                
                                NotificationHelper.sendNotification(
                                    this,
                                    "New Commit: $repoName",
                                    "${latestCommit.commitDetail.commitAuthor.name}: ${latestCommit.commitDetail.message}",
                                    intent
                                )
                            }
                            securePrefs.saveLastCommitSha(repoFullName, latestCommit.sha)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking repo $repoFullName", e)
            }
        }
    }

    private fun handleRateLimitHeaders(response: Response<*>) {
        val remaining = response.headers()["X-RateLimit-Remaining"]?.toLongOrNull() ?: return
        val limit = response.headers()["X-RateLimit-Limit"]?.toLongOrNull() ?: return
        
        Log.d(TAG, "Rate Limit: $remaining / $limit")

        // If we have less than 10% of our limit left, slow down
        val threshold = limit / 10
        if (remaining < threshold && currentIntervalSeconds < 120) {
            currentIntervalSeconds = 120L
            Log.w(TAG, "Low rate limit! Throttling to 120s")
            scheduleTask() // Re-schedule with new interval
        } else if (remaining > threshold * 2 && currentIntervalSeconds > 30) {
            currentIntervalSeconds = 30L
            Log.i(TAG, "Rate limit healthy. Restoring 30s interval")
            scheduleTask()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scheduledTask?.cancel(true)
        executor.shutdown()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }
}
