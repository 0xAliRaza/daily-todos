package com.aliraza.dailytodos.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
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

/** A tab swipe in progress: the tab it heads for and how far along (as a seek fraction), else none. */
@Stable
internal class TabSeek {
    var target by mutableStateOf<String?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
        private set

    fun drag(to: String?, fraction: Float) {
        target = to
        progress = fraction
    }

    fun end() {
        target = null
        progress = 0f
    }
}

/**
 * Switches tabs on a sideways swipe across [route]'s page. Both pages move under the finger, header
 * and all; let go past [COMMIT] of the width, or with a flick, and the next tab carries on in, else
 * the page slides back. Anything inside that swipes sideways itself (a task row, the week strip) sees
 * the touch first and keeps it, so only empty areas switch tabs.
 */
internal fun Modifier.tabSwipe(seek: TabSeek, route: String, onTab: ((String) -> Unit)?): Modifier {
    if (onTab == null || route !in Route.tabs) return this
    return pointerInput(route, onTab) {
        val fling = FLING_SPEED.toPx()
        var dx = 0f
        val velocity = VelocityTracker()
        detectHorizontalDragGestures(
            onDragStart = {
                dx = 0f
                velocity.resetTracking()
            },
            onDragEnd = {
                val speed = velocity.calculateVelocity().x
                val commit = swipedTab(route, dx, threshold = size.width * COMMIT)
                    ?: swipedTab(route, dx, threshold = 0f).takeIf { abs(speed) >= fling && speed * dx > 0 }
                commit?.let(onTab)
                seek.end()
            },
            onDragCancel = { seek.end() },
        ) { change, amount ->
            change.consume()
            velocity.addPosition(change.uptimeMillis, change.position)
            dx += amount
            val target = swipedTab(route, dx, threshold = 0f)
            seek.drag(target, if (target == null) 0f else seekFraction(abs(dx) / size.width))
        }
    }
}

/** Past this share of the width, letting go switches tabs. */
private const val COMMIT = 0.3f

/** A flick this fast switches tabs however short it is. */
private val FLING_SPEED = 800.dp
