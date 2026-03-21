package com.example.stopbreathbelauncher.ui.viewmodel

import android.app.AppOpsManager
import android.app.Application
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stopbreathbelauncher.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val usageTimeMs: Long,
    val openCount: Int,
)

data class LauncherUiState(
    val allApps: List<AppInfo> = emptyList(),
    val topTenApps: List<AppInfo> = emptyList(),
    val pinnedApps: List<AppInfo?> = List(4) { null },
    val preferences: UserPreferences = UserPreferences(
        goalMode = GoalMode.REDUCE,
        dailyLimitMinutes = 120,
        watchList = emptySet(),
        goalApps = emptySet(),
        pinnedApps = emptyList(),
        onboardingComplete = false,
    ),
    val streakData: StreakData = StreakData(0, "", 0, PlantState.STRESSED),
    val totalWatchListUsageMs: Long = 0L,
    val nudgeThresholdCrossed: Boolean = false,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val packageManager = context.packageManager

    private val prefsRepo  = UserPreferencesRepository(context)
    private val streakRepo = StreakRepository(context)

    private val _rawApps = MutableStateFlow<List<AppInfo>>(emptyList())

    val uiState: StateFlow<LauncherUiState> = combine(
        _rawApps,
        prefsRepo.userPreferences,
        streakRepo.streakData,
    ) { apps, prefs, streak ->

        val watchUsageMs = apps
            .filter { it.packageName in prefs.watchList }
            .sumOf { it.usageTimeMs }

        val limitMs = prefs.dailyLimitMinutes * 60 * 1000L
        val nudgeCrossed = watchUsageMs >= limitMs

        val pinned = prefs.pinnedApps.map { pkg ->
            apps.find { it.packageName == pkg }
        }

        LauncherUiState(
            allApps               = apps.sortedBy { it.label.lowercase() },
            topTenApps            = apps.sortedByDescending { it.usageTimeMs }.take(10),
            pinnedApps            = pinned,
            preferences           = prefs,
            streakData            = streak,
            totalWatchListUsageMs = watchUsageMs,
            nudgeThresholdCrossed = nudgeCrossed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState(),
    )

    private val _nudgeApp = MutableStateFlow<AppInfo?>(null)
    val nudgeApp: StateFlow<AppInfo?> = _nudgeApp

    init {
        refreshApps()
        seedDefaultPinnedAppsIfNeeded()
    }

    private fun seedDefaultPinnedAppsIfNeeded() {
        viewModelScope.launch {
            prefsRepo.userPreferences.first().let { prefs ->
                if (prefs.pinnedApps.isNotEmpty()) return@launch

                val defaults = listOfNotNull(
                    resolveDefault(Intent(Intent.ACTION_DIAL)),
                    resolveDefault(Intent(Intent.ACTION_SENDTO).apply { data = android.net.Uri.parse("smsto:") }),
                    resolveDefault(Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse("https://") }),
                    resolveDefault(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)),
                ).distinct().take(4)

                if (defaults.isNotEmpty()) prefsRepo.setPinnedApps(defaults)
            }
        }
    }

    private fun resolveDefault(intent: Intent): String? =
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
            ?.takeIf { it != context.packageName }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!hasUsagePermission()) return@launch
            val usageMap  = getTodayUsageStats()
            val openCount = getTodayOpenCounts()
            _rawApps.value = getInstalledApps(usageMap, openCount)
        }
    }

    fun onAppClicked(app: AppInfo) {
        val state = uiState.value
        val isWatchListed = app.packageName in state.preferences.watchList
        if (isWatchListed && state.nudgeThresholdCrossed) {
            _nudgeApp.value = app
        }
    }

    fun dismissNudge() { _nudgeApp.value = null }

    fun recordTodayResult() {
        viewModelScope.launch {
            val wasGoodDay = !uiState.value.nudgeThresholdCrossed
            streakRepo.recordDayResult(wasGoodDay)
        }
    }

    fun resetStreak() {
        viewModelScope.launch { streakRepo.resetStreak() }
    }

    fun setGoalMode(mode: GoalMode) {
        viewModelScope.launch { prefsRepo.setGoalMode(mode) }
    }

    fun setDailyLimitMinutes(minutes: Int) {
        viewModelScope.launch { prefsRepo.setDailyLimitMinutes(minutes) }
    }

    fun addToWatchList(packageName: String) {
        viewModelScope.launch { prefsRepo.addToWatchList(packageName) }
    }

    fun removeFromWatchList(packageName: String) {
        viewModelScope.launch { prefsRepo.removeFromWatchList(packageName) }
    }

    fun addToGoalApps(packageName: String) {
        viewModelScope.launch { prefsRepo.addToGoalApps(packageName) }
    }

    fun removeFromGoalApps(packageName: String) {
        viewModelScope.launch { prefsRepo.removeFromGoalApps(packageName) }
    }

    fun setPinnedApps(packages: List<String>) {
        viewModelScope.launch { prefsRepo.setPinnedApps(packages) }
    }

    fun swapPinnedApp(slotIndex: Int, packageName: String) {
        viewModelScope.launch { prefsRepo.swapPinnedApp(slotIndex, packageName) }
    }

    fun setOnboardingComplete() {
        viewModelScope.launch { prefsRepo.setOnboardingComplete(true) }
    }

    private fun getTodayUsageStats(): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, todayStartMs(), System.currentTimeMillis())
            ?.associate { it.packageName to it.totalTimeInForeground }
            ?: emptyMap()
    }

    private fun getTodayOpenCounts(): Map<String, Int> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val counts = mutableMapOf<String, Int>()
        val events = usm.queryEvents(todayStartMs(), System.currentTimeMillis())
        val event  = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                counts[event.packageName] = (counts[event.packageName] ?: 0) + 1
            }
        }
        return counts
    }

    private fun getInstalledApps(
        usageMap: Map<String, Long>,
        openCounts: Map<String, Int>,
    ): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) return@mapNotNull null
                try {
                    AppInfo(
                        packageName = pkg,
                        label       = info.loadLabel(packageManager).toString(),
                        icon        = info.loadIcon(packageManager),
                        usageTimeMs = usageMap[pkg] ?: 0L,
                        openCount   = openCounts[pkg] ?: 0,
                    )
                } catch (e: Exception) { null }
            }
    }

    private fun todayStartMs(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }
}
