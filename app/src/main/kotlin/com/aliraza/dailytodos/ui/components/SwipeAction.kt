package com.aliraza.dailytodos.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.ui.theme.tokens
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** What letting go of a swiped row asks for: right opens its focus timer, left deletes. */
enum class SwipeAction { FOCUS, DELETE }

/** Past this share of the row's width, letting go acts; short of it, the row springs back. */
internal const val SWIPE_THRESHOLD = 0.3f

/** The action a swipe of [offset] pixels on a row [width] wide has reached, if any. */
internal fun swipeAction(offset: Float, width: Float): SwipeAction? = when {
    width <= 0f -> null
    offset >= width * SWIPE_THRESHOLD -> SwipeAction.FOCUS
    offset <= -width * SWIPE_THRESHOLD -> SwipeAction.DELETE
    else -> null
}

/**
 * What letting go does: the action the row was dragged to, or the one a quick flick throws it towards
 * ([velocity] at least [fling] pixels a second, the same way the row moved). Else nothing.
 */
internal fun releaseAction(offset: Float, width: Float, velocity: Float, fling: Float): SwipeAction? =
    swipeAction(offset, width) ?: when {
        width <= 0f -> null
        velocity >= fling && offset > 0f -> SwipeAction.FOCUS
        velocity <= -fling && offset < 0f -> SwipeAction.DELETE
        else -> null
    }

/** A flick this fast acts even short of the threshold. */
private val FLING_SPEED = 800.dp

/** The icon behind a swiped row grows once letting go would act. */
private const val ARMED_SCALE = 1.2f

/**
 * A row that swipes: right to focus on it, left to delete. A tick of feedback marks the point where
 * letting go acts. Screen readers get both as actions on the row. [canFocus] is off for done tasks.
 */
@Composable
fun SwipeRow(
    enabled: Boolean,
    canFocus: Boolean,
    onFocus: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Written straight from each drag step, so no queued update can land after letting go and leave
    // the row half open.
    var offset by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(0) }
    var armed by remember { mutableStateOf<SwipeAction?>(null) }
    var settling by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val tick = rememberTick()
    val fling = with(LocalDensity.current) { FLING_SPEED.toPx() }
    val drag = rememberDraggableState { delta ->
        val limit = width.toFloat()
        offset = (offset + delta).coerceIn(-limit, if (canFocus) limit else 0f)
        val reached = swipeAction(offset, limit)
        if (reached != armed && reached != null) tick()
        armed = reached
    }
    val slideTo: suspend (Float) -> Unit = { target -> animate(offset, target) { value, _ -> offset = value } }
    val release = { velocity: Float ->
        val action = releaseAction(offset, width.toFloat(), velocity, fling)
        armed = null
        settling = scope.launch {
            when (action) {
                SwipeAction.DELETE -> {
                    slideTo(-width.toFloat())
                    onDelete()
                }

                // The row springs back; nothing about the task changes.
                SwipeAction.FOCUS -> {
                    onFocus()
                    slideTo(0f)
                }

                null -> slideTo(0f)
            }
        }
    }
    val focus = stringResource(R.string.action_focus)
    val delete = stringResource(R.string.action_delete)
    Box(
        modifier.fillMaxWidth().onSizeChanged { width = it.width }.clipToBounds().semantics {
            customActions = listOfNotNull(
                CustomAccessibilityAction(focus) { onFocus().let { true } }.takeIf { canFocus },
                CustomAccessibilityAction(delete) { onDelete().let { true } },
            )
        },
    ) {
        SwipeBackground(offset, armed, Modifier.matchParentSize())
        Box(
            Modifier.offset { IntOffset(offset.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.background)
                .draggable(
                    drag,
                    Orientation.Horizontal,
                    enabled = enabled,
                    onDragStarted = { settling?.cancel() },
                    onDragStopped = { velocity -> release(velocity) },
                ),
        ) { content() }
    }
}

/** The accent with a timer behind a right swipe, red with a bin behind a left one. */
@Composable
private fun SwipeBackground(offset: Float, armed: SwipeAction?, modifier: Modifier) {
    if (offset == 0f) return
    val right = offset > 0f
    val tokens = MaterialTheme.tokens
    Box(
        modifier.background(if (right) tokens.accent else tokens.danger).padding(horizontal = 22.dp),
        contentAlignment = if (right) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Icon(
            if (right) AppIcons.Timer else AppIcons.Delete,
            null,
            tint = if (right) tokens.onAccent else tokens.onDanger,
            modifier = Modifier.size(22.dp).scale(if (armed != null) ARMED_SCALE else 1f),
        )
    }
}
