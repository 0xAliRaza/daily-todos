package com.aliraza.dailytasks.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aliraza.dailytasks.R
import com.aliraza.dailytasks.ui.theme.tokens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** What letting go of a swiped row asks for: right reschedules, left deletes. */
enum class SwipeAction { RESCHEDULE, DELETE }

/** Past this share of the row's width, letting go acts; short of it, the row springs back. */
internal const val SWIPE_THRESHOLD = 0.3f

/** The action a swipe of [offset] pixels on a row [width] wide has reached, if any. */
internal fun swipeAction(offset: Float, width: Float): SwipeAction? = when {
    width <= 0f -> null
    offset >= width * SWIPE_THRESHOLD -> SwipeAction.RESCHEDULE
    offset <= -width * SWIPE_THRESHOLD -> SwipeAction.DELETE
    else -> null
}

/** The icon behind a swiped row grows once letting go would act. */
private const val ARMED_SCALE = 1.2f

/**
 * A row that swipes: right to reschedule, left to delete. A tick of feedback marks the point where
 * letting go acts. Screen readers get both as actions on the row. [canReschedule] is off for done tasks.
 */
@Composable
fun SwipeRow(
    enabled: Boolean,
    canReschedule: Boolean,
    onReschedule: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }
    var width by remember { mutableIntStateOf(0) }
    var armed by remember { mutableStateOf<SwipeAction?>(null) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val drag = rememberDraggableState { delta ->
        val limit = width.toFloat()
        val next = (offset.value + delta).coerceIn(-limit, if (canReschedule) limit else 0f)
        scope.launch { offset.snapTo(next) }
        val reached = swipeAction(next, limit)
        if (reached != armed && reached != null) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        armed = reached
    }
    val reschedule = stringResource(R.string.action_reschedule)
    val delete = stringResource(R.string.action_delete)
    Box(
        modifier.fillMaxWidth().onSizeChanged { width = it.width }.clipToBounds().semantics {
            customActions = listOfNotNull(
                CustomAccessibilityAction(reschedule) { onReschedule().let { true } }.takeIf { canReschedule },
                CustomAccessibilityAction(delete) { onDelete().let { true } },
            )
        },
    ) {
        SwipeBackground(offset.value, armed, Modifier.matchParentSize())
        Box(
            Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.background)
                .draggable(
                    drag,
                    Orientation.Horizontal,
                    enabled = enabled,
                    onDragStopped = {
                        when (swipeAction(offset.value, width.toFloat())) {
                            SwipeAction.DELETE -> {
                                offset.animateTo(-width.toFloat())
                                onDelete()
                            }

                            SwipeAction.RESCHEDULE -> {
                                offset.animateTo(0f)
                                onReschedule()
                            }

                            null -> offset.animateTo(0f)
                        }
                        armed = null
                    },
                ),
        ) { content() }
    }
}

/** Purple with a calendar behind a right swipe, red with a bin behind a left one. */
@Composable
private fun SwipeBackground(offset: Float, armed: SwipeAction?, modifier: Modifier) {
    if (offset == 0f) return
    val right = offset > 0f
    val tokens = MaterialTheme.tokens
    Box(
        modifier.background(if (right) tokens.swipeReschedule else tokens.accent).padding(horizontal = 22.dp),
        contentAlignment = if (right) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Icon(
            if (right) AppIcons.Today else AppIcons.Delete,
            null,
            tint = tokens.onAccent,
            modifier = Modifier.size(22.dp).scale(if (armed != null) ARMED_SCALE else 1f),
        )
    }
}
