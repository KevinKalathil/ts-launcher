package com.example.stopbreathbelauncher.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.stopbreathbelauncher.ui.theme.StopBreathBeLauncherTheme
import com.example.stopbreathbelauncher.ui.viewmodel.LauncherViewModel

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.hasUsagePermission()) {
            startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        setContent {
            StopBreathBeLauncherTheme {

                val apps by viewModel.allApps.collectAsState()
                val disabledApps by viewModel.disabledApps.collectAsState()

                AppsScreen(
                    apps = apps,
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