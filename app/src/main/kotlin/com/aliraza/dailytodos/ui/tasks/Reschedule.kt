package com.aliraza.dailytodos.ui.tasks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.pluralStringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.ui.components.DueDateSheet
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** The task on another day at the same time; no date also drops the time, which needs a day. */
internal fun Task.movedTo(date: LocalDate?): Task =
    if (date == null) copy(dueDate = null, dueTime = null) else copy(dueDate = date)

/** Hosts the due date sheet for [ids] while it is open: one task by its title, several by their count. */
@Composable
internal fun RescheduleHost(viewModel: TasksViewModel, ids: List<String>, onDone: () -> Unit) {
    val now by viewModel.now.collectAsStateWithLifecycle()
    val today = now.toLocalDate()
    val pick = { date: LocalDate? -> viewModel.reschedule(ids, date) }
    if (ids.size > 1) {
        // Several tasks may mix dated and undated ones, so "No date" is always offered.
        val title = pluralStringResource(R.plurals.reschedule_many, ids.size, ids.size)
        DueDateSheet(title, current = today, today, onPick = pick, onDismiss = onDone)
        return
    }
    // Null while loading; empty once the task is gone (completed or deleted elsewhere), which closes the sheet.
    val found by remember(ids) { viewModel.observeTask(ids.first()).map { listOfNotNull(it) } }
        .collectAsStateWithLifecycle(null)
    val task = found?.let { it.firstOrNull() ?: return LaunchedEffect(Unit) { onDone() } } ?: return
    DueDateSheet(task.title, task.dueDate, today, onPick = pick, onDismiss = onDone)
}
