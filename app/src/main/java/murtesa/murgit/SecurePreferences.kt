// SecurePreferences.kt - Updated for "All-Enabled by Default" logic with external repo support
package murtesa.murgit

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecurePreferences(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            "secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        prefs.edit().putString("github_token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("github_token", null)
    }

    fun saveUsername(username: String) {
        prefs.edit().putString("github_username", username).apply()
    }

    fun getUsername(): String? {
        return prefs.getString("github_username", null)
    }

    // --- Background Monitoring State ---

    fun saveSshKeysCount(count: Int) {
        prefs.edit().putInt("ssh_keys_count", count).apply()
    }

    fun getSshKeysCount(): Int {
        return prefs.getInt("ssh_keys_count", -1)
    }

    fun saveLastSeenEventId(id: String) {
        prefs.edit().putString("last_seen_event_id", id).apply()
    }

    fun getLastSeenEventId(): String? {
        return prefs.getString("last_seen_event_id", null)
    }

    // --- Monitoring Logic (Enable all by default, blacklist in unmonitored_repos) ---

    fun addMonitoredRepo(fullName: String) {
        // Removing from blacklist enables monitoring
        val unmonitored = getUnmonitoredRepos().toMutableSet()
        unmonitored.remove(fullName)
        prefs.edit().putStringSet("unmonitored_repos", unmonitored).apply()
    }

    fun removeMonitoredRepo(fullName: String) {
        // Adding to blacklist disables monitoring
        val unmonitored = getUnmonitoredRepos().toMutableSet()
        unmonitored.add(fullName)
        prefs.edit().putStringSet("unmonitored_repos", unmonitored).apply()
    }

    fun isRepoMonitored(fullName: String): Boolean {
        return !getUnmonitoredRepos().contains(fullName)
    }

    private fun getUnmonitoredRepos(): Set<String> {
        return prefs.getStringSet("unmonitored_repos", emptySet()) ?: emptySet()
    }
    
    // Returns the set of repos that should actually be polled
    fun getMonitoredRepos(allAvailableRepos: Set<String>): Set<String> {
        val unmonitored = getUnmonitoredRepos()
        return (allAvailableRepos + getExternalRepos()).filter { !unmonitored.contains(it) }.toSet()
    }

    // --- External Repositories (Repos not owned by user but explicitly added) ---

    fun addExternalRepo(fullName: String) {
        val external = getExternalRepos().toMutableSet()
        external.add(fullName)
        prefs.edit().putStringSet("external_repos", external).apply()
        // Ensure it's not blacklisted when added
        addMonitoredRepo(fullName)
    }

    fun getExternalRepos(): Set<String> {
        return prefs.getStringSet("external_repos", emptySet()) ?: emptySet()
    }

    fun removeExternalRepo(fullName: String) {
        val external = getExternalRepos().toMutableSet()
        external.remove(fullName)
        prefs.edit().putStringSet("external_repos", external).apply()
        // Also clean up from blacklist if it was there
        val unmonitored = getUnmonitoredRepos().toMutableSet()
        unmonitored.remove(fullName)
        prefs.edit().putStringSet("unmonitored_repos", unmonitored).apply()
    }

    // --- Commit Tracking ---

    fun saveLastCommitSha(repoFullName: String, sha: String) {
        prefs.edit().putString("last_sha_$repoFullName", sha).apply()
    }

    fun getLastCommitSha(repoFullName: String): String? {
        return prefs.getString("last_sha_$repoFullName", null)
    }

    // --- Known Repositories (for detecting new ones) ---

    fun saveKnownRepos(repoFullNames: Set<String>) {
        prefs.edit().putStringSet("known_repos", repoFullNames).apply()
    }

    fun getKnownRepos(): Set<String> {
        return prefs.getStringSet("known_repos", emptySet()) ?: emptySet()
    }

    // --- ETag Support ---

    fun saveEtag(key: String, etag: String?) {
        if (etag == null) {
            prefs.edit().remove("etag_$key").apply()
        } else {
            prefs.edit().putString("etag_$key", etag).apply()
        }
    }

    fun getEtag(key: String): String? {
        return prefs.getString("etag_$key", null)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
