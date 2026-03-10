package com.example.stopbreathbelauncher.ui.scroll

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun <T> LineWheelScroll(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    itemContent: @Composable (T, Boolean) -> Unit
) {

    val visibleRange = 2

    var focusedIndex by remember { mutableStateOf(selectedIndex) }
    var scrollPosition by remember { mutableFloatStateOf(selectedIndex.toFloat()) }

    val thumbRatio =
        if (items.size > 1) scrollPosition / (items.size - 1)
        else 0f

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {

            val start = (focusedIndex - visibleRange).coerceAtLeast(0)
            val end = (focusedIndex + visibleRange).coerceAtMost(items.lastIndex)

            for (i in start..end) {

                val distance = (i - focusedIndex).absoluteValue

                val targetAlpha = when (distance) {
                    0 -> 1f
                    1 -> 0.6f
                    2 -> 0.3f
                    else -> 0f
                }

                val alpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )

                Box(Modifier.alpha(alpha)) {
                    itemContent(items[i], i == focusedIndex)
                }
            }
        }

        ScrollLine(
            itemCount = items.size,
            thumbRatio = thumbRatio,
            currentScrollPosition = scrollPosition,
            onScrollChanged = { newPos ->

                scrollPosition = newPos

                val newIndex = newPos.roundToInt().coerceIn(0, items.lastIndex)

                if (newIndex != focusedIndex) {
                    focusedIndex = newIndex
                    onItemSelected(newIndex)
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )

    }
}

@Composable
fun ScrollLine(
    itemCount: Int,
    thumbRatio: Float,
    currentScrollPosition: Float,
    onScrollChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {

    var lineHeightPx by remember { mutableStateOf(1f) }
    var dragPosition by remember { mutableFloatStateOf(currentScrollPosition) }

    // keep drag position synced if parent updates
    LaunchedEffect(currentScrollPosition) {
        dragPosition = currentScrollPosition
    }

    val thumbY = thumbRatio * lineHeightPx

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
    ) {

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.5f)
                .fillMaxWidth()
                .onSizeChanged {
                    lineHeightPx = it.height.toFloat()
                }

                // TAP
                .pointerInput(itemCount) {
                    detectTapGestures { offset ->

                        val ratio = (offset.y / lineHeightPx)
                            .coerceIn(0f, 1f)

                        dragPosition = ratio * (itemCount - 1)

                        onScrollChanged(dragPosition)
                    }
                }

                // DRAG
                .pointerInput(itemCount) {
                    detectDragGestures { change, dragAmount ->

                        change.consume()

                        val delta =
                            dragAmount.y / lineHeightPx * (itemCount - 1)

                        dragPosition = (dragPosition + delta)
                            .coerceIn(0f, (itemCount - 1).toFloat())

                        onScrollChanged(dragPosition)
                    }
                }
        ) {

            // track
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .background(MaterialTheme.colorScheme.primary)
            )

            // thumb
            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = "Position",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = thumbY
                    }
                    .size(16.dp)
            )
        }
    }
}