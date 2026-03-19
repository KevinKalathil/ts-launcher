package com.example.stopbreathbelauncher.ui.screens

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.stopbreathbelauncher.ui.theme.StopBreathBeLauncherTheme
import com.example.stopbreathbelauncher.ui.viewmodel.LauncherViewModel

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.hasUsagePermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }


        setContent {
            StopBreathBeLauncherTheme {

                val pagerState = rememberPagerState(pageCount = { 2 })
                val apps by viewModel.allApps.collectAsState()
                val disabledApps by viewModel.disabledApps.collectAsState()
                val topTenApps = apps.take(10)

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

    @Deprecated("Launchers should stay on the home screen")
    override fun onBackPressed() {
        // Do nothing — prevents exiting launcher
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}