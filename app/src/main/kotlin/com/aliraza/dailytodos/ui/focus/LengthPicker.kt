package com.aliraza.dailytodos.ui.focus

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.ui.components.rememberTick
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * The length of a round still to start: its minutes, the allowed [range], the −/+ [step], the quick chips,
 * and its name. [onChange] is the one setter for the steppers, the chips and the dial.
 */
internal class LengthPicker(
    val minutes: Int,
    val range: IntRange,
    val step: Int,
    val presets: List<Int>,
    val label: String,
    val onChange: (Int) -> Unit,
)

private const val FULL_TURN = 360f
private const val HALF_TURN = 180f

/** One turn of the ring is the hour face [DialReading] draws a round on. */
private const val MINUTES_PER_TURN = 60
private const val DEGREES_PER_MINUTE = FULL_TURN / MINUTES_PER_TURN

/** Touches closer to the centre than this share of the radius are on the time, not the ring. */
private const val RING_INNER = 0.5f

/**
 * What the dial needs to set [length] by touch: the newest length for a drag already under way, the
 * haptic [tick], and the minutes as [spoken] by a screen reader.
 */
internal class DialInput(
    val length: LengthPicker,
    val latest: State<LengthPicker?>,
    val tick: () -> Unit,
    val spoken: String,
)

/** The dial's touch input for [length]; none while a round runs and nothing can be set. */
@Composable
internal fun rememberDialInput(length: LengthPicker?): DialInput? {
    val latest = rememberUpdatedState(length)
    val tick = rememberTick()
    if (length == null) return null
    return DialInput(length, latest, tick, stringResource(R.string.focus_minutes, length.minutes))
}

/**
 * Makes the dial set the length by touch: a tap on the ring jumps to that minute, a drag winds it, past 12
 * o'clock into the next hour too. It reads as an adjustable progress bar to screen readers. With no
 * [input] the dial is left as it is.
 */
internal fun Modifier.dialInput(input: DialInput?): Modifier {
    if (input == null) return this
    val length = input.length
    return semantics {
        contentDescription = length.label
        stateDescription = input.spoken
        progressBarRangeInfo = ProgressBarRangeInfo(
            length.minutes.toFloat(),
            length.range.first.toFloat()..length.range.last.toFloat(),
            steps = (length.range.last - length.range.first) / length.step - 1,
        )
        setProgress { target ->
            length.onChange(target.roundToInt().coerceIn(length.range))
            true
        }
    }.pointerInput(input.tick) { windMinutes(input.latest, input.tick) }
}

/** Follows each touch on the ring, setting the minutes it points at and ticking on each [LengthPicker.step]. */
private suspend fun PointerInputScope.windMinutes(latest: State<LengthPicker?>, tick: () -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val start = latest.value ?: return@awaitEachGesture
        val centre = Offset(size.width / 2f, size.height / 2f)
        val fromCentre = down.position - centre
        if (hypot(fromCentre.x, fromCentre.y) < size.width / 2f * RING_INNER) return@awaitEachGesture
        down.consume()
        var total = touchAngle(dialDegrees(fromCentre.x, fromCentre.y), start.minutes)
        var last = settle(start, start.minutes, total, tick)
        var change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
        while (change != null && change.pressed) {
            change.consume()
            val moved = change.position - centre
            total = unwrapAngle(total, dialDegrees(moved.x, moved.y))
            val length = latest.value ?: break
            last = settle(length, last, total, tick)
            change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
        }
    }
}

/** Sets the minutes at [total] degrees when they differ from [last], and returns them. */
private fun settle(length: LengthPicker, last: Int, total: Float, tick: () -> Unit): Int {
    val next = minutesAt(total, length.range)
    if (next != last) {
        if (passesMark(last, next, length.step)) tick()
        length.onChange(next)
    }
    return next
}

/** The angle of a point [dx], [dy] from the dial's centre, in degrees clockwise from 12 o'clock, 0 until 360. */
internal fun dialDegrees(dx: Float, dy: Float): Float {
    val degrees = Math.toDegrees(atan2(dx, -dy).toDouble()).toFloat()
    return if (degrees < 0f) degrees + FULL_TURN else degrees
}

/**
 * The minutes at [angleDegrees] wound from 12 o'clock, the inverse of the ring's hour face: 90° is 15
 * minutes and 450° is 75. Snapped to whole minutes and kept inside [range].
 */
internal fun minutesAt(angleDegrees: Float, range: IntRange): Int =
    (angleDegrees / DEGREES_PER_MINUTE).roundToInt().coerceIn(range)

/**
 * The wound angle after the finger moves to [absolute] degrees from a wound [total]: the shorter way round,
 * so passing 12 o'clock keeps counting instead of jumping between the ends.
 */
internal fun unwrapAngle(total: Float, absolute: Float): Float {
    val delta = ((absolute - total) % FULL_TURN + FULL_TURN + HALF_TURN) % FULL_TURN - HALF_TURN
    return total + delta
}

/** Where a touch at [absolute] degrees starts winding: on the hour of the [current] minutes, so 90 stays near 90. */
internal fun touchAngle(absolute: Float, current: Int): Float {
    val turn = (current - 1).coerceAtLeast(0) / MINUTES_PER_TURN
    return absolute + turn * FULL_TURN
}

/** Whether going [from] → [to] minutes reaches or passes a whole multiple of [step], the stepper's marks. */
internal fun passesMark(from: Int, to: Int, step: Int): Boolean = when {
    to > from -> Math.floorDiv(to, step) > Math.floorDiv(from, step)
    to < from -> Math.floorDiv(to + step - 1, step) < Math.floorDiv(from + step - 1, step)
    else -> false
}
