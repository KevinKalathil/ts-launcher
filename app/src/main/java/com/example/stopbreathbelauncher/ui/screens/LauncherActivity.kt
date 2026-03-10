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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.stopbreathbelauncher.ui.theme.StopBreathBeLauncherTheme
import com.example.stopbreathbelauncher.ui.viewmodel.LauncherViewModel

import com.example.stopbreathbelauncher.ui.screens.AllAppsScreen
import com.example.stopbreathbelauncher.ui.screens.TopTenScreen
import com.example.stopbreathbelauncher.ui.scroll.LineWheelScroll

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


                val items = listOf("Apple", "Banana", "Cherry", "Date", "Elderberry", "Fig", "Grape")
                var selectedIndex by remember { mutableStateOf(0) }

                LineWheelScroll(
                    items = items,
                    selectedIndex = selectedIndex,
                    onItemSelected = { selectedIndex = it }
                ) { item, isFocused ->
                    Text(
                        text = item,
                        fontSize = if (isFocused) 20.sp else 16.sp,
                        color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

//
//                HorizontalPager(
//                    state = pagerState,
//                    modifier = Modifier.fillMaxSize()
//                ) { page ->
//                    // Explicitly handle the content based on page index
//                    when (page) {
//                        0 -> TopTenScreen(
//                            apps = topTenApps,
//                            disabledApps = disabledApps,
//                            onAppClick = { app ->
//                                if (!disabledApps.contains(app.packageName)) {
//                                    launchApp(app.packageName)
//                                } else {
//                                    viewModel.onDisabledAppTapped(app)
//                                }
//                            },
//                        )
//                        1 -> AllAppsScreen(
//                            apps = apps,
//                            disabledApps = disabledApps,
//                            onAppClick = { app -> launchApp(app.packageName) }
//                        )
//                    }
//                }
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