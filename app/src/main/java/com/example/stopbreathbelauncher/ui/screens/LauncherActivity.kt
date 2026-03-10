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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.stopbreathbelauncher.ui.screens.TopTenScreen
import com.example.stopbreathbelauncher.ui.screens.AllAppsScreen
import com.example.stopbreathbelauncher.ui.theme.StopBreathBeLauncherTheme
import com.example.stopbreathbelauncher.ui.viewmodel.LauncherViewModel

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StopBreathBeLauncherTheme {
                // Page 0 = T10, Page 1 = All Apps
                val pagerState = rememberPagerState(pageCount = { 2 })

                val apps by viewModel.allApps.collectAsState()
                val topTenApps by viewModel.topTenApps.collectAsState()
                val disabledApps by viewModel.disabledApps.collectAsState()

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    // Explicitly handle the content based on page index
                    when (page) {
                        0 -> TopTenScreen(
                            apps = topTenApps,
                            disabledApps = disabledApps,
                            onAppClick = { app ->
                                if (!disabledApps.contains(app.packageName)) {
                                    launchApp(app.packageName)
                                } else {
                                    viewModel.onDisabledAppTapped(app)
                                }
                            },
                            onDisableToggle = { app ->
                                viewModel.toggleDisabled(app)
                            }
                        )
                        1 -> AllAppsScreen(
                            apps = apps,
                            disabledApps = disabledApps,
                            onAppClick = { app -> launchApp(app.packageName) }
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Launchers should stay on home screen
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}