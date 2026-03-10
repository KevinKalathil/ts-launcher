package com.example.stopbreathbelauncher.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.stopbreathbelauncher.ui.viewmodel.AppInfo
import androidx.compose.foundation.lazy.items

// ─── Top Ten Screen ───────────────────────────────────────────────────────────

@Composable
fun TopTenScreen(
    apps: List<AppInfo>,
    disabledApps: Set<String>,
    onAppClick: (AppInfo) -> Unit,
    onDisableToggle: (AppInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Your Top 10",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 2-column grid of up to 10 apps
        val rows = apps.chunked(2)
        rows.forEach { rowApps ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowApps.forEach { app ->
                    AppGridItem(
                        app = app,
                        isDisabled = disabledApps.contains(app.packageName),
                        onClick = { onAppClick(app) },
                        onLongClick = { onDisableToggle(app) }
                    )
                }
                // Fill empty slot if row has only 1 app
                if (rowApps.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Long press any app to disable it from this screen",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: AppInfo,
    isDisabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .alpha(if (isDisabled) 0.35f else 1f) // visually dim disabled apps
            .padding(8.dp)
    ) {
        // App icon
        val bitmap = remember(app.packageName) {
            app.icon.toBitmap(96, 96).asImageBitmap()
        }
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )

        if (isDisabled) {
            Text(
                text = "disabled",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ─── All Apps Screen ──────────────────────────────────────────────────────────

@Composable
fun AllAppsScreen(
    apps: List<AppInfo>,
    disabledApps: Set<String>,
    onAppClick: (AppInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "All Apps",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(apps, key = { it.packageName }) { app ->
                AppListItem(
                    app = app,
                    isDisabled = disabledApps.contains(app.packageName),
                    onClick = { onAppClick(app) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class) // <--- Add this line
@Composable
fun AppListItem(
    app: AppInfo,
    isDisabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)  // full access from all apps view
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = remember(app.packageName) {
            app.icon.toBitmap(80, 80).asImageBitmap()
        }
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyMedium)
            Text(
                formatUsageTime(app.usageTimeMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isDisabled) {
            Text(
                "hidden from T10",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

fun formatUsageTime(ms: Long): String {
    if (ms == 0L) return "No usage today"
    val minutes = ms / 1000 / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours > 0 -> "${hours}h ${remainingMinutes}m today"
        else -> "${remainingMinutes}m today"
    }
}