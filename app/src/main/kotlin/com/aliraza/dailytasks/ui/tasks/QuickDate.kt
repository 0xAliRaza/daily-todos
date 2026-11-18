package com.aliraza.dailytasks.ui.tasks

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytasks.R
import com.aliraza.dailytasks.core.model.Task
import com.aliraza.dailytasks.ui.components.AppIcons
import com.aliraza.dailytasks.ui.components.dateTone
import com.aliraza.dailytasks.ui.components.toneColor
import com.aliraza.dailytasks.ui.components.weekdayShort
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** A day offered with one tap when rescheduling. */
internal data class QuickDate(@param:StringRes val label: Int, val date: LocalDate)

/** Today, tomorrow and next Monday: the days the sheet's date menu and the reschedule sheet offer. */
internal fun quickDates(today: LocalDate): List<QuickDate> = listOf(
    QuickDate(R.string.day_today, today),
    QuickDate(R.string.day_tomorrow, today.plusDays(1)),
    QuickDate(R.string.date_next_week, today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))),
)

/** The task on another day at the same time; no date also drops the time, which needs a day. */
internal fun Task.movedTo(date: LocalDate?): Task =
    if (date == null) copy(dueDate = null, dueTime = null) else copy(dueDate = date)

/**
 * Opened by swiping a row right: the quick days, a date picker, and "No date". Each day shows its
 * weekday and its due colour, so "Next week" reads as "Mon".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RescheduleSheet(task: Task, today: LocalDate, onPick: (LocalDate?) -> Unit, onDismiss: () -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val pick = { date: LocalDate? ->
        onPick(date)
        onDismiss()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            task.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        quickDates(today).forEach { quick ->
            val ink = toneColor(dateTone(quick.date, today))
            DayOption(AppIcons.Today, stringResource(quick.label), weekdayShort(quick.date), ink) { pick(quick.date) }
        }
        DayOption(
            AppIcons.Calendar,
            stringResource(R.string.date_pick),
            null,
            MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            picking = true
        }
        if (task.dueDate != null) {
            DayOption(
                AppIcons.Close,
                stringResource(R.string.task_no_date),
                null,
                MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                pick(null)
            }
        }
        Spacer(Modifier.navigationBarsPadding().padding(bottom = 12.dp))
    }
    if (picking) {
        DateDialog(task.dueDate ?: today, onDismiss = { picking = false }) { pick(it) }
    }
}

@Composable
private fun DayOption(icon: ImageVector, label: String, weekday: String?, ink: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 52.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        weekday?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Hosts the reschedule sheet for [taskId] while it is open. */
@Composable
internal fun RescheduleHost(viewModel: TasksViewModel, taskId: String, onDone: () -> Unit) {
    val task by remember(taskId) { viewModel.observeTask(taskId) }.collectAsStateWithLifecycle(null)
    val now by viewModel.now.collectAsStateWithLifecycle()
    val current = task ?: return
    RescheduleSheet(current, now.toLocalDate(), onPick = { viewModel.reschedule(taskId, it) }, onDismiss = onDone)
}
