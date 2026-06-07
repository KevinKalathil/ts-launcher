package com.app.timespentlauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.timespentlauncher.ui.theme.SbbColors

@Composable
fun DailyLimitRow(
    minutes: Int,
    onMinutesChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text("30M", style = MaterialTheme.typography.labelLarge, color = SbbColors.TextDim)

        Slider(
            value = minutes.toFloat(),
            onValueChange = { newValue -> onMinutesChange(newValue) },
            onValueChangeFinished = {  },
            valueRange = 30f..240f,
            steps = 13,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = SbbColors.PlantGreen,
                activeTrackColor = SbbColors.PlantGreenDark,
                inactiveTrackColor = SbbColors.Border
            )
        )

        Text("4H", style = MaterialTheme.typography.labelLarge, color = SbbColors.TextDim)

        Text(
            "${minutes}M",
            style = MaterialTheme.typography.titleLarge,
            color = SbbColors.PlantGreen,
            modifier = Modifier.width(48.dp)
        )
    }
}