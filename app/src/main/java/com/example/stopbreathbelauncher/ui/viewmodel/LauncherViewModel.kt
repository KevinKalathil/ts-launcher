package com.example.stopbreathbelauncher.ui.viewmodel

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val usageTimeMs: Long  // milliseconds used today
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val packageManager = context.packageManager

    // Persisted set of disabled package names (swap for Room later)
    private val _disabledApps = MutableStateFlow<Set<String>>(emptySet())
    val disabledApps: StateFlow<Set<String>> = _disabledApps

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps

    private val _topTenApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val topTenApps: StateFlow<List<AppInfo>> = _topTenApps

    // Shown when user taps a disabled app
    private val _disabledTapEvent = MutableStateFlow<AppInfo?>(null)
    val disabledTapEvent: StateFlow<AppInfo?> = _disabledTapEvent

    init {
        refreshApps()
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val usageMap = getUsageStats()
            val apps = getInstalledApps(usageMap)

            // Sort by usage time descending
            val sorted = apps.sortedByDescending { it.usageTimeMs }

            _allApps.value = sorted
            _topTenApps.value = sorted.take(10)
        }
    }

    fun toggleDisabled(app: AppInfo) {
        val current = _disabledApps.value.toMutableSet()
        if (current.contains(app.packageName)) {
            current.remove(app.packageName)
        } else {
            current.add(app.packageName)
        }
        _disabledApps.value = current
        // TODO: persist to Room database
    }

    fun onDisabledAppTapped(app: AppInfo) {
        _disabledTapEvent.value = app
    }

    fun clearDisabledTapEvent() {
        _disabledTapEvent.value = null
    }

    private fun getUsageStats(): Map<String, Long> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (1000 * 60 * 60 * 24) // last 24 hours

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return stats?.associate { it.packageName to it.totalTimeInForeground } ?: emptyMap()
    }

    private fun getInstalledApps(usageMap: Map<String, Long>): List<AppInfo> {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }

        return packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                // Skip our own launcher from the list
                if (pkg == context.packageName) return@mapNotNull null

                try {
                    AppInfo(
                        packageName = pkg,
                        label = resolveInfo.loadLabel(packageManager).toString(),
                        icon = resolveInfo.loadIcon(packageManager),
                        usageTimeMs = usageMap[pkg] ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }
}