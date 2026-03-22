package com.example.stopbreathbelauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.stopbreathbelauncher.ui.theme.SbbColors
import com.example.stopbreathbelauncher.ui.viewmodel.AppInfo

@Composable
fun AppIconSlot(
    app: AppInfo?,
    size: Dp = 44.dp,
    bgColor: androidx.compose.ui.graphics.Color = SbbColors.SurfaceVariant,
    badge: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val bitmap = app?.let {
        remember(it.packageName) { it.icon.toBitmap(128, 128).asImageBitmap() }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap             = bitmap,
                    contentDescription = app.label,
                )
            } else {
                Text("?", style = MaterialTheme.typography.bodyLarge, color = SbbColors.TextDim)
            }
        }
        badge?.invoke()
    }
}