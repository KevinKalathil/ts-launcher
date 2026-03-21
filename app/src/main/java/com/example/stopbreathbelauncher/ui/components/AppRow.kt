package com.example.stopbreathbelauncher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import com.example.stopbreathbelauncher.ui.theme.SbbColors
import com.example.stopbreathbelauncher.ui.viewmodel.AppInfo

enum class AppFlag { NONE, WATCH, GOAL }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: AppInfo,
    flag: AppFlag = AppFlag.NONE,
    isFocused: Boolean = true,
    scale: Float = 1f,
    showBar: Boolean = false,
    totalUsageMs: Long = 0L,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    hint: String? = null,
) {
    val alpha = if (isFocused) 1f else if (alpha(isFocused) > 0.4f) 0.55f else 0.25f

    val bitmap = remember(app.packageName) {
        app.icon.toBitmap(96, 96).asImageBitmap()
    }

    val proportion = if (totalUsageMs > 0L && app.usageTimeMs > 0L)
        (app.usageTimeMs.toFloat() / totalUsageMs).coerceIn(0f, 1f)
    else 0f

    val barColor = when (flag) {
        AppFlag.WATCH -> when {
            proportion >= 0.9f -> SbbColors.WatchRed
            proportion >= 0.6f -> SbbColors.WatchOrange
            else               -> SbbColors.WatchYellow
        }
        AppFlag.GOAL -> when {
            proportion >= 0.9f -> SbbColors.GoalGreen
            proportion >= 0.6f -> SbbColors.GoalGreenMid
            else               -> SbbColors.GoalGreenLight
        }
        AppFlag.NONE -> SbbColors.NeutralBlue
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .alpha(alpha)
            .padding(vertical = 6.dp),
    ) {
        // Top row: icon + name + flag badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                bitmap             = bitmap,
                contentDescription = app.label,
                modifier           = Modifier.size(26.dp),
            )

            Text(
                text     = app.label,
                style    = MaterialTheme.typography.titleLarge,
                color    = SbbColors.TextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )

            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(1f, 0.5f)
                    clip = false
                }
            ) {
                when (flag) {
                    AppFlag.WATCH -> FlagBadge("WATCH", SbbColors.WatchRedLight, SbbColors.WatchRedBorder, SbbColors.WatchRedBg)
                    AppFlag.GOAL  -> FlagBadge("GOAL",  SbbColors.GoalGreenLight, SbbColors.GoalGreenBorder, SbbColors.GoalGreenBg)
                    AppFlag.NONE  -> {}
                }
            }
        }

        // Bottom row: time + bar (indented to align with text)
        Column(modifier = Modifier.padding(start = 36.dp)) {
            Text(
                text  = formatUsageTime(app.usageTimeMs, totalUsageMs),
                style = MaterialTheme.typography.labelLarge,
                color = SbbColors.TextSecondary,
            )

            if (showBar) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(SbbColors.Border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(proportion)
                            .fillMaxHeight()
                            .background(barColor)
                    )
                }
            }

            if (hint != null) {
                Text(
                    text  = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = SbbColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
fun FlagBadge(
    label: String,
    textColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
    bgColor: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(bgColor)
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

private fun alpha(isFocused: Boolean) = if (isFocused) 1f else 0.3f

fun formatUsageTime(usageMs: Long, totalMs: Long = 0L): String {
    if (usageMs == 0L) return "NO USAGE TODAY"
    val minutes = usageMs / 1000 / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    val timeStr = if (hours > 0) "${hours}H ${remainingMinutes}M" else "${remainingMinutes}M"
    return if (totalMs > 0L) {
        val pct = ((usageMs.toFloat() / totalMs) * 100).toInt()
        "$timeStr · $pct%"
    } else {
        timeStr
    }
}