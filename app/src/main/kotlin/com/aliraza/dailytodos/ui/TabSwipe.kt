package com.aliraza.dailytodos.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * The tab a sideways swipe of [dx] pixels lands on from [current]: left goes to the next tab, right to
 * the one before. Null when the swipe is shorter than [threshold], at either end, or [current] is not a tab.
 */
internal fun swipedTab(current: String, dx: Float, threshold: Float): String? {
    val index = Route.tabs.indexOf(current)
    if (index < 0 || abs(dx) < threshold) return null
    return Route.tabs.getOrNull(if (dx < 0) index + 1 else index - 1)
}

/**
 * Switches tabs on a sideways swipe across [route]'s page. Anything inside that swipes sideways itself
 * (a task row, the week strip) sees the touch first and keeps it, so only empty areas switch tabs.
 */
internal fun Modifier.tabSwipe(route: String, onTab: ((String) -> Unit)?): Modifier =
    if (onTab == null || route !in Route.tabs) {
        this
    } else {
        pointerInput(route, onTab) {
            val threshold = TAB_SWIPE.toPx()
            var dx = 0f
            detectHorizontalDragGestures(
                onDragStart = { dx = 0f },
                onDragEnd = { swipedTab(route, dx, threshold)?.let(onTab) },
            ) { change, amount ->
                change.consume()
                dx += amount
            }
        }
    }

/** How far a swipe must travel to switch tabs. */
private val TAB_SWIPE = 56.dp
