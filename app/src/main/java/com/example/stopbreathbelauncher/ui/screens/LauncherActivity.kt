package com.example.stopbreathbelauncher.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.stopbreathbelauncher.ui.components.NudgeDialog
import com.example.stopbreathbelauncher.ui.theme.SbbScaffold
import com.example.stopbreathbelauncher.ui.theme.StopBreathBeLauncherTheme
import com.example.stopbreathbelauncher.ui.viewmodel.AppInfo
import com.example.stopbreathbelauncher.ui.viewmodel.LauncherViewModel

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StopBreathBeLauncherTheme {
                SbbScaffold {
                    val uiState by viewModel.uiState.collectAsState()
                    val nudgeApp by viewModel.nudgeApp.collectAsState()
                    val pagerState = rememberPagerState(pageCount = { 2 })

                    HorizontalPager(
                        state    = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        when (page) {
                            0 -> HomeScreen(
                                state          = uiState,
                                onAppClick     = { app -> handleAppClick(app) },
                                onSettingsClick = { openSettings() },
                            )
                            1 -> AllAppsScreen(
                                state          = uiState,
                                onAppClick     = { app -> handleAppClick(app) },
                                onAppLongClick = { app -> toggleFlag(app) },
                            )
                        }
                    }

                    // Nudge dialog overlay
                    nudgeApp?.let { app ->
                        NudgeDialog(
                            app                   = app,
                            totalWatchListUsageMs  = uiState.totalWatchListUsageMs,
                            dailyLimitMs           = uiState.preferences.dailyLimitMinutes * 60 * 1000L,
                            onOpenAnyway           = {
                                viewModel.dismissNudge()
                                launchApp(app.packageName)
                            },
                            onDismiss = { viewModel.dismissNudge() },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
        viewModel.recordTodayResult()
    }

    @Deprecated("Launchers stay on home screen")
    override fun onBackPressed() { /* no-op */ }

    private fun handleAppClick(app: AppInfo) {
        viewModel.onAppClicked(app)
        // If no nudge was set, launch directly
        if (viewModel.nudgeApp.value == null) {
            launchApp(app.packageName)
        }
    }

    private fun toggleFlag(app: AppInfo) {
        val prefs = viewModel.uiState.value.preferences
        when {
            app.packageName in prefs.watchList -> viewModel.removeFromWatchList(app.packageName)
            app.packageName in prefs.goalApps  -> viewModel.removeFromGoalApps(app.packageName)
            else -> viewModel.addToWatchList(app.packageName) // default: add to watch list
        }
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
