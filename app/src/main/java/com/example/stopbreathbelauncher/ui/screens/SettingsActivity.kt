package com.example.stopbreathbelauncher.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.stopbreathbelauncher.ui.components.AppFlag
import com.example.stopbreathbelauncher.ui.components.AppIconSlot
import com.example.stopbreathbelauncher.ui.theme.SbbColors
import com.example.stopbreathbelauncher.ui.theme.SbbScaffold
import com.example.stopbreathbelauncher.ui.theme.StopBreathBeLauncherTheme
import com.example.stopbreathbelauncher.ui.viewmodel.AppInfo
import com.example.stopbreathbelauncher.ui.viewmodel.LauncherViewModel
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
                        uiState    = uiState,
                        viewModel  = viewModel,
                        onBack     = { finish() },
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    uiState: com.example.stopbreathbelauncher.ui.viewmodel.LauncherUiState,
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
) {
    val prefs  = uiState.preferences
    val streak = uiState.streakData

    var showDockPicker   by remember { mutableStateOf<Int?>(null) }           // slot index
    var showAppPicker    by remember { mutableStateOf<AppFlag?>(null) }       // which list

    var limitSlider by remember(prefs.dailyLimitMinutes) {
        mutableFloatStateOf(prefs.dailyLimitMinutes.toFloat())
    }
    var sliderInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(prefs.dailyLimitMinutes) {
        if (!sliderInitialized) {
            limitSlider = prefs.dailyLimitMinutes.toFloat()
            sliderInitialized = true
        }
    }

    // Dock picker overlay
    showDockPicker?.let { slotIndex ->
        AppPickerScreen(
            apps       = uiState.allApps,
            title      = "REPLACING SLOT ${slotIndex + 1}",
            currentSlots = uiState.pinnedApps.map { it?.packageName ?: "" },
            activeSlot = slotIndex,
            onPick     = { app ->
                viewModel.swapPinnedApp(slotIndex, app.packageName)
                showDockPicker = null
            },
            onBack     = { showDockPicker = null },
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
                showAppPicker = null
            },
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
            Text("[←]", style = MaterialTheme.typography.bodyLarge, color = SbbColors.TextMuted,
                modifier = Modifier.clickable { onBack() })
        }

        Divider()

        // Limit scrubber
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            val hrs = (limitSlider / 60).toInt()
            val mins = (limitSlider % 60).toInt()
            val display = if (hrs > 0) "${hrs}H ${mins}M" else "${mins}M"
            Text(
                text  = "Nudge me after $display total on watch list apps per day",
                style = MaterialTheme.typography.labelLarge,
                color = SbbColors.TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("30M", style = MaterialTheme.typography.labelLarge, color = SbbColors.TextDim)
                Slider(
                    value         = limitSlider,
                    onValueChange = { limitSlider = it },
                    onValueChangeFinished = { viewModel.setDailyLimitMinutes(limitSlider.roundToInt()) },
                    valueRange    = 30f..240f,
                    steps         = 13,
                    modifier      = Modifier.weight(1f),
                    colors        = SliderDefaults.colors(
                        thumbColor       = SbbColors.PlantGreen,
                        activeTrackColor = SbbColors.PlantGreenDark,
                        inactiveTrackColor = SbbColors.Border,
                    ),
                )
                Text("4H", style = MaterialTheme.typography.labelLarge, color = SbbColors.TextDim)
                Text(display, style = MaterialTheme.typography.titleLarge, color = SbbColors.PlantGreen,
                    modifier = Modifier.width(48.dp))
            }
        }

        SectionDivider()

        // ── Pinned Dock ─────────────────────────────────────────────────────

        SectionLabel("PINNED DOCK")
        Text("TAP ANY SLOT TO SWAP",
            style = MaterialTheme.typography.labelSmall,
            color = SbbColors.TextDim,
            modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.pinnedApps.forEachIndexed { index, app ->
                DockSlotEditor(
                    app     = app,
                    onClick = { showDockPicker = index },
                )
            }
        }

        SectionDivider()

        // ── Watch List ──────────────────────────────────────────────────────

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
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
        FlowChips(
            packages   = prefs.watchList,
            allApps    = uiState.allApps,
            chipColor  = SbbColors.TextPrimary,
            chipBg     = SbbColors.WatchRedBg,
            chipBorder = SbbColors.WatchRedBorder,
            onRemove   = { viewModel.removeFromWatchList(it) },
            onAdd      = { showAppPicker = AppFlag.WATCH },
        )

        SectionDivider()

        // ── Plant ───────────────────────────────────────────────────────────

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
            }
        )

        SettingRow(
            title = "Reset streak",
            sub   = "Start over from day 1",
            right = {
                Text("[RESET]", style = MaterialTheme.typography.bodyLarge, color = SbbColors.WatchRed,
                    modifier = Modifier.clickable { viewModel.resetStreak() })
            }
        )

        Spacer(Modifier.height(40.dp))
    }
}

// ── App picker for dock slot ───────────────────────────────────────────────────

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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("PICK APP", style = MaterialTheme.typography.headlineMedium, color = SbbColors.TextPrimary)
            Text("[←]", style = MaterialTheme.typography.bodyLarge, color = SbbColors.TextMuted,
                modifier = Modifier.clickable { onBack() })
        }

        // Context banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SbbColors.Surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = SbbColors.TextMuted)
        }

        Divider()

        Box(modifier = Modifier.weight(1f)) {
            com.example.stopbreathbelauncher.ui.scroll.LineWheelScroll(
                items          = apps,
                selectedIndex  = selectedIndex,
                onItemSelected = { selectedIndex = it },
                thumbColor     = SbbColors.TextSecondary,
            ) { app, isFocused, scale ->
                com.example.stopbreathbelauncher.ui.components.AppRow(
                    app       = app,
                    isFocused = isFocused,
                    scale     = scale,
                    onClick   = { onPick(app) },
                )
            }
        }

        // Dock preview at bottom
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SbbColors.Surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            currentSlots.forEachIndexed { i, pkg ->
                val isActive = i == activeSlot
                val label = apps.find { it.packageName == pkg }?.label?.take(5) ?: "?"
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(if (isActive) SbbColors.GoalGreenBg else SbbColors.SurfaceVariant)
                            .border(2.dp, if (isActive) SbbColors.PlantGreen else SbbColors.Border),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (isActive) "?" else label.take(2),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isActive) SbbColors.PlantGreen else SbbColors.TextMuted,
                        )
                    }
                    Text(
                        if (isActive) "SLOT ${i+1}" else label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) SbbColors.PlantGreen else SbbColors.TextMuted,
                    )
                }
            }
        }
    }
}

// ── Add app picker (watch/goal) ───────────────────────────────────────────────

@Composable
fun AddAppPickerScreen(
    apps: List<AppInfo>,
    watchList: Set<String>,
    targetFlag: AppFlag,
    onAdd: (AppInfo) -> Unit,
    onBack: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var currentTarget by remember { mutableStateOf(targetFlag) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SbbColors.Background),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ADD APP", style = MaterialTheme.typography.headlineMedium, color = SbbColors.TextPrimary)
            Text("[←]", style = MaterialTheme.typography.bodyLarge, color = SbbColors.TextMuted,
                modifier = Modifier.clickable { onBack() })
        }

        // Mode toggle banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SbbColors.Surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ADDING TO", style = MaterialTheme.typography.labelLarge, color = SbbColors.TextMuted)
            Row(modifier = Modifier.border(1.dp, SbbColors.BorderStrong)) {
                listOf(AppFlag.WATCH to "WATCH LIST").forEach { (flag, label) ->
                    val selected = currentTarget == flag
                    val bgColor = when {
                        selected && flag == AppFlag.WATCH -> SbbColors.WatchRedBg
                        else -> SbbColors.Surface
                    }
                    val textColor = when {
                        selected && flag == AppFlag.WATCH -> SbbColors.WatchRed
                        else -> SbbColors.TextMuted
                    }
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
            com.example.stopbreathbelauncher.ui.scroll.LineWheelScroll(
                items          = apps,
                selectedIndex  = selectedIndex,
                onItemSelected = { selectedIndex = it },
                thumbColor     = if (currentTarget == AppFlag.WATCH) SbbColors.WatchRed else SbbColors.GoalGreen,
            ) { app, isFocused, scale ->
                val existingFlag = when {
                    app.packageName in watchList -> AppFlag.WATCH
                    else -> AppFlag.NONE
                }
                val alreadyAdded = existingFlag == currentTarget
                com.example.stopbreathbelauncher.ui.components.AppRow(
                    app       = app,
                    flag      = existingFlag,
                    isFocused = isFocused,
                    scale     = scale,
                    onClick   = { if (!alreadyAdded) onAdd(app) },
                )
            }
        }

        // Current list summary
        Divider()
        val currentList = if (currentTarget == AppFlag.WATCH) watchList else emptyList()
        val label = if (currentTarget == AppFlag.WATCH) "CURRENT WATCH LIST" else "CURRENT GOAL APPS"
        val chipColor = if (currentTarget == AppFlag.WATCH) SbbColors.WatchRed else SbbColors.GoalGreenLight
        val chipBg = if (currentTarget == AppFlag.WATCH) SbbColors.WatchRedBg else SbbColors.GoalGreenBg
        val chipBorder = if (currentTarget == AppFlag.WATCH) SbbColors.WatchRedBorder else SbbColors.GoalGreenBorder

        Column(modifier = Modifier.background(SbbColors.Surface).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SbbColors.TextDim)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                currentList.take(5).forEach { pkg ->
                    val appLabel = apps.find { it.packageName == pkg }?.label?.take(8) ?: pkg.take(8)
                    Box(
                        modifier = Modifier
                            .background(chipBg)
                            .border(1.dp, chipBorder)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(appLabel.uppercase(), style = MaterialTheme.typography.labelSmall, color = chipColor)
                    }
                }
                if (currentList.isEmpty()) {
                    Text("NONE YET", style = MaterialTheme.typography.labelSmall, color = SbbColors.TextDim)
                }
            }
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
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = SbbColors.TextPrimary)
            Text(sub, style = MaterialTheme.typography.labelLarge, color = SbbColors.TextMuted)
        }
        right()
    }
}

@Composable
private fun DockSlotEditor(app: AppInfo?, onClick: () -> Unit) {
    AppIconSlot(
        app         = app,
        onClick     = onClick,
    )
}
@Composable
private fun FlowChips(
    packages: Set<String>,
    allApps: List<AppInfo>,
    chipColor: androidx.compose.ui.graphics.Color,
    chipBg: androidx.compose.ui.graphics.Color,
    chipBorder: androidx.compose.ui.graphics.Color,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    var localPackages by remember(packages) { mutableStateOf(packages) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        localPackages.forEach { pkg ->
            val app = allApps.find { it.packageName == pkg }
            AppIconSlot(
                app         = app,
                bgColor     = chipBg,
                badge = {
                    Text(
                        "×",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = chipColor.copy(alpha = 0.6f),
                        modifier = Modifier.clickable {
                            localPackages = localPackages - pkg
                            onRemove(pkg)
                        },
                    )
                },
            )
        }
    }
}