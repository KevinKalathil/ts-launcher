package com.example.stopbreathbelauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stopbreathbelauncher.ui.theme.SbbColors
import com.example.stopbreathbelauncher.ui.viewmodel.AppInfo
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable

@Composable
fun NudgeDialog(
    app: AppInfo,
    totalWatchListUsageMs: Long,
    dailyLimitMs: Long,
    onOpenAnyway: () -> Unit,
    onDismiss: () -> Unit,
) {
    var countdown by remember { mutableIntStateOf(3) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(app.packageName) {
        countdown = 3
        ready = false
        repeat(3) {
            delay(1000)
            countdown--
        }
        ready = true
    }

    val limitPct = if (dailyLimitMs > 0)
        ((totalWatchListUsageMs.toFloat() / dailyLimitMs) * 100).toInt().coerceIn(0, 100)
    else 0

    val nudgeMessage = when {
        limitPct >= 100 -> "You've hit your limit. Still opening this?"
        limitPct >= 75  -> "You've used ${app.openCount}x today. Still worth it?"
        else            -> "You've opened this ${app.openCount} times today."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SbbColors.Background.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(SbbColors.Surface)
                .border(2.dp, SbbColors.BorderStrong)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(SbbColors.SurfaceVariant)
                        .border(2.dp, SbbColors.BorderStrong),
                    contentAlignment = Alignment.Center,
                ) {
                    // App initial as placeholder (real icon needs AsyncImage or similar)
                    Text(
                        text  = app.label.first().toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = SbbColors.TextSecondary,
                    )
                }
                Column {
                    Text(
                        text  = app.label.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = SbbColors.TextPrimary,
                    )
                    Text(
                        text  = "WATCH LIST",
                        style = MaterialTheme.typography.labelSmall,
                        color = SbbColors.WatchRed,
                    )
                }
            }

            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(SbbColors.Border))

            Spacer(modifier = Modifier.height(16.dp))

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatBox("TODAY", formatUsageTime(app.usageTimeMs), Modifier.weight(1f), warn = app.usageTimeMs > dailyLimitMs / 3)
                StatBox("OPENS", "${app.openCount}×", Modifier.weight(1f))
                StatBox("LIMIT", formatUsageTime(dailyLimitMs), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Limit bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("DAILY LIMIT", style = MaterialTheme.typography.labelLarge, color = SbbColors.TextMuted)
                    Text("$limitPct% USED", style = MaterialTheme.typography.labelLarge, color = SbbColors.WatchRed)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(SbbColors.Border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(limitPct / 100f)
                            .fillMaxHeight()
                            .background(SbbColors.WatchRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Message
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp)
                    .border(width = 2.dp, color = SbbColors.Border)
                    .padding(10.dp),
            ) {
                Text(
                    text  = nudgeMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SbbColors.TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            val openLabel = if (ready) "[ OPEN ANYWAY ]" else "[ OPENING IN ${countdown}S... ]"
            SbbButton(
                label    = openLabel,
                enabled  = ready,
                onClick  = onOpenAnyway,
                color    = if (ready) SbbColors.TextPrimary else SbbColors.TextMuted,
                border   = if (ready) SbbColors.BorderStrong else SbbColors.Border,
            )

            Spacer(modifier = Modifier.height(10.dp))

            SbbButton(
                label   = "[ NOT NOW ]",
                enabled = true,
                onClick = onDismiss,
                color   = SbbColors.TextMuted,
                border  = SbbColors.Border,
            )
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    warn: Boolean = false,
) {
    Column(
        modifier = modifier
            .border(1.dp, SbbColors.Border)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SbbColors.TextMuted)
        Text(
            text  = value,
            style = MaterialTheme.typography.headlineMedium,
            color = if (warn) SbbColors.WatchRed else SbbColors.TextPrimary,
        )
    }
}

@Composable
fun SbbButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    color: Color,
    border: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SbbColors.SurfaceVariant)
            .border(2.dp, border)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
        )
    }
}