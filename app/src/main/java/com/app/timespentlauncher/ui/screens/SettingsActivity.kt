package com.app.timespentlauncher.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.timespentlauncher.ui.components.AppFlag
import com.app.timespentlauncher.ui.components.AppIconSlot
import com.app.timespentlauncher.ui.components.AppRow
import com.app.timespentlauncher.ui.components.DailyLimitRow
import com.app.timespentlauncher.ui.scroll.LineWheelScroll
import com.app.timespentlauncher.ui.theme.SbbColors
import com.app.timespentlauncher.ui.theme.SbbScaffold
import com.app.timespentlauncher.ui.theme.StopBreathBeLauncherTheme
import com.app.timespentlauncher.ui.viewmodel.AppInfo
import com.app.timespentlauncher.ui.viewmodel.LauncherUiState
import com.app.timespentlauncher.ui.viewmodel.LauncherViewModel
import kotlin.math.roundToInt

class SettingsActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StopBreathBeLauncherTheme {
                SbbScaffold {
                    val uiState by viewModel.uiState.collectAsState()
                    SettingsScreen(
                        uiState   = uiState,
                        viewModel = viewModel,
                        onBack    = { finish() },
                    )
                }
            }
        }
    }
}

// ── Shared app icon strip ─────────────────────────────────────────────────────

/**
 * A horizontally-scrollable row of [AppIconSlot]s with optional remove badges.
 * Used in both the Settings watch-list and the AddAppPicker summary footer.
 *
 * @param packages   Ordered set of package names to display.
 * @param allApps    Full app list used to resolve package → AppInfo.
 * @param chipBg     Background tint for each slot.
 * @param chipColor  Accent color used for the "×" badge.
 * @param onRemove   Called with the package name when a badge is tapped.
 *                   Pass null to hide badges entirely (read-only strip).
 */
@Composable
fun AppIconStrip(
    packages: Collection<String>,
    allApps: List<AppInfo>,
    chipBg: Color,
    chipColor: Color,
    onRemove: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        if (packages.isEmpty()) {
            Text(
                "NONE YET",
                style = MaterialTheme.typography.labelSmall,
                color = SbbColors.TextDim,
            )
        } else {
            packages.forEach { pkg ->
                val app = allApps.find { it.packageName == pkg }
                AppIconSlot(
                    app     = app,
                    bgColor = chipBg,
                    badge   = if (onRemove != null) {
                        {
                            Text(
                                "×",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = chipColor.copy(alpha = 0.6f),
                                modifier = Modifier.clickable { onRemove(pkg) },
                            )
                        }
                    } else null,
                )
            }
        }
    }
}

// ── SettingsScreen ────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    uiState: LauncherUiState,
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
) {
    val prefs  = uiState.preferences
    val streak = uiState.streakData

    var showDockPicker by remember { mutableStateOf<Int?>(null) }
    var showAppPicker  by remember { mutableStateOf<AppFlag?>(null) }

    var limitSlider by remember(prefs.dailyLimitMinutes) {
        mutableFloatStateOf(prefs.dailyLimitMinutes.toFloat())
    }

    // Dock picker overlay
    showDockPicker?.let { slotIndex ->
        AppPickerScreen(
            apps         = uiState.allApps,
            title        = "REPLACING SLOT ${slotIndex + 1}",
            currentSlots = uiState.pinnedApps.map { it?.packageName ?: "" },
            activeSlot   = slotIndex,
            onPick       = { app ->
                viewModel.swapPinnedApp(slotIndex, app.packageName)
            },
            onBack       = { showDockPicker = null },
        )
        return
    }

    // Add app picker overlay
    showAppPicker?.let { targetFlag ->
        AddAppPickerScreen(
            apps       = uiState.allApps,
            watchList  = prefs.watchList,
            targetFlag = targetFlag,
            onAdd      = { app ->
                when (targetFlag) {
                    AppFlag.WATCH -> viewModel.addToWatchList(app.packageName)
                    AppFlag.NONE  -> {}
                }
            },
            onRemove   = { pkg -> viewModel.removeFromWatchList(pkg) },
            onBack     = { showAppPicker = null },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SbbColors.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Page header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("SETTINGS", style = MaterialTheme.typography.headlineMedium, color = SbbColors.TextPrimary)
            Text(
                "[←]",
                style    = MaterialTheme.typography.bodyLarge,
                color    = SbbColors.TextMuted,
                modifier = Modifier.clickable { onBack() },
            )
        }

        Divider()

        // Limit scrubber
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            val hrs = (limitSlider / 60).toInt()
            val mins = (limitSlider % 60).toInt()
            val display = if (hrs > 0) "${hrs}H ${mins}M" else "${mins}M"

            Text(
                text = "Nudge me after $display total on watch list apps per day",
                style = MaterialTheme.typography.labelLarge,
                color = SbbColors.TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            DailyLimitRow(
                minutes = limitSlider.roundToInt(),
                onMinutesChange = { viewModel.setDailyLimitMinutes(it.toInt()) }
            )
        }
        SectionDivider()

        // ── Pinned Dock ──────────────────────────────────────────────────────

        SectionLabel("PINNED DOCK")
        Text(
            "TAP ANY SLOT TO SWAP",
            style    = MaterialTheme.typography.labelSmall,
            color    = SbbColors.TextDim,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier              = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.pinnedApps.forEachIndexed { index, app ->
                AppIconSlot(
                    app     = app,
                    onClick = { showDockPicker = index },
                )
            }
        }

        SectionDivider()

        // ── Watch List ───────────────────────────────────────────────────────

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text("WATCH LIST", style = MaterialTheme.typography.labelSmall, color = SbbColors.TextDim)
            Box(
                modifier = Modifier
                    .background(SbbColors.Surface)
                    .border(1.dp, SbbColors.Border)
                    .clickable { showAppPicker = AppFlag.WATCH }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text("+ ADD", style = MaterialTheme.typography.labelSmall, color = SbbColors.TextMuted)
            }
        }

        // Shared strip — removal wired directly to viewModel here
        var localWatchList by remember(prefs.watchList) { mutableStateOf(prefs.watchList) }
        AppIconStrip(
            packages  = localWatchList,
            allApps   = uiState.allApps,
            chipBg    = SbbColors.WatchRedBg,
            chipColor = SbbColors.TextPrimary,
            onRemove  = { pkg ->
                localWatchList = localWatchList - pkg
                viewModel.removeFromWatchList(pkg)
            },
            modifier  = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )

        SectionDivider()

        // ── Plant ────────────────────────────────────────────────────────────

        SectionLabel("PLANT")

        SettingRow(
            title = "Current streak",
            sub   = "${streak.currentStreak} consecutive days within goal",
            right = {
                Text(
                    buildString { repeat(7) { i -> append(if (i < streak.currentStreak) "▮" else "▯") } },
                    style = MaterialTheme.typography.labelLarge,
                    color = SbbColors.PlantGreen,
                )
            },
        )

        SettingRow(
            title = "Reset streak",
            sub   = "Start over from day 1",
            right = {
                Text(
                    "[RESET]",
                    style    = MaterialTheme.typography.bodyLarge,
                    color    = SbbColors.WatchRed,
                    modifier = Modifier.clickable { viewModel.resetStreak() },
                )
            },
        )

        Spacer(Modifier.height(40.dp))
    }
}

// ── AppPickerScreen ───────────────────────────────────────────────────────────

@Composable
fun AppPickerScreen(
    apps: List<AppInfo>,
    title: String,
    currentSlots: List<String>,
    activeSlot: Int,
    onPick: (AppInfo) -> Unit,
    onBack: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SbbColors.Background),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text("PICK APP", style = MaterialTheme.typography.headlineMedium, color = SbbColors.TextPrimary)
            Text(
                "[←]",
                style    = MaterialTheme.typography.bodyLarge,
                color    = SbbColors.TextMuted,
                modifier = Modifier.clickable { onBack() },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SbbColors.Surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = SbbColors.TextMuted)
        }

        Divider()

        Box(modifier = Modifier.weight(1f)) {
            LineWheelScroll(
                items          = apps,
                selectedIndex  = selectedIndex,
                onItemSelected = { selectedIndex = it },
                thumbColor     = SbbColors.TextSecondary,
            ) { app, isFocused, scale ->
                AppRow(
                    app = app,
                    isFocused = isFocused,
                    scale = scale,
                    onClick = { onPick(app) },
                )
            }
        }

        // Dock preview
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SbbColors.Surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            currentSlots.forEachIndexed { i, pkg ->
                val isActive = i == activeSlot
                val app = apps.find { it.packageName == pkg }
                if (isActive) {
                    // Mirror EmptyDockSlot but with green highlight to show "this slot"
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(SbbColors.GoalGreenBg)
                                .border(2.dp, SbbColors.PlantGreen),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("?", style = MaterialTheme.typography.bodyLarge, color = SbbColors.PlantGreen)
                        }
                        Text("SLOT ${i + 1}", style = MaterialTheme.typography.labelSmall, color = SbbColors.PlantGreen)
                    }
                } else {
                    AppIconSlot(
                        app  = app,
                        size = 52.dp,
                    )
                }
            }
        }
    }
}

// ── AddAppPickerScreen ────────────────────────────────────────────────────────

@Composable
fun AddAppPickerScreen(
    apps: List<AppInfo>,
    watchList: Set<String>,
    targetFlag: AppFlag,
    onAdd: (AppInfo) -> Unit,
    onRemove: (String) -> Unit,
    onBack: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var currentTarget by remember { mutableStateOf(targetFlag) }

    // Local copy so strip updates immediately without waiting for DataStore round-trip
    var localWatchList by remember(watchList) { mutableStateOf(watchList) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SbbColors.Background),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text("ADD APP", style = MaterialTheme.typography.headlineMedium, color = SbbColors.TextPrimary)
            Text(
                "[←]",
                style    = MaterialTheme.typography.bodyLarge,
                color    = SbbColors.TextMuted,
                modifier = Modifier.clickable { onBack() },
            )
        }

        // Mode toggle banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SbbColors.Surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text("ADDING TO", style = MaterialTheme.typography.labelLarge, color = SbbColors.TextMuted)
            Row(modifier = Modifier.border(1.dp, SbbColors.BorderStrong)) {
                listOf(AppFlag.WATCH to "WATCH LIST").forEach { (flag, label) ->
                    val selected  = currentTarget == flag
                    val bgColor   = if (selected && flag == AppFlag.WATCH) SbbColors.WatchRedBg else SbbColors.Surface
                    val textColor = if (selected && flag == AppFlag.WATCH) SbbColors.WatchRed else SbbColors.TextMuted
                    Text(
                        text     = label,
                        style    = MaterialTheme.typography.labelLarge,
                        color    = textColor,
                        modifier = Modifier
                            .background(bgColor)
                            .clickable { currentTarget = flag }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Divider()

        Box(modifier = Modifier.weight(1f)) {
            LineWheelScroll(
                items          = apps,
                selectedIndex  = selectedIndex,
                onItemSelected = { selectedIndex = it },
                thumbColor     = if (currentTarget == AppFlag.WATCH) SbbColors.WatchRed else SbbColors.GoalGreen,
            ) { app, isFocused, scale ->
                val existingFlag  = if (app.packageName in localWatchList) AppFlag.WATCH else AppFlag.NONE
                val alreadyAdded  = existingFlag == currentTarget
                AppRow(
                    app = app,
                    flag = existingFlag,
                    isFocused = isFocused,
                    scale = scale,
                    onClick = { if (!alreadyAdded) onAdd(app) },
                )
            }
        }

        // Current list summary footer — uses shared AppIconStrip
        Divider()
        val currentList  = if (currentTarget == AppFlag.WATCH) localWatchList else emptySet()
        val footerLabel  = if (currentTarget == AppFlag.WATCH) "CURRENT WATCH LIST" else "CURRENT GOAL APPS"
        val chipBg       = if (currentTarget == AppFlag.WATCH) SbbColors.WatchRedBg else SbbColors.GoalGreenBg
        val chipColor    = SbbColors.TextPrimary   // consistent with Settings screen

        Column(
            modifier = Modifier
                .background(SbbColors.Surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(footerLabel, style = MaterialTheme.typography.labelSmall, color = SbbColors.TextDim)
            Spacer(Modifier.height(6.dp))
            AppIconStrip(
                packages  = currentList,
                allApps   = apps,
                chipBg    = chipBg,
                chipColor = chipColor,
                onRemove  = { pkg ->
                    localWatchList = localWatchList - pkg
                    onRemove(pkg)
                },
            )
        }
    }
}

// ── Settings helpers ──────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelSmall,
        color    = SbbColors.TextDim,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(8.dp))
    Divider()
}

@Composable
private fun SettingRow(
    title: String,
    sub: String,
    right: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = SbbColors.TextPrimary)
            Text(sub, style = MaterialTheme.typography.labelLarge, color = SbbColors.TextMuted)
        }
        right()
    }
}