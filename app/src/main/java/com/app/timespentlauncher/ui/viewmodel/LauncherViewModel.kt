package com.app.timespentlauncher.ui.viewmodel

import android.app.AppOpsManager
import android.app.Application
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.timespentlauncher.data.PlantState
import com.app.timespentlauncher.data.StreakData
import com.app.timespentlauncher.data.StreakRepository
import com.app.timespentlauncher.data.UserPreferences
import com.app.timespentlauncher.data.UserPreferencesRepository
import com.app.timespentlauncher.ui.theme.SbbColors
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
    val nudgeThresholdCrossed: Boolean = false,
)

data class LauncherUiState(
    val allApps: List<AppInfo> = emptyList(),
    val topTenApps: List<AppInfo> = emptyList(),
    val pinnedApps: List<AppInfo?> = List(4) { null },
    val preferences: UserPreferences = UserPreferences(
        dailyLimitMinutes = 120,
        watchList = emptySet(),
        pinnedApps = emptyList(),
        onboardingComplete = false,
    ),
    val streakData: StreakData = StreakData(
        currentStreak      = 0,
        lastCheckedDate    = "",
        consecutiveBadDays = 0,
        plantState         = PlantState.STRESSED,
        history            = emptyList(),
    ),
    val totalWatchListUsageMs: Long = 0L,
    val nudgeThresholdCrossed: Boolean = false,
)

enum class UsageStatus { NORMAL, WARNING, EXCEEDED }

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
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
        viewModelScope.launch {
            prefsRepo.userPreferences.collect { prefs ->
                Log.d("SBB_PREFS", "dailyLimitMinutes = ${prefs.dailyLimitMinutes}")
            }
        }

        viewModelScope.launch {
            uiState
                .filter { it.allApps.isNotEmpty() }
                .collect { state ->
                    val limitMs = state.preferences.dailyLimitMinutes * 60 * 1000L
                    streakRepo.recordDayResult(
                        watchUsageMs = state.totalWatchListUsageMs,
                        limitMs      = limitMs,
                    )
                }
        }

    }

    private fun seedDefaultPinnedAppsIfNeeded() {
        viewModelScope.launch {
            prefsRepo.userPreferences.first().let { prefs ->
                if (prefs.pinnedApps.isNotEmpty()) return@launch

                val defaults = listOfNotNull(
                    resolveDefault(Intent(Intent.ACTION_DIAL)),
                    resolveDefault(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("smsto:") }),
                    resolveDefault(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("https://") }),
                    resolveDefault(Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
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
            val prefs = prefsRepo.userPreferences.first()
            val limitMs = prefs.dailyLimitMinutes * 60 * 1000L
            val usageMap  = getTodayUsageStats()
            val openCount = getTodayOpenCounts()
            _rawApps.value = getInstalledApps(usageMap, openCount, prefs.watchList, limitMs)
        }
    }

    fun onAppClicked(app: AppInfo) {
        val state = uiState.value
        if (app.packageName !in state.preferences.watchList) return

        val limitMs = state.preferences.dailyLimitMinutes * 60 * 1000L

        // Per-app proportion
        val appProportion = if (limitMs > 0) app.usageTimeMs.toFloat() / limitMs else 0f
        // Total watchlist proportion
        val totalProportion = if (limitMs > 0) state.totalWatchListUsageMs.toFloat() / limitMs else 0f

        val appStatus = getUsageStatus(appProportion)
        val totalStatus = getUsageStatus(totalProportion)

        if (appStatus == UsageStatus.WARNING || appStatus == UsageStatus.EXCEEDED ||
            totalStatus == UsageStatus.WARNING || totalStatus == UsageStatus.EXCEEDED) {
            _nudgeApp.value = app
        }
    }

    fun dismissNudge() { _nudgeApp.value = null }

    fun resetStreak() {
        viewModelScope.launch { streakRepo.resetStreak() }
    }

    suspend fun isOnboardingComplete(): Boolean =
        prefsRepo.userPreferences.first().onboardingComplete

    fun setDailyLimitMinutes(minutes: Int) {
        viewModelScope.launch { prefsRepo.setDailyLimitMinutes(minutes) }
    }

    fun addToWatchList(packageName: String) {
        viewModelScope.launch { prefsRepo.addToWatchList(packageName) }
    }

    fun removeFromWatchList(packageName: String) {
        viewModelScope.launch { prefsRepo.removeFromWatchList(packageName) }
    }

    fun setPinnedApps(packages: List<String>) {
        viewModelScope.launch { prefsRepo.setPinnedApps(packages) }
    }

    fun swapPinnedApp(slotIndex: Int, packageName: String) {
        viewModelScope.launch { prefsRepo.swapPinnedApp(slotIndex, packageName) }
    }

    private fun getTodayUsageStats(): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val startMs = todayStartMs()
        val endMs = System.currentTimeMillis()

        return usm
            .queryAndAggregateUsageStats(startMs, endMs)
            .mapValues { (_, stats) ->
                stats.totalTimeInForeground
            }
    }

    private fun getTodayOpenCounts(): Map<String, Int> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val counts = mutableMapOf<String, Int>()
        val events = usm.queryEvents(todayStartMs(), System.currentTimeMillis())
        val event = UsageEvents.Event()

        var currentPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val pkg = event.packageName

                    if (pkg != currentPackage) {
                        counts[pkg] = (counts[pkg] ?: 0) + 1
                        currentPackage = pkg
                    }
                }

                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (event.packageName == currentPackage) {
                        currentPackage = null
                    }
                }
            }
        }

        return counts
    }
    private fun getInstalledApps(
        usageMap: Map<String, Long>,
        openCounts: Map<String, Int>,
        watchList: Set<String>,
        limitMs: Long,
    ): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) return@mapNotNull null
                val usageMs = usageMap[pkg] ?: 0L
                try {
                    AppInfo(
                        packageName          = pkg,
                        label                = info.loadLabel(packageManager).toString(),
                        icon                 = info.loadIcon(packageManager),
                        usageTimeMs          = usageMs,
                        openCount            = openCounts[pkg] ?: 0,
                        nudgeThresholdCrossed = pkg in watchList && usageMs >= limitMs,
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

    companion object {
        fun formatUsageTime(usageMs: Long, totalMs: Long = 0L): String {
            if (usageMs == 0L) return "NO USAGE"
            val minutes = usageMs / 1000 / 60
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            val timeStr = when {
                hours > 0   -> "${hours}H ${remainingMinutes}M"
                minutes > 0 -> "${minutes}M"
                else        -> "< 1M"
            }
            return if (totalMs > 0L) {
                val pct = ((usageMs.toFloat() / totalMs) * 100).toInt()
                "$timeStr · $pct%"
            } else {
                timeStr
            }
        }

        fun getUsageStatus(proportion: Float) = when {
            proportion >= 0.9f -> UsageStatus.EXCEEDED
            proportion >= 0.6f -> UsageStatus.WARNING
            else               -> UsageStatus.NORMAL
        }

        fun getUsageColor(proportion: Float) = when (getUsageStatus(proportion)) {
            UsageStatus.EXCEEDED -> SbbColors.WatchRed
            UsageStatus.WARNING  -> SbbColors.WatchOrange
            UsageStatus.NORMAL   -> SbbColors.TextSecondary
        }

    }

}
