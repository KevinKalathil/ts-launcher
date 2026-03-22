package com.example.stopbreathbelauncher.data

import android.content.Context
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class PlantState {
    THRIVING,   // 7+ good days
    HEALTHY,    // 3–6 good days
    STRESSED,   // at limit today
    WILTING,    // over limit today
    DYING,      // 2 bad days
    DEAD        // 3+ bad days
}

@Serializable
data class DayRecord(
    val date: String,
    val watchUsageMs: Long,
    val limitMs: Long,
    val wasGood: Boolean,       // locked in at end of day, dynamic today
)

data class StreakData(
    val currentStreak: Int,
    val lastCheckedDate: String,
    val consecutiveBadDays: Int,
    val plantState: PlantState,
    val history: List<DayRecord>,
)

class StreakRepository(private val context: Context) {

    private object Keys {
        val DAY_HISTORY = stringPreferencesKey("streak_day_history")  // JSON array
    }

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val json = Json { ignoreUnknownKeys = true }

    val streakData: Flow<StreakData> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val history = parseHistory(prefs[Keys.DAY_HISTORY])
            buildStreakData(history)
        }

    suspend fun recordDayResult(watchUsageMs: Long, limitMs: Long) {
        val today = LocalDate.now().format(fmt)
        context.dataStore.edit { prefs ->
            val history = parseHistory(prefs[Keys.DAY_HISTORY]).toMutableList()

            val todayIndex = history.indexOfFirst { it.date == today }
            val wasGood = watchUsageMs < limitMs

            if (todayIndex >= 0) {
                // Update today's record dynamically
                history[todayIndex] = history[todayIndex].copy(
                    watchUsageMs = watchUsageMs,
                    limitMs      = limitMs,
                    wasGood      = wasGood,
                )
            } else {
                // New day — add record and trim to 7 days
                history.add(DayRecord(
                    date         = today,
                    watchUsageMs = watchUsageMs,
                    limitMs      = limitMs,
                    wasGood      = wasGood,
                ))
                history.sortBy { it.date }
                while (history.size > 7) history.removeAt(0)
            }

            prefs[Keys.DAY_HISTORY] = json.encodeToString(history)
        }
    }

    suspend fun resetStreak() {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAY_HISTORY] = "[]"
        }
    }

    private fun parseHistory(raw: String?): List<DayRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<DayRecord>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun buildStreakData(history: List<DayRecord>): StreakData {
        val today = LocalDate.now().format(fmt)
        val sorted = history.sortedBy { it.date }

        // Compute streak — count consecutive good days ending today or yesterday
        var streak = 0
        var badDays = 0
        for (record in sorted.reversed()) {
            if (record.wasGood) {
                streak++
                badDays = 0
            } else {
                badDays++
                if (badDays >= 1) break
            }
        }

        // Consecutive bad days from end of history
        var consecutiveBad = 0
        for (record in sorted.reversed()) {
            if (!record.wasGood) consecutiveBad++
            else break
        }

        val todayRecord = sorted.find { it.date == today }
        val plantState = resolvePlantState(streak, consecutiveBad, todayRecord)

        return StreakData(
            currentStreak      = streak,
            lastCheckedDate    = sorted.lastOrNull()?.date ?: "",
            consecutiveBadDays = consecutiveBad,
            plantState         = plantState,
            history            = sorted,
        )
    }

    private fun resolvePlantState(streak: Int, badDays: Int, today: DayRecord?): PlantState = when {
        badDays >= 3                                          -> PlantState.DEAD
        badDays == 2                                          -> PlantState.DYING
        badDays == 1                                          -> PlantState.WILTING
        today != null && today.watchUsageMs >= today.limitMs  -> PlantState.WILTING
        streak >= 7                                           -> PlantState.THRIVING
        streak >= 3                                           -> PlantState.HEALTHY
        else                                                  -> PlantState.STRESSED
    }
}