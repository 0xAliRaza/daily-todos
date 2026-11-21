package com.aliraza.dailytodos.ui.calendar

import com.aliraza.dailytodos.core.model.Task

/**
 * One day's tasks as the hour grid shows them. A task with a time sits at that time; one with only a
 * date goes in the "No time" row above the grid, since tasks have no length and it has no place on it.
 */
internal data class DaySplit(val timed: List<Task>, val untimed: List<Task>)

/** Splits [tasks] into timed and date-only ones, each keeping its order. */
internal fun splitByTime(tasks: List<Task>): DaySplit {
    val (timed, untimed) = tasks.partition { it.dueTime != null }
    return DaySplit(timed, untimed)
}

/** The lines of a label that fit, and whether the whole label did. */
internal data class Wrapped(val lines: List<String>, val complete: Boolean)

/**
 * [text] wrapped into at most [maxLines] lines no wider than [maxWidth] as [measure] measures them,
 * never breaking a word. Words that don't fit are left out, so what shows is always whole words.
 */
internal fun wrapWhole(text: String, maxWidth: Float, maxLines: Int, measure: (String) -> Float): Wrapped {
    val words = text.split(' ').filter { it.isNotEmpty() }
    if (maxLines < 1) return Wrapped(emptyList(), complete = words.isEmpty())
    val lines = mutableListOf<String>()
    var line = ""
    for (word in words) {
        val joined = if (line.isEmpty()) word else "$line $word"
        if (measure(joined) <= maxWidth) {
            line = joined
            continue
        }
        // The word has to start a line of its own: stop if there is none left or it is too wide for one.
        if (line.isEmpty() || lines.size + 1 >= maxLines || measure(word) > maxWidth) {
            return Wrapped(lines + listOfNotNull(line.ifEmpty { null }), complete = false)
        }
        lines += line
        line = word
    }
    if (line.isNotEmpty()) lines += line
    return Wrapped(lines, complete = true)
}

/** How a block on the hour grid shows its task. */
internal enum class BlockFit {
    /** The whole label in whole words. */
    WHOLE,

    /** A wide block whose label runs out of room: cut with an ellipsis. */
    CLIPPED,

    /** Too short or narrow for whole words: a slim pill with the colour edge and no text. */
    COMPACT,
}

/**
 * Picks a block's look from how its label wrapped in [lines] available lines. A [roomy] block (Day and
 * 3 days) cuts a long label short; a narrow one (Week) turns into a compact pill instead of showing a
 * word broken in two.
 */
internal fun blockFit(wrapped: Wrapped, lines: Int, roomy: Boolean): BlockFit = when {
    lines < 1 -> BlockFit.COMPACT
    wrapped.complete && wrapped.lines.isNotEmpty() -> BlockFit.WHOLE
    roomy && wrapped.lines.isNotEmpty() -> BlockFit.CLIPPED
    else -> BlockFit.COMPACT
}

/** How many label lines a block of [height] holds, given its [padding] and [lineHeight], up to [max]. */
internal fun linesThatFit(height: Float, padding: Float, lineHeight: Float, max: Int): Int =
    ((height - padding) / lineHeight).toInt().coerceIn(0, max)

/** The tops of tasks scrolled out of view: above the viewport, and below it. */
internal data class Hidden(val above: List<Float>, val below: List<Float>)

/**
 * Which of the blocks at [tops] (each [block] tall) are out of a viewport from [viewTop] to
 * [viewBottom]. A block is above once all of it has scrolled past; one cut by the bottom edge counts
 * as below, since its title is what is hidden.
 */
internal fun hidden(tops: List<Float>, block: Float, viewTop: Float, viewBottom: Float): Hidden =
    Hidden(tops.filter { it + block <= viewTop }, tops.filter { it + block > viewBottom })
