package com.example.stopbreathbelauncher.ui.screens

import android.app.AppOpsManager
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stopbreathbelauncher.ui.theme.StopBreathBeLauncherTheme

class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already fully set up, go straight to launcher
        if (isFullyConfigured()) {
            goToLauncher()
            return
        }

        setContent {
            StopBreathBeLauncherTheme {
                OnboardingScreen(
                    hasUsagePermission = hasUsagePermission(),
                    isDefaultLauncher = isDefaultLauncher(),
                    onGrantUsagePermission = {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    onSetDefaultLauncher = {
                        startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                    },
                    onFinish = { goToLauncher() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check on return from settings
        if (isFullyConfigured()) {
            goToLauncher()
        }
    }

    private fun isFullyConfigured(): Boolean {
        return hasUsagePermission() && isDefaultLauncher()
    }

    private fun hasUsagePermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(intent, 0)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun goToLauncher() {
        startActivity(Intent(this, LauncherActivity::class.java))
        finish()
    }
}

@Composable
fun OnboardingScreen(
    hasUsagePermission: Boolean,
    isDefaultLauncher: Boolean,
    onGrantUsagePermission: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Two quick steps to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Step 1 — Usage permission
        OnboardingStep(
            stepNumber = 1,
            title = "Allow Usage Access",
            description = "So we can show how much time you spend in each app.",
            isComplete = hasUsagePermission,
            buttonLabel = "Grant Permission",
            onAction = onGrantUsagePermission
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Step 2 — Default launcher
        OnboardingStep(
            stepNumber = 2,
            title = "Set as Default Launcher",
            description = "We'll take you to Settings. Select this app under 'Home app'.",
            isComplete = isDefaultLauncher,
            buttonLabel = "Open Settings",
            onAction = onSetDefaultLauncher
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (hasUsagePermission && isDefaultLauncher) {
            Button(onClick = onFinish) {
                Text("Let's Go")
            }
        }
    }
}

@Composable
fun OnboardingStep(
    stepNumber: Int,
    title: String,
    description: String,
    isComplete: Boolean,
    buttonLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Step indicator
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (isComplete)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    if (isComplete) "✓" else stepNumber.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isComplete)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isComplete) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onAction) {
                    Text(buttonLabel)
                }
            }
        }
    }
}