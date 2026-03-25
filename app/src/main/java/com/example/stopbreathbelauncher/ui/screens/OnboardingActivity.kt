package com.example.stopbreathbelauncher.ui.screens

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.stopbreathbelauncher.data.PlantState
import com.example.stopbreathbelauncher.ui.components.AppFlag
import com.example.stopbreathbelauncher.ui.components.AppRow
import com.example.stopbreathbelauncher.ui.components.DailyLimitRow
import com.example.stopbreathbelauncher.ui.components.PlantDisplay
import com.example.stopbreathbelauncher.ui.scroll.LineWheelScroll
import com.example.stopbreathbelauncher.ui.theme.SbbColors
import com.example.stopbreathbelauncher.ui.theme.SbbScaffold
import com.example.stopbreathbelauncher.ui.theme.StopBreathBeLauncherTheme
import com.example.stopbreathbelauncher.ui.viewmodel.AppInfo
import com.example.stopbreathbelauncher.ui.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            if (isFullyConfigured()) {
                goToLauncher()
            } else {
                setContent {
                    StopBreathBeLauncherTheme {
                        SbbScaffold {
                            val uiState by viewModel.uiState.collectAsState()
                            OnboardingFlow(
                                allApps            = uiState.allApps,
                                initialPinnedApps  = uiState.preferences.pinnedApps,
                                hasUsagePermission = ::hasUsagePermission,
                                isDefaultLauncher  = ::isDefaultLauncher,
                                onGrantUsage       = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                                onSetDefault       = { startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) },
                                onSetPinnedApps    = { viewModel.setPinnedApps(it) },
                                onSetWatchList     = { pkg, add -> if (add) viewModel.addToWatchList(pkg) else viewModel.removeFromWatchList(pkg) },
                                onSetDailyLimitMinutes = { viewModel.setDailyLimitMinutes(it) },
                                onFinish = { goToLauncher() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()

        lifecycleScope.launch {
            if (isFullyConfigured()) goToLauncher()
        }
    }

    private suspend fun isFullyConfigured(): Boolean {
        val onboardingComplete = viewModel.isOnboardingComplete()
        // Default launcher is now last step, not required here
        return hasUsagePermission() && onboardingComplete
    }

    private fun hasUsagePermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun isDefaultLauncher(): Boolean {
        val filters = arrayListOf<IntentFilter>()
        val activities = arrayListOf<ComponentName>()
        packageManager.getPreferredActivities(filters, activities, packageName)
        for (filter in filters) {
            if (filter.hasAction(Intent.ACTION_MAIN) && filter.hasCategory(Intent.CATEGORY_HOME)) {
                return true
            }
        }
        return false
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
    initialPinnedApps: List<String>,
    hasUsagePermission: () -> Boolean,
    isDefaultLauncher: () -> Boolean,
    onGrantUsage: () -> Unit,
    onSetDefault: () -> Unit,
    onSetPinnedApps: (List<String>) -> Unit,
    onSetWatchList: (String, Boolean) -> Unit,
    onSetDailyLimitMinutes: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    var step by remember {
        mutableIntStateOf(
            when {
                !hasUsagePermission() -> 1
                else -> 2
            }
        )
    }

    var pinnedPackages by remember(initialPinnedApps) { mutableStateOf(initialPinnedApps) }
    var watchList by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSlot by remember { mutableIntStateOf(0) }

    val totalSteps = 6

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SbbColors.Background),
    ) {
        StepProgress(current = step, total = totalSteps)

        Box(modifier = Modifier.weight(1f)) {
            when (step) {
                1 -> StepWelcome(onNext = { step = 2 })
                2 -> StepUsagePermission(
                    isGranted = hasUsagePermission(),
                    onGrant   = onGrantUsage,
                    onNext    = { step = 3 },
                )
                3 -> StepPinnedApps(
                    allApps      = allApps,
                    pinned       = pinnedPackages,
                    selectedSlot = selectedSlot,
                    onSlotTap    = { selectedSlot = it },
                    onAppSelected = { pkg ->
                        val list = pinnedPackages.toMutableList()
                        while (list.size <= selectedSlot) list.add("")
                        list[selectedSlot] = pkg
                        pinnedPackages = list.filter { it.isNotEmpty() }
                        selectedSlot = (selectedSlot + 1).coerceAtMost(3)
                    },
                    onNext = {
                        onSetPinnedApps(pinnedPackages)
                        step = 4
                    },
                )
                4 -> StepWatchList(
                    allApps   = allApps,
                    watchList = watchList,
                    onToggle  = { pkg ->
                        watchList = if (watchList.contains(pkg)) watchList - pkg else watchList + pkg
                        onSetWatchList(pkg, watchList.contains(pkg))
                    },
                    onNext = { step = 5 },
                )
                5 -> StepDailyLimit(
                    onSet = { onSetDailyLimitMinutes(it) },
                    onNext = { step = 6 }
                )
                6 -> StepDefaultLauncher(
                    isDefault = isDefaultLauncher(),
                    onSet     = onSetDefault,
                    onNext    = {
                        // mark onboarding complete and launch home
                        onFinish()
                    },
                )
            }
        }
    }
}

// ── Remaining Step Composables ───────────────────────────────────────────────
// StepWelcome, StepUsagePermission, StepPinnedApps, StepWatchList, StepDailyLimit, StepDefaultLauncher
// StepProgress, StepLabel, OnboardingButton
// [Same as your previous implementations, just updated ordering]
// You can copy the same Composables from your current file.
// ── Step 1: Welcome ───────────────────────────────────────────────────────────

@Composable
private fun StepWelcome(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
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
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            StepLabel("SETUP_02")
            Text("USAGE ACCESS", style = MaterialTheme.typography.headlineLarge, color = SbbColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            Text(
                "So we can show how much time you spend in each app. We never share this data.",
                style = MaterialTheme.typography.bodyLarge,
                color = SbbColors.TextSecondary,
                modifier = Modifier
                    .border(1.dp, SbbColors.Border)
                    .padding(10.dp),
            )
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .border(1.dp, SbbColors.Border)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("WHAT WE READ", style = MaterialTheme.typography.labelLarge, color = SbbColors.TextMuted)
                Text(
                    "▮ Time spent per app today\n▮ Number of times opened\n▯ Nothing else",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SbbColors.TextSecondary,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!isGranted) {
                OnboardingButton("[ GRANT PERMISSION → ]", onClick = onGrant)
                Text(
                    "WAITING FOR PERMISSION...",
                    style = MaterialTheme.typography.labelSmall,
                    color = SbbColors.TextDim,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            } else {
                OnboardingButton("[ PERMISSION GRANTED — NEXT → ]", onClick = onNext, highlight = true)
            }
        }
    }
}

// ── Step 3: Default launcher ──────────────────────────────────────────────────

@Composable
private fun StepDefaultLauncher(isDefault: Boolean, onSet: () -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            StepLabel("SETUP_03")
            Text("SET AS DEFAULT", style = MaterialTheme.typography.headlineLarge, color = SbbColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            Text(
                "We'll open Settings. Select this app under Home App.",
                style = MaterialTheme.typography.bodyLarge,
                color = SbbColors.TextSecondary,
                modifier = Modifier
                    .border(1.dp, SbbColors.Border)
                    .padding(10.dp),
            )
            Spacer(Modifier.height(24.dp))
            Column(modifier = Modifier
                .border(1.dp, SbbColors.Border)
                .padding(16.dp)) {
                listOf("Settings → Apps", "→ Default Apps", "→ Home App", "→ StopBreathBe").forEachIndexed { i, line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (i == 3) SbbColors.PlantGreen else SbbColors.TextSecondary,
                    )
                    if (i < 3) Spacer(Modifier.height(4.dp))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!isDefault) {
                OnboardingButton("[ OPEN SETTINGS → ]", onClick = onSet)
                Text(
                    "WAITING FOR DEFAULT...",
                    style = MaterialTheme.typography.labelSmall,
                    color = SbbColors.TextDim,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            } else {
                OnboardingButton("[ DEFAULT SET — NEXT → ]", onClick = onNext, highlight = true)
            }
        }
    }
}

// ── Step 4: Pinned apps ───────────────────────────────────────────────────────

@Composable
private fun StepPinnedApps(
    allApps: List<AppInfo>,
    pinned: List<String>,
    selectedSlot: Int,
    onSlotTap: (Int) -> Unit,
    onAppSelected: (String) -> Unit,
    onNext: () -> Unit,
) {
    var scrollIndex by remember { mutableIntStateOf(0) }
    val remaining = 4 - pinned.size

    Column(modifier = Modifier
        .fillMaxSize()
        .background(SbbColors.Background)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            StepLabel("SETUP_04")
            Text("PICK 4 DOCK APPS", style = MaterialTheme.typography.headlineLarge, color = SbbColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap a slot to select it, then tap an app to assign it.",
                style = MaterialTheme.typography.bodyLarge,
                color = SbbColors.TextSecondary,
            )
            Spacer(Modifier.height(12.dp))

            // Dock preview slots
            val pinnedAppInfos = List(4) { i -> allApps.find { it.packageName == pinned.getOrNull(i) } }

            Dock(
                pinnedApps   = pinnedAppInfos,
                onAppClick   = { },
                selectedSlot = selectedSlot,
                onSlotTap    = onSlotTap,
            )
        }

        Divider()

        Box(modifier = Modifier.weight(1f)) {
            LineWheelScroll(
                items          = allApps,
                selectedIndex  = scrollIndex,
                onItemSelected = { scrollIndex = it },
            ) { app, isFocused, scale ->
                val isPinned = pinned.contains(app.packageName)
                AppRow(
                    app       = app,
                    flag      = if (isPinned) AppFlag.NONE else AppFlag.NONE,
                    isFocused = isFocused,
                    scale     = scale,
                    onClick   = { onAppSelected(app.packageName) },
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

// ── Step 5: Watch list ────────────────────────────────────────────────────────

@Composable
private fun StepWatchList(
    allApps: List<AppInfo>,
    watchList: Set<String>,
    onToggle: (String) -> Unit,
    onNext: () -> Unit,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var localWatchList by remember { mutableStateOf(watchList) }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(SbbColors.Background)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            StepLabel("SETUP_05")
            Text("WATCH LIST", style = MaterialTheme.typography.headlineLarge, color = SbbColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Apps you want to use less. You'll get a nudge before opening these.",
                style = MaterialTheme.typography.bodyLarge,
                color = SbbColors.TextSecondary,
            )
            Spacer(Modifier.height(8.dp))


            AppIconStrip(
                packages  = localWatchList,
                allApps   = allApps,
                chipBg    = SbbColors.WatchRedBg,
                chipColor = SbbColors.TextPrimary,
                onRemove  = { pkg ->
                    localWatchList = localWatchList - pkg
                    onToggle(pkg) // update ViewModel
                },
                modifier  = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
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
                    onClick   = {
                        onToggle(app.packageName)
                        localWatchList = localWatchList + app.packageName
                    },
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
            OnboardingButton(label = "[ FINISH ]", onClick = onNext)
            Text(
                "OR SKIP FOR NOW",
                style    = MaterialTheme.typography.labelSmall,
                color    = SbbColors.TextDim,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onNext() },
            )
        }
    }
}

// ── Step 6: Daily Limit ────────────────────────────────────────────────────────

@Composable
private fun StepDailyLimit(
    onSet: (Int) -> Unit,
    onNext: () -> Unit,
) {
    var minutes by remember { mutableIntStateOf(120) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            StepLabel("SETUP_06")
            Text(
                "DAILY LIMIT",
                style = MaterialTheme.typography.headlineLarge,
                color = SbbColors.TextPrimary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "How many minutes per day for your watch list?",
                style = MaterialTheme.typography.bodyLarge,
                color = SbbColors.TextSecondary,
                modifier = Modifier
                    .border(1.dp, SbbColors.Border)
                    .padding(10.dp),
            )

            Spacer(Modifier.height(24.dp))

            DailyLimitRow(
                minutes = minutes,
                onMinutesChange = { newValue ->
                    minutes = newValue.toInt()
                    onSet(newValue.toInt())
                }
            )
        }

        OnboardingButton(
            "[ SET LIMIT → ]",
            onClick = {
                onSet(minutes)
                onNext()
            },
            highlight = true
        )
    }
}

// ── Done screen ───────────────────────────────────────────────────────────────

@Composable
private fun StepDone(onLaunch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
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
            Text(
                "Your plant is alive.\nKeep it that way.",
                style = MaterialTheme.typography.bodyLarge,
                color = SbbColors.TextSecondary,
            )
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
            .border(
                2.dp, when {
                    !enabled -> SbbColors.Border
                    highlight -> SbbColors.PlantGreen
                    else -> SbbColors.BorderStrong
                }
            )
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

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SbbColors.Border)
    )
}