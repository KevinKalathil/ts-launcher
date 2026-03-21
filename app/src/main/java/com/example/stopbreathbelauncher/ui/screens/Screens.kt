package com.example.stopbreathbelauncher.ui.screens

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
import com.example.stopbreathbelauncher.data.PlantState
import com.example.stopbreathbelauncher.ui.components.*
import com.example.stopbreathbelauncher.ui.scroll.LineWheelScroll
import com.example.stopbreathbelauncher.ui.theme.SbbColors
import com.example.stopbreathbelauncher.ui.viewmodel.AppInfo
import com.example.stopbreathbelauncher.ui.viewmodel.LauncherUiState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

// ── Homescreen ────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    state: LauncherUiState,
    onAppClick: (AppInfo) -> Unit,
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

        // List header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("TOP_10 // USAGE", style = MaterialTheme.typography.labelLarge, color = SbbColors.TextSecondary)
        }

        Divider()

        // Wheel scroll list
        Box(modifier = Modifier.weight(1f)) {
            LineWheelScroll(
                items          = apps,
                selectedIndex  = selectedIndex,
                onItemSelected = { selectedIndex = it },
            ) { app, isFocused, scale ->
                val flag = when {
                    app.packageName in state.preferences.watchList -> AppFlag.WATCH
                    app.packageName in state.preferences.goalApps  -> AppFlag.GOAL
                    else -> AppFlag.NONE
                }
                AppRow(
                    app          = app,
                    flag         = flag,
                    isFocused    = isFocused,
                    scale        = scale,
                    showBar      = true,
                    totalUsageMs = totalUsageMs,
                    onClick      = { onAppClick(app) },
                )
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
            Text(
                text  = "STREAK: ${if (streakCount > 0) "$streakCount DAYS" else "—"}  $streakPips",
                style = MaterialTheme.typography.labelMedium,
                color = if (streakCount > 0) SbbColors.PlantGreen else SbbColors.TextSecondary,  // was TextDim
            )
        }

        // Plant + settings gear
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.TopEnd) {
                PlantDisplay(state = plantState)
                Text(
                    text     = "[⚙]",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = SbbColors.TextDim,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable { onSettingsClick() }
                        .padding(4.dp),
                )
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
            ) { app, isFocused, scale ->
                val flag = when {
                    app.packageName in state.preferences.watchList -> AppFlag.WATCH
                    app.packageName in state.preferences.goalApps  -> AppFlag.GOAL
                    else -> AppFlag.NONE
                }
                AppRow(
                    app          = app,
                    flag         = flag,
                    isFocused    = isFocused,
                    scale        = scale,
                    showBar      = true,
                    onClick      = { onAppClick(app) },
                    onLongClick  = { onAppLongClick(app) },
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SbbColors.Surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        pinnedApps.forEach { app ->
            if (app != null) {
                DockSlot(app = app, onClick = { onAppClick(app) })
            } else {
                EmptyDockSlot()
            }
        }
    }
}

@Composable
private fun DockSlot(app: AppInfo, onClick: () -> Unit) {
    val bitmap = remember(app.packageName) {
        app.icon.toBitmap(128, 128).asImageBitmap()
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .background(SbbColors.SurfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            bitmap             = bitmap,
            contentDescription = app.label,
            modifier           = Modifier.size(52.dp),
        )
    }


}

@Composable
private fun EmptyDockSlot() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(2.dp, SbbColors.Border),
            contentAlignment = Alignment.Center,
        ) {
            Text("?", style = MaterialTheme.typography.bodyLarge, color = SbbColors.TextDim)
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
