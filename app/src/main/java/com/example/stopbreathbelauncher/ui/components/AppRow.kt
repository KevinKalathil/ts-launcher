package com.example.stopbreathbelauncher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.stopbreathbelauncher.ui.theme.SbbColors
import com.example.stopbreathbelauncher.ui.viewmodel.AppInfo
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

enum class AppFlag { NONE, WATCH }

private data class Quad(
    val label: String,
    val color: androidx.compose.ui.graphics.Color,
    val bg: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
)

@Composable
fun AppFlagPopup(
    flag: AppFlag,
    onDismiss: () -> Unit,
    onFlagChange: ((AppFlag) -> Unit)?,
    onUninstall: (() -> Unit)?,
) {
    Popup(
        alignment        = Alignment.TopEnd,
        onDismissRequest = onDismiss,
        properties       = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .background(SbbColors.Surface)
                .width(200.dp)
                .border(1.dp, SbbColors.BorderStrong)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(AppFlag.WATCH, AppFlag.NONE).forEach { f ->
                val isSelected = flag == f
                val (label, color, bg, border) = when (f) {
                    AppFlag.WATCH -> Quad("WATCH", SbbColors.WatchRed, SbbColors.WatchRedBg, SbbColors.WatchRedBorder)
                    AppFlag.NONE  -> Quad("NONE",  SbbColors.TextMuted, SbbColors.Surface, SbbColors.Border)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) bg else SbbColors.Background)
                        .border(1.dp, if (isSelected) border else SbbColors.Border)
                        .clickable {
                            onFlagChange?.invoke(f)
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) color else SbbColors.TextDim,
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SbbColors.Border))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SbbColors.Background)
                    .border(1.dp, SbbColors.Border)
                    .clickable {
                        onUninstall?.invoke()
                        onDismiss()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text  = "[×] Uninstall",
                    style = MaterialTheme.typography.labelSmall,
                    color = SbbColors.WatchRed,
                )
            }
        }
    }
}

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
    onFlagChange: ((AppFlag) -> Unit)? = null,
    onUninstall: (() -> Unit)? = null,
    hint: String? = null,
) {
    var showPopup by remember { mutableStateOf(false) }

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
        AppFlag.NONE -> SbbColors.NeutralBlue
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (onFlagChange != null || onUninstall != null) {
                        showPopup = true
                    } else {
                        onLongClick?.invoke()
                    }
                },
            )
            .padding(top = 6.dp, bottom = 6.dp, end = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                bitmap             = bitmap,
                contentDescription = app.label,
                modifier           = Modifier.size(36.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = app.label,
                    style    = MaterialTheme.typography.titleLarge,
                    color    = SbbColors.TextPrimary,
                    maxLines = 1,
                )
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

            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(1f, 0.5f)
                    clip = false
                }
            ) {
                when (flag) {
                    AppFlag.WATCH -> FlagBadge("WATCH", SbbColors.WatchRed, SbbColors.WatchRedBorder, SbbColors.WatchRedBg)
                    AppFlag.NONE  -> {}
                }
            }
        }

        if (showPopup) {
            AppFlagPopup(
                flag        = flag,
                onDismiss   = { showPopup = false },
                onFlagChange = onFlagChange,
                onUninstall  = onUninstall,
            )
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
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

fun formatUsageTime(usageMs: Long, totalMs: Long = 0L): String {
    if (usageMs == 0L) return "NO USAGE"
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