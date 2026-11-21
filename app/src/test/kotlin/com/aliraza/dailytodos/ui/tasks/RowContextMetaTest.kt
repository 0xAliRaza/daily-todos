package com.aliraza.dailytodos.ui.tasks

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.core.model.TaskList
import com.aliraza.dailytodos.data.SubtaskProgress
import com.aliraza.dailytodos.ui.components.DateTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/** What a row's meta line is given on each screen. */
class RowContextMetaTest {
    private val now = LocalDateTime.of(2026, 11, 21, 18, 0)
    private val today = now.toLocalDate()
    private val daily = TaskList(id = "daily", name = "Daily", position = 0, isDefault = true)
    private val work = TaskList(id = "work", name = "Work", position = 1, isDefault = false)
    private val ctx = RowContext(
        now = now,
        lists = listOf(daily, work).associateBy { it.id },
        selection = Selection(),
        open = {},
        toggle = {},
        subtasks = mapOf("pack" to SubtaskProgress(done = 1, total = 3)),
        zone = ZoneOffset.UTC,
    )

    private fun task(
        id: String = "t",
        listId: String = daily.id,
        date: LocalDate? = today,
        time: LocalTime? = null,
        completedAt: Instant? = null,
    ) = Task(
        id = id,
        listId = listId,
        title = id,
        dueDate = date,
        dueTime = time,
        completedAt = completedAt,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun onTodayARowShowsOnlyItsTimeAndAPassedTimeStaysGrey() {
        val meta = ctx.meta(task(time = LocalTime.of(16, 0)), showDate = false)
        assertNull(meta.date)
        assertEquals(LocalTime.of(16, 0), meta.time)
        assertEquals(DateTone.PASSED, meta.tone)
        assertEquals(Duration.ofHours(2), meta.passedFor)
    }

    @Test
    fun onlyAPassedTimeSaysHowLongAgo() {
        assertNull(ctx.meta(task(time = LocalTime.of(20, 0)), showDate = false).passedFor)
        assertNull(ctx.meta(task(date = today.minusDays(1), time = LocalTime.of(16, 0)), showDate = true).passedFor)
        assertNull(ctx.meta(task(time = LocalTime.of(16, 0), completedAt = Instant.EPOCH), showDate = false).passedFor)
    }

    @Test
    fun overdueRowsKeepTheirDateAndRed() {
        val meta = ctx.meta(task(date = today.minusDays(2)), showDate = true)
        assertEquals(today.minusDays(2), meta.date)
        assertEquals(DateTone.OVERDUE, meta.tone)
    }

    @Test
    fun theListNameShowsOnlyForOtherListsUnlessTheScreenAsks() {
        assertNull(ctx.meta(task(), showDate = false).listName)
        assertEquals("Work", ctx.meta(task(listId = work.id), showDate = false).listName)
        assertNull(ctx.meta(task(listId = work.id), showDate = true, list = ListLabel.HIDDEN).listName)
        assertEquals("Daily", ctx.meta(task(), showDate = true, list = ListLabel.ALWAYS).listName)
    }

    @Test
    fun aRowCountsItsSubtasks() {
        assertEquals(1 to 3, ctx.meta(task(id = "pack"), showDate = false).subtasks)
        assertNull(ctx.meta(task(), showDate = false).subtasks)
    }

    @Test
    fun aTaskDoneTodayShowsWhenItWasTickedAndOneDoneEarlierDoesNot() {
        val ticked = now.withHour(17).withMinute(48).toInstant(ZoneOffset.UTC)
        assertEquals(LocalTime.of(17, 48), ctx.meta(task(completedAt = ticked), showDate = false).doneAt)
        val yesterday = now.minusDays(1).toInstant(ZoneOffset.UTC)
        assertNull(ctx.meta(task(completedAt = yesterday), showDate = false).doneAt)
        assertNull(ctx.meta(task(), showDate = false).doneAt)
    }

    @Test
    fun searchFindsEveryMatchIgnoringCase() {
        assertEquals(listOf(0..3, 11..14), matchRanges("Call bank, call mum", " CALL "))
        assertEquals(emptyList<IntRange>(), matchRanges("Call bank", "  "))
        assertEquals(emptyList<IntRange>(), matchRanges("Call bank", "milk"))
    }

    @Test
    fun searchSetsOnlyTheMatchesInTheHighlight() {
        val bold = SpanStyle(fontWeight = FontWeight.Bold)
        val title = highlightMatches("Call bank, call mum", "call", bold)
        assertEquals("Call bank, call mum", title.text)
        assertEquals(listOf(0 to 4, 11 to 15), title.spanStyles.map { it.start to it.end })
    }
}
