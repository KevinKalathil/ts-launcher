package com.example.stopbreathbelauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Top-level DataStore instance — one per app
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val dailyLimitMinutes: Int,       // Reduce: cap in minutes. Redirect: target ratio numerator (goal mins per watch min)
    val watchList: Set<String>,       // Package names
    val pinnedApps: List<String>,     // Package names, ordered — exactly 4
    val onboardingComplete: Boolean,
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val DAILY_LIMIT_MINUTES   = intPreferencesKey("daily_limit_minutes")
        val WATCH_LIST            = stringSetPreferencesKey("watch_list")
        val PINNED_APPS           = stringPreferencesKey("pinned_apps") // CSV — preserves order
        val ONBOARDING_COMPLETE   = booleanPreferencesKey("onboarding_complete")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs -> prefs.toUserPreferences() }

    // --- Daily limit ---

    suspend fun setDailyLimitMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DAILY_LIMIT_MINUTES] = minutes }
    }

    // --- Watch list ---

    suspend fun addToWatchList(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.WATCH_LIST] ?: emptySet()
            prefs[Keys.WATCH_LIST] = current + packageName
        }
    }

    suspend fun removeFromWatchList(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.WATCH_LIST] ?: emptySet()
            prefs[Keys.WATCH_LIST] = current - packageName
        }
    }

    suspend fun setWatchList(packages: Set<String>) {
        context.dataStore.edit { it[Keys.WATCH_LIST] = packages }
    }

    // --- Pinned apps ---
    // Stored as CSV string to preserve order (DataStore Set is unordered)

    suspend fun setPinnedApps(packages: List<String>) {
        require(packages.size == 4) { "Must have exactly 4 pinned apps" }
        context.dataStore.edit { it[Keys.PINNED_APPS] = packages.joinToString(",") }
    }

    suspend fun swapPinnedApp(slotIndex: Int, packageName: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.PINNED_APPS] ?: "")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableList()
            while (current.size < 4) current.add("")
            current[slotIndex] = packageName
            prefs[Keys.PINNED_APPS] = current.joinToString(",")
        }
    }

    // --- Onboarding ---

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    // --- Mapping ---

    private fun Preferences.toUserPreferences() = UserPreferences(
        dailyLimitMinutes = this[Keys.DAILY_LIMIT_MINUTES] ?: 120, // default 2h
        watchList = this[Keys.WATCH_LIST] ?: emptySet(),
        pinnedApps = (this[Keys.PINNED_APPS] ?: "")
            .split(",")
            .filter { it.isNotBlank() }
            .take(4),
        onboardingComplete = this[Keys.ONBOARDING_COMPLETE] ?: false,
    )
}
