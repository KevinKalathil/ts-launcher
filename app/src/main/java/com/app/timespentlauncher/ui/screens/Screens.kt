package com.app.timespentlauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.timespentlauncher.data.PlantState
import com.app.timespentlauncher.ui.components.AppFlag
import com.app.timespentlauncher.ui.components.AppIconSlot
import com.app.timespentlauncher.ui.components.AppRow
import com.app.timespentlauncher.ui.components.PlantDisplay
import com.app.timespentlauncher.ui.scroll.LineWheelScroll
import com.app.timespentlauncher.ui.theme.SbbColors
import com.app.timespentlauncher.ui.viewmodel.AppInfo
import com.app.timespentlauncher.ui.viewmodel.LauncherUiState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.app.timespentlauncher.ui.viewmodel.LauncherViewModel

// ── Homescreen ────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    state: LauncherUiState,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onSetFlag: (AppInfo, AppFlag) -> Unit,
    onUninstall: (AppInfo) -> Unit,
    onSettingsClick: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val apps = state.topTenApps
    val totalUsageMs = apps.sumOf { it.usageTimeMs }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SbbColors.Background),
    ) {
        // Header: clock + date + plant
        PlantHeader(
            streakCount = state.streakData.currentStreak,
            plantState  = state.streakData.plantState,
            onSettingsClick = onSettingsClick,
        )

        // List header + total time
        val limitMs = state.preferences.dailyLimitMinutes * 60 * 1000L
        val totalProportion = if (limitMs > 0) (state.totalWatchListUsageMs.toFloat() / limitMs).coerceIn(0f, 1f) else 0f

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("MOST_USED_APPS", style = MaterialTheme.typography.headlineMedium, color = SbbColors.TextPrimary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "${LauncherViewModel.formatUsageTime(state.totalWatchListUsageMs)} / ${"%.2g".format(state.preferences.dailyLimitMinutes / 60f)}H",
                    style = MaterialTheme.typography.labelLarge,
                    color = LauncherViewModel.getUsageColor(proportion = totalProportion),
                )
                Text(
                    text     = "[⚙]",
                    style    = MaterialTheme.typography.labelLarge,
                    color    = SbbColors.TextDim,
                    modifier = Modifier.clickable { onSettingsClick() },
                )
            }
        }

        Divider()

        // Wheel scroll list
        Box(modifier = Modifier.weight(1f)) {
            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "LOADING...",
                        style = MaterialTheme.typography.labelLarge,
                        color = SbbColors.TextDim,
                    )
                }
            } else {
                LineWheelScroll(
                    items          = apps,
                    selectedIndex  = selectedIndex,
                    onItemSelected = { selectedIndex = it },
                ) { app, isFocused, scale ->
                    val flag = when {
                        app.packageName in state.preferences.watchList -> AppFlag.WATCH
                        else -> AppFlag.NONE
                    }
                    AppRow(
                        app = app,
                        flag = flag,
                        isFocused = isFocused,
                        scale = scale,
                        showBar = true,
                        totalUsageMs = totalUsageMs,
                        onClick = { onAppClick(app) },
                        onFlagChange = { newFlag ->
                            when (newFlag) {
                                AppFlag.WATCH -> onSetFlag(app, AppFlag.WATCH)
                                AppFlag.NONE -> onSetFlag(app, AppFlag.NONE)
                            }
                        },
                        onUninstall = { onUninstall(app) },
                    )
                }
            }
        }

        Divider()

        // Dock
        Dock(pinnedApps = state.pinnedApps, onAppClick = onAppClick)
    }
}

// ── Plant header ──────────────────────────────────────────────────────────────

@Composable
private fun PlantHeader(
    streakCount: Int,
    plantState: PlantState,
    onSettingsClick: () -> Unit,
) {
    var timeStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"))
            dateStr = now.format(DateTimeFormatter.ofPattern("EEE // MMM d")).uppercase()
            delay(1000)
        }
    }

    val streakPips = buildString {
        repeat(7) { i -> append(if (i < streakCount) "▮" else "▯") }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Clock + date + streak
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = timeStr,
                style = MaterialTheme.typography.displayLarge,
                color = SbbColors.TextPrimary,  // was TextDim
            )
            Text(
                text  = dateStr,
                style = MaterialTheme.typography.labelLarge,
                color = SbbColors.TextSecondary,  // was TextDim
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text  = "STREAK: ${if (streakCount > 0) "$streakCount DAYS" else "—"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (streakCount > 0) SbbColors.PlantGreen else SbbColors.TextSecondary,
                )
                Text(
                    text  = streakPips,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (streakCount > 0) SbbColors.PlantGreen else SbbColors.TextSecondary,
                    modifier = Modifier.offset(y = (-3).dp),
                )
            }
        }

        // Plant + settings gear
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.TopEnd) {
                PlantDisplay(state = plantState)
            }
        }
    }

    Divider()
}

// ── All Apps screen ───────────────────────────────────────────────────────────

@Composable
fun AllAppsScreen(
    state: LauncherUiState,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onSetFlag: (AppInfo, AppFlag) -> Unit,
    onUninstall: (AppInfo) -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val apps = state.allApps

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SbbColors.Background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ALL_APPS // A-Z", style = MaterialTheme.typography.headlineMedium, color = SbbColors.TextPrimary)
        }

        Divider()

        Box(modifier = Modifier.weight(1f)) {
            LineWheelScroll(
                items          = apps,
                selectedIndex  = selectedIndex,
                onItemSelected = { selectedIndex = it },
                indexBar       = true,
                itemLabel      = { it.label },
            ) { app, isFocused, scale ->
                val flag = when {
                    app.packageName in state.preferences.watchList -> AppFlag.WATCH
                    else -> AppFlag.NONE
                }
                AppRow(
                    app = app,
                    flag = flag,
                    isFocused = isFocused,
                    scale = scale,
                    showBar = true,
                    onClick = { onAppClick(app) },
                    onFlagChange = { newFlag ->
                        when (newFlag) {
                            AppFlag.WATCH -> onSetFlag(app, AppFlag.WATCH)
                            AppFlag.NONE -> onSetFlag(app, AppFlag.NONE)
                        }
                    },
                    onUninstall = { onUninstall(app) },
                )
            }
        }
    }
}
// ── Dock ──────────────────────────────────────────────────────────────────────

@Composable
fun Dock(
    pinnedApps: List<AppInfo?>,
    onAppClick: (AppInfo) -> Unit,
    selectedSlot: Int? = null,
    onSlotTap: ((Int) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SbbColors.Surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        pinnedApps.forEachIndexed { i, app ->
            val isSelected = i == selectedSlot
            if (app != null) {
                DockSlot(
                    app         = app,
                    onClick     = { onSlotTap?.invoke(i) ?: onAppClick(app) },
                    isSelected  = isSelected,
                )
            } else {
                EmptyDockSlot(
                    isSelected = isSelected,
                    onClick    = { onSlotTap?.invoke(i) },
                )
            }
        }
    }
}

@Composable
private fun DockSlot(app: AppInfo, onClick: () -> Unit, isSelected: Boolean = false) {
    AppIconSlot(
        app = app,
        size = 52.dp,
        onClick = onClick,
        isSelected = isSelected,
    )
}

@Composable
private fun EmptyDockSlot(isSelected: Boolean = false, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(2.dp, if (isSelected) SbbColors.TextPrimary else SbbColors.Border)
                .background(if (isSelected) SbbColors.Surface else Color.Transparent)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (isSelected) "▮" else "?",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) SbbColors.TextPrimary else SbbColors.TextDim,
            )
        }
        Text("—", style = MaterialTheme.typography.labelSmall, color = SbbColors.Border)
    }
}

// ── Shared ────────────────────────────────────────────────────────────────────

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SbbColors.Border)
    )
}
