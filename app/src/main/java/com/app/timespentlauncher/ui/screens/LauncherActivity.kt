package com.app.timespentlauncher.ui.screens

import android.content.Intent
import android.net.Uri
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
import com.app.timespentlauncher.ui.components.AppFlag
import com.app.timespentlauncher.ui.components.NudgeDialog
import com.app.timespentlauncher.ui.theme.SbbScaffold
import com.app.timespentlauncher.ui.theme.StopBreathBeLauncherTheme
import com.app.timespentlauncher.ui.viewmodel.AppInfo
import com.app.timespentlauncher.ui.viewmodel.LauncherViewModel

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
                                state = uiState,
                                onAppClick = { app -> handleAppClick(app) },
                                onAppLongClick = { app -> toggleFlag(app) },
                                onSetFlag = { app, flag -> setFlag(app, flag) },
                                onUninstall = { app -> uninstallApp(app) },
                                onSettingsClick = { openSettings() },
                            )
                            1 -> AllAppsScreen(
                                state = uiState,
                                onAppClick = { app -> handleAppClick(app) },
                                onAppLongClick = { app -> toggleFlag(app) },
                                onSetFlag = { app, flag -> setFlag(app, flag) },
                                onUninstall = { app -> uninstallApp(app) },
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
            app.packageName in prefs.watchList -> {
                // WATCH → None
                viewModel.removeFromWatchList(app.packageName)
            }
            else -> {
                // NONE → WATCH
                viewModel.addToWatchList(app.packageName)
            }
        }
    }

    private fun setFlag(app: AppInfo, flag: AppFlag) {
        val prefs = viewModel.uiState.value.preferences
        // Clear from both lists first
        if (app.packageName in prefs.watchList) viewModel.removeFromWatchList(app.packageName)
        // Then add to the right one
        when (flag) {
            AppFlag.WATCH -> viewModel.addToWatchList(app.packageName)
            AppFlag.NONE  -> { /* already cleared */ }
        }
    }

    @Suppress("DEPRECATION")
    private fun uninstallApp(app: AppInfo) {
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
            data = Uri.parse("package:${app.packageName}")
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
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
