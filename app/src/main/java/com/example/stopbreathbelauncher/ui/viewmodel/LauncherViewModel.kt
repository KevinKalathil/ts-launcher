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
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stopbreathbelauncher.data.*
import com.example.stopbreathbelauncher.ui.theme.SbbColors
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
            val prefs = prefsRepo.userPreferences.first()
            val limitMs = prefs.dailyLimitMinutes * 60 * 1000L
            val usageMap  = getTodayUsageStats()
            val openCount = getTodayOpenCounts()
            _rawApps.value = getInstalledApps(usageMap, openCount, prefs.watchList, limitMs)
        }
    }

    fun onAppClicked(app: AppInfo) {
        val state = uiState.value
        val limitMs = state.preferences.dailyLimitMinutes * 60 * 1000L
        val proportion = if (limitMs > 0) app.usageTimeMs.toFloat() / limitMs else 0f
        val usageStatus = getUsageStatus(proportion)
        if (app.packageName in state.preferences.watchList &&
            (usageStatus == UsageStatus.WARNING || usageStatus == UsageStatus.EXCEEDED)) {
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
        val usageMap = mutableMapOf<String, Long>()
        // Key is (packageName, className) to handle multi-activity packages
        val resumeMap = mutableMapOf<Pair<String, String>, Long>()

        val events = usm.queryEvents(startMs, endMs)
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val key = event.packageName to event.className
            when (event.eventType) {
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                    resumeMap[key] = event.timeStamp
                }
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val resumeTime = resumeMap.remove(key)
                    if (resumeTime != null) {
                        usageMap[event.packageName] =
                            (usageMap[event.packageName] ?: 0L) + (event.timeStamp - resumeTime)
                    }
                }
            }
        }

        // Close any still-open sessions, aggregating back to package
        resumeMap.forEach { (key, resumeTime) ->
            val (pkg, _) = key
            usageMap[pkg] = (usageMap[pkg] ?: 0L) + (endMs - resumeTime)
        }

        return usageMap
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
