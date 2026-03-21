package com.example.stopbreathbelauncher.ui.screens

import android.app.AppOpsManager
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stopbreathbelauncher.data.GoalMode
import com.example.stopbreathbelauncher.data.PlantState
import com.example.stopbreathbelauncher.ui.components.AppFlag
import com.example.stopbreathbelauncher.ui.components.AppRow
import com.example.stopbreathbelauncher.ui.components.PlantDisplay
import com.example.stopbreathbelauncher.ui.scroll.LineWheelScroll
import com.example.stopbreathbelauncher.ui.theme.SbbColors
import com.example.stopbreathbelauncher.ui.theme.SbbScaffold
import com.example.stopbreathbelauncher.ui.theme.StopBreathBeLauncherTheme
import com.example.stopbreathbelauncher.ui.viewmodel.AppInfo
import com.example.stopbreathbelauncher.ui.viewmodel.LauncherViewModel

class OnboardingActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFullyConfigured()) {
            goToLauncher()
            return
        }

        setContent {
            StopBreathBeLauncherTheme {
                SbbScaffold {
                    val uiState by viewModel.uiState.collectAsState()
                    OnboardingFlow(
                        allApps          = uiState.allApps,
                        hasUsagePermission = ::hasUsagePermission,
                        isDefaultLauncher  = ::isDefaultLauncher,
                        onGrantUsage       = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                        onSetDefault       = { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) },
                        onSetGoalMode      = { viewModel.setGoalMode(it) },
                        onSetPinnedApps    = { viewModel.setPinnedApps(it) },
                        onSetWatchList     = { pkg, add ->
                            if (add) viewModel.addToWatchList(pkg)
                            else viewModel.removeFromWatchList(pkg)
                        },
                        onSetGoalApps      = { pkg, add ->
                            if (add) viewModel.addToGoalApps(pkg)
                            else viewModel.removeFromGoalApps(pkg)
                        },
                        onFinish = {
                            viewModel.setOnboardingComplete()
                            goToLauncher()
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
        if (isFullyConfigured()) goToLauncher()
    }

    private fun isFullyConfigured() = hasUsagePermission() && isDefaultLauncher()

    private fun hasUsagePermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        return packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName == packageName
    }

    private fun goToLauncher() {
        startActivity(Intent(this, LauncherActivity::class.java))
        finish()
    }
}

// ── Onboarding flow ───────────────────────────────────────────────────────────

@Composable
fun OnboardingFlow(
    allApps: List<AppInfo>,
    hasUsagePermission: () -> Boolean,
    isDefaultLauncher: () -> Boolean,
    onGrantUsage: () -> Unit,
    onSetDefault: () -> Unit,
    onSetGoalMode: (GoalMode) -> Unit,
    onSetPinnedApps: (List<String>) -> Unit,
    onSetWatchList: (String, Boolean) -> Unit,
    onSetGoalApps: (String, Boolean) -> Unit,
    onFinish: () -> Unit,
) {
    var step by remember { mutableIntStateOf(1) }
    var selectedGoal by remember { mutableStateOf(GoalMode.REDUCE) }
    var pinnedPackages by remember { mutableStateOf<List<String>>(emptyList()) }
    var watchList by remember { mutableStateOf<Set<String>>(emptySet()) }
    var goalApps by remember { mutableStateOf<Set<String>>(emptySet()) }

    val totalSteps = if (selectedGoal == GoalMode.REDIRECT) 7 else 6

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SbbColors.Background),
    ) {
        // Progress bar
        StepProgress(current = step, total = totalSteps)

        Box(modifier = Modifier.weight(1f)) {
            when (step) {
                1 -> StepWelcome(onNext = { step = 2 })
                2 -> StepUsagePermission(
                    isGranted  = hasUsagePermission(),
                    onGrant    = onGrantUsage,
                    onNext     = { step = 3 },
                )
                3 -> StepDefaultLauncher(
                    isDefault  = isDefaultLauncher(),
                    onSet      = onSetDefault,
                    onNext     = { step = 4 },
                )
                4 -> StepGoalMode(
                    selected   = selectedGoal,
                    onSelect   = {
                        selectedGoal = it
                        onSetGoalMode(it)
                    },
                    onNext     = { step = 5 },
                )
                5 -> StepPinnedApps(
                    allApps    = allApps,
                    pinned     = pinnedPackages,
                    onToggle   = { pkg ->
                        pinnedPackages = if (pinnedPackages.contains(pkg))
                            pinnedPackages - pkg
                        else if (pinnedPackages.size < 4)
                            pinnedPackages + pkg
                        else pinnedPackages
                    },
                    onNext     = {
                        onSetPinnedApps(pinnedPackages)
                        step = 6
                    },
                )
                6 -> StepWatchList(
                    allApps    = allApps,
                    watchList  = watchList,
                    onToggle   = { pkg ->
                        watchList = if (watchList.contains(pkg)) watchList - pkg else watchList + pkg
                        onSetWatchList(pkg, !watchList.contains(pkg))
                    },
                    onNext     = {
                        if (selectedGoal == GoalMode.REDIRECT) step = 7
                        else onFinish()
                    },
                    isLastStep = selectedGoal == GoalMode.REDUCE,
                )
                7 -> StepGoalApps(
                    allApps   = allApps,
                    goalApps  = goalApps,
                    onToggle  = { pkg ->
                        goalApps = if (goalApps.contains(pkg)) goalApps - pkg else goalApps + pkg
                        onSetGoalApps(pkg, !goalApps.contains(pkg))
                    },
                    onNext    = onFinish,
                )
            }
        }
    }
}

// ── Step 1: Welcome ───────────────────────────────────────────────────────────

@Composable
private fun StepWelcome(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            StepLabel("SETUP_01")
            Spacer(Modifier.height(8.dp))
            Text("STOP.\nBREATHE.\nBE.", style = MaterialTheme.typography.headlineLarge, color = SbbColors.TextPrimary)
            Spacer(Modifier.height(16.dp))
            Text(
                "A launcher that makes every phone open a conscious choice.\nTakes 2 minutes to set up.",
                style = MaterialTheme.typography.bodyLarge,
                color = SbbColors.TextSecondary,
                modifier = Modifier
                    .border(1.dp, SbbColors.Border)
                    .padding(10.dp),
            )
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PlantDisplay(state = PlantState.HEALTHY, modifier = Modifier.padding(16.dp))
        }

        OnboardingButton("[ BEGIN ]", onClick = onNext)
    }
}

// ── Step 2: Usage permission ──────────────────────────────────────────────────

@Composable
private fun StepUsagePermission(isGranted: Boolean, onGrant: () -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            StepLabel("SETUP_02")
            Text("USAGE ACCESS", style = MaterialTheme.typography.headlineLarge, color = SbbColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            Text("So we can show how much time you spend in each app. We never share this data.",
                style = MaterialTheme.typography.bodyLarge, color = SbbColors.TextSecondary,
                modifier = Modifier.border(1.dp, SbbColors.Border).padding(10.dp))
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.border(1.dp, SbbColors.Border).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("WHAT WE READ", style = MaterialTheme.typography.labelLarge, color = SbbColors.TextMuted)
                Text("▮ Time spent per app today\n▮ Number of times opened\n▯ Nothing else",
                    style = MaterialTheme.typography.bodyLarge, color = SbbColors.TextSecondary)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!isGranted) {
                OnboardingButton("[ GRANT PERMISSION → ]", onClick = onGrant)
                Text("WAITING FOR PERMISSION...", style = MaterialTheme.typography.labelSmall,
                    color = SbbColors.TextDim, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                OnboardingButton("[ PERMISSION GRANTED — NEXT → ]", onClick = onNext, highlight = true)
            }
        }
    }
}

// ── Step 3: Default launcher ──────────────────────────────────────────────────

@Composable
private fun StepDefaultLauncher(isDefault: Boolean, onSet: () -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            StepLabel("SETUP_03")
            Text("SET AS DEFAULT", style = MaterialTheme.typography.headlineLarge, color = SbbColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            Text("We'll open Settings. Select this app under Home App.",
                style = MaterialTheme.typography.bodyLarge, color = SbbColors.TextSecondary,
                modifier = Modifier.border(1.dp, SbbColors.Border).padding(10.dp))
            Spacer(Modifier.height(24.dp))
            Column(modifier = Modifier.border(1.dp, SbbColors.Border).padding(16.dp)) {
                listOf("Settings → Apps", "→ Default Apps", "→ Home App", "→ StopBreathBe").forEachIndexed { i, line ->
                    Text(line, style = MaterialTheme.typography.bodyLarge,
                        color = if (i == 3) SbbColors.PlantGreen else SbbColors.TextSecondary)
                    if (i < 3) Spacer(Modifier.height(4.dp))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!isDefault) {
                OnboardingButton("[ OPEN SETTINGS → ]", onClick = onSet)
                Text("WAITING FOR DEFAULT...", style = MaterialTheme.typography.labelSmall,
                    color = SbbColors.TextDim, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                OnboardingButton("[ DEFAULT SET — NEXT → ]", onClick = onNext, highlight = true)
            }
        }
    }
}

// ── Step 4: Goal mode ─────────────────────────────────────────────────────────

@Composable
private fun StepGoalMode(selected: GoalMode, onSelect: (GoalMode) -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            StepLabel("SETUP_04")
            Text("YOUR GOAL", style = MaterialTheme.typography.headlineLarge, color = SbbColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text("What are you here to change?", style = MaterialTheme.typography.bodyLarge, color = SbbColors.TextSecondary)
            Spacer(Modifier.height(24.dp))

            listOf(
                GoalMode.REDUCE   to ("REDUCE" to "Use my phone less.\nSet a daily limit. Get nudged when you're close."),
                GoalMode.REDIRECT to ("REDIRECT" to "Use my phone better.\nBalance time on bad apps with time on good ones."),
            ).forEach { (mode, pair) ->
                val (tag, desc) = pair
                val isSelected = selected == mode
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, if (isSelected) SbbColors.PlantGreen else SbbColors.Border)
                        .background(if (isSelected) SbbColors.GoalGreenBg else SbbColors.Surface)
                        .clickable { onSelect(mode) }
                        .padding(16.dp),
                ) {
                    Box(modifier = Modifier
                        .background(if (isSelected) SbbColors.GoalGreenBg else SbbColors.SurfaceVariant)
                        .border(1.dp, if (isSelected) SbbColors.GoalGreenBorder else SbbColors.Border)
                        .padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text(tag, style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) SbbColors.GoalGreenLight else SbbColors.TextMuted)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(if (mode == GoalMode.REDUCE) "Use my phone less" else "Use my phone better",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isSelected) SbbColors.PlantGreen else SbbColors.TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(desc, style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) SbbColors.GoalGreenLight else SbbColors.TextMuted)
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        OnboardingButton("[ NEXT → ]", onClick = onNext)
    }
}

// ── Step 5: Pinned apps ───────────────────────────────────────────────────────

@Composable
private fun StepPinnedApps(
    allApps: List<AppInfo>,
    pinned: List<String>,
    onToggle: (String) -> Unit,
    onNext: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val remaining = 4 - pinned.size

    Column(modifier = Modifier.fillMaxSize().background(SbbColors.Background)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            StepLabel("SETUP_05")
            Text("PICK 4 DOCK APPS", style = MaterialTheme.typography.headlineLarge, color = SbbColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Your always-visible shortcuts.", style = MaterialTheme.typography.bodyLarge, color = SbbColors.TextSecondary)
            Spacer(Modifier.height(12.dp))

            // Dock preview
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) { i ->
                    val pkg = pinned.getOrNull(i)
                    val label = allApps.find { it.packageName == pkg }?.label?.take(3) ?: "?"
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(2.dp, if (pkg != null) SbbColors.PlantGreen else SbbColors.Border)
                            .background(if (pkg != null) SbbColors.GoalGreenBg else SbbColors.Surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (pkg != null) label.uppercase() else "?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (pkg != null) SbbColors.PlantGreen else SbbColors.TextDim)
                    }
                }
            }
        }

        Divider()

        Box(modifier = Modifier.weight(1f)) {
            LineWheelScroll(
                items          = allApps,
                selectedIndex  = selectedIndex,
                onItemSelected = { selectedIndex = it },
            ) { app, isFocused, scale ->
                val isPinned = pinned.contains(app.packageName)
                AppRow(
                    app       = app,
                    flag      = if (isPinned) AppFlag.GOAL else AppFlag.NONE,
                    isFocused = isFocused,
                    scale     = scale,
                    onClick   = { onToggle(app.packageName) },
                    hint      = when {
                        isFocused && isPinned     -> "PINNED ✓ — TAP TO REMOVE"
                        isFocused && !isPinned    -> "TAP TO PIN"
                        else -> null
                    },
                )
            }
        }

        Divider()

        Box(modifier = Modifier.padding(16.dp)) {
            val canProceed = pinned.size == 4
            OnboardingButton(
                label   = if (canProceed) "[ NEXT → ]" else "[ PICK $remaining MORE ]",
                enabled = canProceed,
                onClick = onNext,
            )
        }
    }
}

// ── Step 6: Watch list ────────────────────────────────────────────────────────

@Composable
private fun StepWatchList(
    allApps: List<AppInfo>,
    watchList: Set<String>,
    onToggle: (String) -> Unit,
    onNext: () -> Unit,
    isLastStep: Boolean,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(SbbColors.Background)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            StepLabel("SETUP_06")
            Text("WATCH LIST", style = MaterialTheme.typography.headlineLarge, color = SbbColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Apps you want to use less. You'll get a nudge before opening these.",
                style = MaterialTheme.typography.bodyLarge, color = SbbColors.TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                watchList.take(4).forEach { pkg ->
                    val label = allApps.find { it.packageName == pkg }?.label?.take(8) ?: pkg.take(6)
                    Box(modifier = Modifier
                        .background(SbbColors.WatchRedBg)
                        .border(1.dp, SbbColors.WatchRedBorder)
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = SbbColors.WatchRedLight)
                    }
                }
            }
        }

        Divider()

        Box(modifier = Modifier.weight(1f)) {
            LineWheelScroll(
                items          = allApps,
                selectedIndex  = selectedIndex,
                onItemSelected = { selectedIndex = it },
                thumbColor     = SbbColors.WatchRed,
            ) { app, isFocused, scale ->
                val isWatched = watchList.contains(app.packageName)
                AppRow(
                    app       = app,
                    flag      = if (isWatched) AppFlag.WATCH else AppFlag.NONE,
                    isFocused = isFocused,
                    scale     = scale,
                    onClick   = { onToggle(app.packageName) },
                    hint      = when {
                        isFocused && isWatched  -> "WATCH ✓ — TAP TO REMOVE"
                        isFocused && !isWatched -> "TAP TO ADD TO WATCH LIST"
                        else -> null
                    },
                )
            }
        }

        Divider()

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OnboardingButton(
                label  = if (isLastStep) "[ FINISH ]" else "[ NEXT → ]",
                onClick = onNext,
            )
            Text(
                "OR SKIP FOR NOW",
                style    = MaterialTheme.typography.labelSmall,
                color    = SbbColors.TextDim,
                modifier = Modifier.align(Alignment.CenterHorizontally).clickable { onNext() },
            )
        }
    }
}

// ── Step 7: Goal apps (Redirect only) ─────────────────────────────────────────

@Composable
private fun StepGoalApps(
    allApps: List<AppInfo>,
    goalApps: Set<String>,
    onToggle: (String) -> Unit,
    onNext: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(SbbColors.Background)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            StepLabel("SETUP_07")
            Text("GOAL APPS", style = MaterialTheme.typography.headlineLarge, color = SbbColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Apps you want to use more. Time here helps your plant recover.",
                style = MaterialTheme.typography.bodyLarge, color = SbbColors.TextSecondary)
        }

        Divider()

        Box(modifier = Modifier.weight(1f)) {
            LineWheelScroll(
                items          = allApps,
                selectedIndex  = selectedIndex,
                onItemSelected = { selectedIndex = it },
                thumbColor     = SbbColors.GoalGreen,
            ) { app, isFocused, scale ->
                val isGoal = goalApps.contains(app.packageName)
                AppRow(
                    app       = app,
                    flag      = if (isGoal) AppFlag.GOAL else AppFlag.NONE,
                    isFocused = isFocused,
                    scale     = scale,
                    onClick   = { onToggle(app.packageName) },
                    hint      = when {
                        isFocused && isGoal  -> "GOAL ✓ — TAP TO REMOVE"
                        isFocused && !isGoal -> "TAP TO ADD TO GOAL APPS"
                        else -> null
                    },
                )
            }
        }

        Divider()

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OnboardingButton("[ FINISH ]", onClick = onNext)
            Text("OR SKIP FOR NOW",
                style    = MaterialTheme.typography.labelSmall,
                color    = SbbColors.TextDim,
                modifier = Modifier.align(Alignment.CenterHorizontally).clickable { onNext() })
        }
    }
}

// ── Done screen ────────────────────────────────────────────────────────────────

@Composable
private fun StepDone(onLaunch: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PlantDisplay(state = PlantState.HEALTHY)
            Spacer(Modifier.height(8.dp))
            Text("DAY_1 ▮▯▯▯▯▯▯", style = MaterialTheme.typography.labelLarge, color = SbbColors.PlantGreen)
            Spacer(Modifier.height(24.dp))
            Text("YOU'RE SET.", style = MaterialTheme.typography.headlineLarge, color = SbbColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Your plant is alive.\nKeep it that way.",
                style = MaterialTheme.typography.bodyLarge,
                color = SbbColors.TextSecondary)
        }
        OnboardingButton("[ LAUNCH → ]", onClick = onLaunch, highlight = true)
    }
}

// ── Shared onboarding components ──────────────────────────────────────────────

@Composable
private fun StepProgress(current: Int, total: Int) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(total) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .background(
                            when {
                                i < current - 1 -> SbbColors.PlantGreen
                                i == current - 1 -> SbbColors.GoalGreenBg
                                else -> SbbColors.Border
                            }
                        )
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("STEP $current OF $total", style = MaterialTheme.typography.labelSmall, color = SbbColors.TextDim)
    }
}

@Composable
private fun StepLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = SbbColors.TextDim)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun OnboardingButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    highlight: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlight) SbbColors.GoalGreenBg else SbbColors.Surface)
            .border(2.dp, when {
                !enabled  -> SbbColors.Border
                highlight -> SbbColors.PlantGreen
                else      -> SbbColors.BorderStrong
            })
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.headlineMedium,
            color = when {
                !enabled  -> SbbColors.TextDim
                highlight -> SbbColors.PlantGreen
                else      -> SbbColors.TextPrimary
            },
        )
    }
}

private fun Modifier.border(start: Int = 0, color: Color) = this.then(
    Modifier.padding(start = start.dp)
)
