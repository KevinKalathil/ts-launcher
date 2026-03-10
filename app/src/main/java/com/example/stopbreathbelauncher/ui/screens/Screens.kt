package com.example.stopbreathbelauncher.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.stopbreathbelauncher.ui.viewmodel.AppInfo

/**
 * Top 10 screen
 * Sorted by usage time (descending), then alphabetically by label
 */
@Composable
fun TopTenScreen(
    apps: List<AppInfo>,
    disabledApps: Set<String>,
    onAppClick: (AppInfo) -> Unit
) {
    // Sort: highest usage first, then A-Z for ties
    val sortedApps = remember(apps) {
        apps.sortedWith(
            compareByDescending<AppInfo> { it.usageTimeMs }
                .thenBy { it.label.lowercase() }
        ).take(10) // Ensure it's limited to 10
    }

    AppListScreen(
        title = "Top 10 Apps",
        apps = sortedApps,
        disabledApps = disabledApps,
        onAppClick = onAppClick
    )
}

/**
 * All apps screen
 * Sorted alphabetically by label
 */
@Composable
fun AllAppsScreen(
    apps: List<AppInfo>,
    disabledApps: Set<String>,
    onAppClick: (AppInfo) -> Unit
) {
    // Sort: A-Z
    val sortedApps = remember(apps) {
        apps.sortedBy { it.label.lowercase() }
    }

    AppListScreen(
        title = "All Apps",
        apps = sortedApps,
        disabledApps = disabledApps,
        onAppClick = onAppClick
    )
}

/**
 * Shared screen layout
 */
@Composable
private fun AppListScreen(
    title: String,
    apps: List<AppInfo>,
    disabledApps: Set<String>,
    onAppClick: (AppInfo) -> Unit
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        item {
            SectionHeader(title)
        }

        items(
            items = apps,
            key = { it.packageName }
        ) { app ->

            AppRow(
                app = app,
                isDisabled = disabledApps.contains(app.packageName),
                onClick = { onAppClick(app) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Section header
 */
@Composable
private fun SectionHeader(title: String) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

/**
 * Single app row
 */
@Composable
private fun AppRow(
    app: AppInfo,
    isDisabled: Boolean,
    onClick: () -> Unit
) {

    val bitmap = remember(app.packageName) {
        app.icon.toBitmap(96, 96).asImageBitmap()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
            .alpha(if (isDisabled) 0.4f else 1f)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier
                .size(44.dp)
                .padding(end = 12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = formatUsageTime(app.usageTimeMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isDisabled) {
            Text(
                text = "hidden",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Usage time formatter
 */
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