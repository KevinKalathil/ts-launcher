package com.example.stopbreathbelauncher.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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

data class StreakData(
    val currentStreak: Int,
    val lastCheckedDate: String,  // ISO date string
    val consecutiveBadDays: Int,
    val plantState: PlantState,
)

class StreakRepository(private val context: Context) {

    private object Keys {
        val CURRENT_STREAK        = intPreferencesKey("streak_current")
        val LAST_CHECKED_DATE     = stringPreferencesKey("streak_last_date")
        val CONSECUTIVE_BAD_DAYS  = intPreferencesKey("streak_bad_days")
        val TODAY_WAS_GOOD        = booleanPreferencesKey("streak_today_was_good")
    }

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    val streakData: Flow<StreakData> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val streak     = prefs[Keys.CURRENT_STREAK] ?: 0
            val badDays    = prefs[Keys.CONSECUTIVE_BAD_DAYS] ?: 0
            val lastDate   = prefs[Keys.LAST_CHECKED_DATE] ?: ""
            StreakData(
                currentStreak       = streak,
                lastCheckedDate     = lastDate,
                consecutiveBadDays  = badDays,
                plantState          = resolvePlantState(streak, badDays),
            )
        }

    /**
     * Call once per day (on resume) to evaluate whether yesterday was a good or bad day.
     * [wasGoodDay] = user stayed within their goal yesterday.
     */
    suspend fun recordDayResult(wasGoodDay: Boolean) {
        Log.d("kevin", "recorded results ${wasGoodDay}")
        val today = LocalDate.now().format(fmt)
        context.dataStore.edit { prefs ->
            val lastDate = prefs[Keys.LAST_CHECKED_DATE] ?: ""

            // New day — reset daily tracking
            if (lastDate != today) {
                prefs[Keys.LAST_CHECKED_DATE] = today
                prefs[Keys.TODAY_WAS_GOOD] = true
            }

            // Already recorded as bad today — nothing can change it
            val alreadyBad = !(prefs[Keys.TODAY_WAS_GOOD] ?: true)
            if (alreadyBad) return@edit

            // Still good — check if it just turned bad
            if (!wasGoodDay) {
                prefs[Keys.TODAY_WAS_GOOD] = false
                val badDays = (prefs[Keys.CONSECUTIVE_BAD_DAYS] ?: 0) + 1
                prefs[Keys.CONSECUTIVE_BAD_DAYS] = badDays
                if (badDays == 1) prefs[Keys.CURRENT_STREAK] = 0
            } else {
                // Good day so far — increment streak once per day
                if (lastDate != today) {
                    prefs[Keys.CURRENT_STREAK] = (prefs[Keys.CURRENT_STREAK] ?: 0) + 1
                }
            }
        }
    }

    suspend fun resetStreak() {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENT_STREAK]       = 0
            prefs[Keys.CONSECUTIVE_BAD_DAYS] = 0
            prefs[Keys.LAST_CHECKED_DATE]    = ""
        }
    }

    private fun resolvePlantState(streak: Int, badDays: Int): PlantState = when {
        badDays >= 3  -> PlantState.DEAD
        badDays == 2  -> PlantState.DYING
        badDays == 1  -> PlantState.WILTING
        streak >= 7   -> PlantState.THRIVING
        streak >= 3   -> PlantState.HEALTHY
        else          -> PlantState.STRESSED
    }
}
