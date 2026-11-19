package com.aliraza.dailytodos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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

/** Where a page sits while a tab swipe drags it. Kept per page, so a page sliding away keeps its place. */
@Stable
internal class TabSwipeState(private val scope: CoroutineScope) {
    val offset = Animatable(0f)

    fun follow(to: Float) {
        scope.launch { offset.snapTo(to) }
    }

    fun settle() {
        scope.launch { offset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
    }
}

@Composable
internal fun rememberTabSwipeState(route: String): TabSwipeState {
    val scope = rememberCoroutineScope()
    return remember(route) { TabSwipeState(scope) }
}

/**
 * Switches tabs on a sideways swipe across [route]'s page. The page follows the finger (stiffly where
 * there is no tab that way); let go past [TAB_SWIPE] and the next tab slides in, short of it and the
 * page springs back. Anything inside that swipes sideways itself (a task row, the week strip) sees the
 * touch first and keeps it, so only empty areas switch tabs.
 */
internal fun Modifier.tabSwipe(state: TabSwipeState, route: String, onTab: ((String) -> Unit)?): Modifier {
    if (onTab == null || route !in Route.tabs) return this
    return graphicsLayer { translationX = state.offset.value }.pointerInput(route, onTab) {
        val threshold = TAB_SWIPE.toPx()
        var dx = 0f
        detectHorizontalDragGestures(
            onDragStart = { dx = 0f },
            onDragEnd = { swipedTab(route, dx, threshold)?.let(onTab) ?: state.settle() },
            onDragCancel = { state.settle() },
        ) { change, amount ->
            change.consume()
            dx += amount
            val open = swipedTab(route, dx, threshold = 0f) != null
            state.follow(dx * if (open) FOLLOW else EDGE_FOLLOW)
        }
    }
}

/** How far a swipe must travel to switch tabs. */
private val TAB_SWIPE = 56.dp

/** The page moves with the finger, a little behind it. */
private const val FOLLOW = 0.6f

/** At the first or last tab the page only gives a little, to show there is nothing that way. */
private const val EDGE_FOLLOW = 0.15f
