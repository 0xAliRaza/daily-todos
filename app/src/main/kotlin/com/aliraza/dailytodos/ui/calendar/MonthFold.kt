package com.aliraza.dailytodos.ui.calendar

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/**
 * How far the month grid is folded away, in pixels. 0 shows every week; the most it folds leaves only
 * the picked day's week. Scrolling the day's tasks up folds it first; pulling down at the top unfolds it.
 */
@Stable
internal class MonthFold {
    var folded by mutableFloatStateOf(0f)
        private set

    /**
     * Takes what it can of a scroll of [delta] pixels (negative is up) and returns the part it used, so
     * the list only scrolls by the rest. [max] is how far the grid can fold.
     */
    fun take(delta: Float, max: Float): Float {
        val before = folded.coerceAtMost(max)
        folded = (before - delta).coerceIn(0f, max)
        return before - folded
    }

    /** Where a let-go fold settles: fully folded past half way, else open. */
    fun settleTarget(max: Float): Float = if (folded > max / 2) max else 0f

    /** Eases a let-go fold to its nearer end. */
    suspend fun settle(max: Float) = glideTo(settleTarget(max))

    /** Opens the whole month again, as when Month is tapped while folded. */
    suspend fun unfold() = glideTo(0f)

    private suspend fun glideTo(target: Float) = animate(folded, target) { value, _ -> folded = value }
}
