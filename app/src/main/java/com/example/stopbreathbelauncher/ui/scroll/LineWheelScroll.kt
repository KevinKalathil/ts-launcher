package com.example.stopbreathbelauncher.ui.scroll

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.stopbreathbelauncher.ui.theme.SbbColors
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private val ALPHABET = ('A'..'Z').toList()

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> LineWheelScroll(
    items: List<T>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    thumbColor: androidx.compose.ui.graphics.Color = SbbColors.PlantGreen,
    indexBar: Boolean = false,
    itemLabel: ((T) -> String)? = null,
    itemContent: @Composable (item: T, isFocused: Boolean, scale: Float) -> Unit
) {
    if (items.isEmpty()) return

    val prefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 40) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - 3).coerceAtLeast(0),
        prefetchStrategy = prefetchStrategy,
    )
    var focusedIndex by remember { mutableIntStateOf(selectedIndex) }

    // Derive focused index from scroll position
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val newFocused = listState.firstVisibleItemIndex + 3
        val clamped = newFocused.coerceIn(0, items.lastIndex)
        if (clamped != focusedIndex) {
            focusedIndex = clamped
            onItemSelected(clamped)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        LazyColumn(
            state    = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = if (indexBar) 56.dp else 16.dp),
        ) {
            itemsIndexed(items) { i, item ->
                androidx.compose.foundation.layout.Box(Modifier.alpha(1f)) {
                    itemContent(item, i == focusedIndex, 1f)
                }
            }
        }

        // ── Alphabet index bar ────────────────────────────────────────────────
        if (indexBar && itemLabel != null) {
            var columnHeightPx by remember { mutableIntStateOf(0) }
            var columnOffsetYPx by remember { mutableIntStateOf(0) }
            val scope = rememberCoroutineScope()

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
                                onItemSelected(target)
                                scope.launch {
                                    listState.scrollToItem((target - 3).coerceAtLeast(0))
                                }
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
                    modifier = Modifier
                        .onSizeChanged { columnHeightPx = it.height }
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