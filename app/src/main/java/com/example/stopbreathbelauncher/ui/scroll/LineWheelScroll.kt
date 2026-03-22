package com.example.stopbreathbelauncher.ui.scroll

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import com.example.stopbreathbelauncher.ui.theme.SbbColors
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val ALPHABET = ('A'..'Z').toList()

@Composable
fun <T> LineWheelScroll(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    thumbColor: androidx.compose.ui.graphics.Color = SbbColors.PlantGreen,
    indexBar: Boolean = false,
    itemLabel: ((T) -> String)? = null,  // needed for letter lookup
    itemContent: @Composable (item: T, isFocused: Boolean, scale: Float) -> Unit
) {
    if (items.isEmpty()) return

    val visibleRange = 6
    var focusedIndex by remember { mutableIntStateOf(selectedIndex) }
    var scrollPosition by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    var barHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedIndex) {
        focusedIndex = selectedIndex
        scrollPosition = selectedIndex.toFloat()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart,
    ) {
        // ── Main scroll area ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(items.size) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var totalDragY = 0f
                        var totalDragX = 0f
                        var decided = false
                        var isVertical = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val drag = event.changes.firstOrNull() ?: break
                            if (!drag.pressed) break

                            val dy = drag.position.y - drag.previousPosition.y
                            val dx = drag.position.x - drag.previousPosition.x
                            totalDragY += dy
                            totalDragX += dx

                            if (!decided && (totalDragY.absoluteValue > 10f || totalDragX.absoluteValue > 10f)) {
                                isVertical = totalDragY.absoluteValue > totalDragX.absoluteValue
                                decided = true
                            }

                            if (decided && isVertical) {
                                drag.consume()
                                val sensitivity = 400f
                                val delta = -dy / sensitivity
                                scrollPosition = (scrollPosition + delta).coerceIn(0f, (items.size - 1).toFloat())
                                val newIndex = scrollPosition.roundToInt().coerceIn(0, items.lastIndex)
                                if (newIndex != focusedIndex) {
                                    focusedIndex = newIndex
                                    onItemSelected(newIndex)
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = if (indexBar) 28.dp else 16.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                val start = (focusedIndex - visibleRange).coerceAtLeast(0)
                val end   = (focusedIndex + visibleRange).coerceAtMost(items.lastIndex)

                for (i in start..end) {
                    val distance = (i - scrollPosition).absoluteValue

                    val targetAlpha = (1f - (distance / (visibleRange + 1))).coerceIn(0.5f, 1f)
                    val targetScale = (1f + ((1f - distance) * 0.15f)).coerceIn(0.85f, 1.15f)

                    val alpha by animateFloatAsState(
                        targetValue   = targetAlpha,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness    = Spring.StiffnessHigh,
                        ),
                        label = "alpha_$i",
                    )
                    val scale by animateFloatAsState(
                        targetValue   = targetScale,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness    = Spring.StiffnessHigh,
                        ),
                        label = "scale_$i",
                    )

                    Box(Modifier.alpha(alpha)) {
                        itemContent(items[i], i == focusedIndex, scale)
                    }
                }
            }
        }

        // ── Alphabet index bar ────────────────────────────────────────────────
        if (indexBar && itemLabel != null) {
            var columnHeightPx by remember { mutableIntStateOf(0) }
            var columnOffsetYPx by remember { mutableIntStateOf(0) }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(48.dp)
                    .pointerInput(items) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)

                            fun jumpToLetter(y: Float) {
                                if (columnHeightPx == 0) return
                                val relative = (y - columnOffsetYPx).coerceIn(0f, columnHeightPx.toFloat())
                                val fraction = relative / columnHeightPx
                                val letter = ALPHABET[(fraction * 25).roundToInt()]
                                val target = items.indexOfFirst {
                                    itemLabel(it).uppercase().firstOrNull() ?: ' ' >= letter
                                }.takeIf { it >= 0 } ?: items.lastIndex
                                focusedIndex = target
                                scrollPosition = target.toFloat()
                                onItemSelected(target)
                            }

                            val down = currentEvent.changes.firstOrNull() ?: return@awaitEachGesture
                            jumpToLetter(down.position.y)
                            down.consume()

                            while (true) {
                                val event = awaitPointerEvent()
                                val drag = event.changes.firstOrNull() ?: break
                                if (!drag.pressed) break
                                drag.consume()
                                jumpToLetter(drag.position.y)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.onSizeChanged { columnHeightPx = it.height }
                        .onGloballyPositioned { columnOffsetYPx = it.positionInParent().y.toInt() },
                ) {
                    ALPHABET.forEach { letter ->
                        Text(
                            text       = letter.toString(),
                            fontSize   = 16.sp,
                            color      = SbbColors.TextDim,
                            textAlign  = TextAlign.Center,
                            lineHeight = 8.sp,
                        )
                    }
                }
            }
        }
    }
}