package com.aliraza.dailytodos.ui.calendar

import com.aliraza.dailytodos.core.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class CalendarLayoutTest {

    private val day = LocalDate.of(2026, 11, 21)

    private fun task(id: String, time: LocalTime?) = Task(
        id = id,
        listId = "l",
        title = id,
        dueDate = day,
        dueTime = time,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    /** Every character is 1 wide, so widths read as character counts. */
    private val chars = { s: String -> s.length.toFloat() }

    @Test
    fun dateOnlyTasksGoToTheNoTimeRowInTheirOrder() {
        val call = task("call", LocalTime.of(16, 0))
        val renew = task("renew", null)
        val read = task("read", null)
        val gym = task("gym", LocalTime.of(7, 30))
        val split = splitByTime(listOf(renew, call, read, gym))
        assertEquals(listOf(call, gym), split.timed)
        assertEquals(listOf(renew, read), split.untimed)
    }

    @Test
    fun aDayWithoutDateOnlyTasksHasAnEmptyNoTimeRow() {
        val split = splitByTime(listOf(task("call", LocalTime.NOON)))
        assertEquals(emptyList<Task>(), split.untimed)
    }

    @Test
    fun wordsWrapWholeOntoTheLinesThereAre() {
        assertEquals(Wrapped(listOf("Pick up", "parcel"), complete = true), wrapWhole("Pick up parcel", 7f, 2, chars))
        assertEquals(Wrapped(listOf("Gym"), complete = true), wrapWhole("Gym", 6f, 2, chars))
    }

    @Test
    fun aWordTooWideForALineIsNeverBroken() {
        assertEquals(Wrapped(listOf("Team"), complete = false), wrapWhole("Team standup", 6f, 2, chars))
        assertEquals(Wrapped(emptyList(), complete = false), wrapWhole("Renewal", 6f, 2, chars))
    }

    @Test
    fun wordsBeyondTheLastLineAreLeftOut() {
        assertEquals(Wrapped(listOf("Renew"), complete = false), wrapWhole("Renew passport", 10f, 1, chars))
        assertEquals(Wrapped(emptyList(), complete = false), wrapWhole("Renew", 10f, 0, chars))
    }

    @Test
    fun aNarrowBlockThatCannotShowWholeWordsBecomesCompact() {
        val partial = Wrapped(listOf("Team"), complete = false)
        assertEquals(BlockFit.COMPACT, blockFit(partial, lines = 2, roomy = false))
        assertEquals(BlockFit.CLIPPED, blockFit(partial, lines = 2, roomy = true))
        assertEquals(BlockFit.WHOLE, blockFit(Wrapped(listOf("Gym"), complete = true), lines = 1, roomy = false))
    }

    @Test
    fun aBlockWithNoRoomForALineIsCompactEvenWhenWide() {
        assertEquals(BlockFit.COMPACT, blockFit(Wrapped(listOf("Gym"), complete = true), lines = 0, roomy = true))
    }

    @Test
    fun theLinesABlockHoldsFollowItsHeight() {
        // Lines of 14 with 6 of padding: 34 holds two, a half hour of 23 holds one, 19 none.
        assertEquals(2, linesThatFit(34f, 6f, 14f, max = 2))
        assertEquals(2, linesThatFit(80f, 6f, 14f, max = 2))
        assertEquals(1, linesThatFit(23f, 6f, 14f, max = 2))
        assertEquals(0, linesThatFit(19f, 6f, 14f, max = 2))
    }

    @Test
    fun blocksScrolledPastCountAboveAndCutOnesBelow() {
        val tops = listOf(0f, 100f, 400f, 590f, 700f)
        val seen = hidden(tops, block = 20f, viewTop = 100f, viewBottom = 600f)
        assertEquals(listOf(0f), seen.above)
        assertEquals(listOf(590f, 700f), seen.below)
    }
}
