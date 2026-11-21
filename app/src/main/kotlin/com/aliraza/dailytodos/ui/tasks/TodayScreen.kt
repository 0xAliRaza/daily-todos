package com.aliraza.dailytodos.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.core.sort.TodaySections
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.EmptyAction
import com.aliraza.dailytodos.ui.components.EmptyState
import com.aliraza.dailytodos.ui.components.HeaderAction
import com.aliraza.dailytodos.ui.theme.body
import com.aliraza.dailytodos.ui.theme.tokens

/**
 * Today: overdue tasks (with Reschedule), then "To do" with how many are done, and the done tasks behind
 * the pinned Completed bar. With nothing left it says "All done for today" and offers tomorrow
 * ([onSeeTomorrow] switches to Upcoming); with nothing at all it says the day is clear.
 */
@Composable
fun TodayContent(viewModel: TasksViewModel, ctx: RowContext, padding: PaddingValues, onSeeTomorrow: () -> Unit = {}) {
    val sections by viewModel.todaySections.collectAsStateWithLifecycle()
    val done by viewModel.completedToday.collectAsStateWithLifecycle()
    val upcoming by viewModel.upcoming.collectAsStateWithLifecycle()
    val current = sections ?: return
    var showDone by rememberSaveable { mutableStateOf(false) }
    if (current.all.isEmpty() && done.isEmpty()) {
        EmptyState(stringResource(R.string.today_empty_title), stringResource(R.string.today_empty_body), padding)
        return
    }
    val allDone = current.all.isEmpty()
    PinnedCompletedPage(padding, done.size, pinned = !showDone, onOpen = { showDone = true }) { inner ->
        if (allDone && !showDone) {
            val waiting = upcoming?.get(ctx.today.plusDays(1)).orEmpty().size
            AllDone(done.size, waiting, inner, onSeeTomorrow)
        } else {
            TodayList(current, done, ctx, inner, showDone) { showDone = false }
        }
    }
}

@Composable
private fun TodayList(
    current: TodaySections,
    done: List<Task>,
    ctx: RowContext,
    padding: PaddingValues,
    showDone: Boolean,
    onHideDone: () -> Unit,
) {
    val overdueIds = current.overdue.map { it.id }
    val moveAll = HeaderAction(
        stringResource(R.string.action_reschedule),
        AppIcons.Reschedule,
        stringResource(R.string.today_reschedule_overdue),
    ) { ctx.reschedule(overdueIds) }
    val doneNote = if (done.isEmpty()) null else pluralStringResource(R.plurals.today_done_count, done.size, done.size)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        if (current.all.isEmpty()) item(key = "allDone") { AllDoneBanner(Modifier.animateItem()) }
        section(R.string.today_section_overdue, current.overdue, ctx, showDate = true, action = moveAll)
        // Timed tasks first, then the rest. On Today a row shows only its time: the day goes without saying.
        section(R.string.today_section_todo, current.timed + current.rest, ctx, trailing = doneNote)
        if (showDone) completedSection(done, ctx, ListLabel.OTHER_LISTS, onHideDone)
    }
}

/** "All done for today", how many were finished, what tomorrow holds and a way to it. */
@Composable
private fun AllDone(finished: Int, waiting: Int, padding: PaddingValues, onSeeTomorrow: () -> Unit) {
    val tomorrow = if (waiting > 0) {
        pluralStringResource(R.plurals.today_done_tomorrow, waiting, waiting)
    } else {
        stringResource(R.string.today_done_tomorrow_none)
    }
    EmptyState(
        stringResource(R.string.today_done_title),
        "${pluralStringResource(R.plurals.today_done_finished, finished, finished)} $tomorrow",
        padding,
        action = EmptyAction(stringResource(R.string.today_see_tomorrow), onSeeTomorrow).takeIf { waiting > 0 },
    )
}

/** One accent-container line over the opened done tasks, so the all-done state still reads. */
@Composable
private fun AllDoneBanner(modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.tokens
    Row(
        modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp).fillMaxWidth().height(48.dp)
            .clip(RoundedCornerShape(14.dp)).background(tokens.accentContainer).padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Mark, null, tint = tokens.onAccentContainer, modifier = Modifier.size(22.dp))
        Text(
            stringResource(R.string.today_done_title),
            style = MaterialTheme.typography.body,
            fontWeight = FontWeight.SemiBold,
            color = tokens.onAccentContainer,
        )
    }
}
