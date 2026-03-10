package com.example.stopbreathbelauncher.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed

@OptIn(ExperimentalFoundationApi::class) // <--- Add this line here
@Composable
fun AppsScreen(
    apps: List<AppInfo>,
    disabledApps: Set<String>,
    onAppClick: (AppInfo) -> Unit,
    onDisableToggle: (AppInfo) -> Unit
) {

    val topApps = remember(apps) { apps.take(10) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        stickyHeader {
            SectionHeader("Top 10 Most Used")
        }

        itemsIndexed(topApps, key = { _, it -> "top_${it.packageName}" }) { index, app ->
            AppRow(
                rank = index + 1,
                app = app,
                isDisabled = disabledApps.contains(app.packageName),
                onClick = { onAppClick(app) },
                onLongClick = { onDisableToggle(app) }
            )
        }

        stickyHeader {
            SectionHeader("All Apps")
        }

        items(apps, key = { it.packageName }) { app ->
            AppRow(
                rank = null,
                app = app,
                isDisabled = disabledApps.contains(app.packageName),
                onClick = { onAppClick(app) },
                onLongClick = {}
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    rank: Int?,
    app: AppInfo,
    isDisabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .alpha(if (isDisabled) 0.4f else 1f)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (rank != null) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(28.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(28.dp))
        }

        val bitmap = remember(app.packageName) {
            app.icon.toBitmap(96, 96).asImageBitmap()
        }

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