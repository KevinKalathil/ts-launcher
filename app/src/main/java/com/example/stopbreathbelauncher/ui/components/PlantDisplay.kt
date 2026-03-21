package com.example.stopbreathbelauncher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stopbreathbelauncher.data.PlantState
import com.example.stopbreathbelauncher.ui.theme.SbbColors

@Composable
fun PlantDisplay(
    state: PlantState,
    modifier: Modifier = Modifier,
) {
    val (canopy, pot, color) = plantArt(state)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text       = canopy,
            fontFamily = FontFamily.Monospace,
            fontSize   = 10.sp,
            lineHeight = 12.sp,
            color      = color,
        )
        Text(
            text       = pot,
            fontFamily = FontFamily.Monospace,
            fontSize   = 10.sp,
            lineHeight = 12.sp,
            color      = SbbColors.PlantBrown,
        )
    }
}

private data class PlantArt(val canopy: String, val pot: String, val color: Color)

private fun plantArt(state: PlantState): PlantArt {
    val pot = "  [___]\n  |   |\n  |___|"
    return when (state) {
        PlantState.THRIVING -> PlantArt(
            canopy = " \\@@@@@/\n @@@@@@@\n@@@@@@@@@\n @@@@@@@\n  @@@@@\n    |\n   \\|/",
            pot    = pot,
            color  = SbbColors.PlantGreenLight,
        )
        PlantState.HEALTHY -> PlantArt(
            canopy = "  ,@@@,\n @@@@@@@\n @@@@@@@\n  @@@@@\n    |\n   \\|/",
            pot    = pot,
            color  = SbbColors.PlantGreen,
        )
        PlantState.STRESSED -> PlantArt(
            canopy = "  ,@.@,\n @@ @@@@\n @@@@ @@\n  @.@@@\n    |\n    |",
            pot    = pot,
            color  = SbbColors.StressedYellow,
        )
        PlantState.WILTING -> PlantArt(
            canopy = "   , . ,\n  @ . . @\n  .@ . @.\n   . . .\n    |\n    |",
            pot    = pot,
            color  = SbbColors.WiltingBrown,
        )
        PlantState.DYING -> PlantArt(
            canopy = " . . . .\n. . . . .\n . . . .\n   . . .\n    |\n    |",
            pot    = pot,
            color  = SbbColors.DyingDark,
        )
        PlantState.DEAD -> PlantArt(
            canopy = "   . . .\n  . . . .\n   . . .\n    . .\n    |\n    |",
            pot    = pot,
            color  = SbbColors.DeadDarkest,
        )
    }
}
