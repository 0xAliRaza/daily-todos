package com.aliraza.dailytasks.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliraza.dailytasks.core.model.NewTask
import com.aliraza.dailytasks.core.parse.QuickAddInput
import com.aliraza.dailytasks.core.parse.QuickAddParser
import com.aliraza.dailytasks.core.sort.TaskOrdering
import com.aliraza.dailytasks.core.sort.TodaySections
import com.aliraza.dailytasks.data.ListRepository
import com.aliraza.dailytasks.data.TaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Today: overdue, timed and the rest, across every list. */
class TodayViewModel(private val tasks: TaskRepository, private val lists: ListRepository, private val clock: Clock) :
    ViewModel() {

    /** The date, re-checked every minute so the view rolls over at midnight. */
    private val today = flow {
        while (true) {
            emit(LocalDate.now(clock))
            delay(Duration.ofMinutes(1).toMillis())
        }
    }.distinctUntilChanged()

    val sections: StateFlow<TodaySections?> =
        combine(tasks.observeOpenDated(), lists.observe(), today) { open, allLists, date ->
            TaskOrdering(allLists.associate { it.id to it.position }).today(open, date)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** Adds a task from one typed line; date, time, priority and #list are read from it. */
    fun add(line: String) {
        if (line.isBlank()) return
        viewModelScope.launch {
            val allLists = lists.all()
            val parsed = QuickAddParser.parse(
                QuickAddInput(line, LocalDateTime.now(clock), listNames = allLists.map { it.name }),
            )
            tasks.addTask(
                NewTask(
                    title = parsed.title,
                    listId = allLists.firstOrNull { it.name == parsed.listName }?.id,
                    priority = parsed.priority,
                    dueDate = parsed.date,
                    dueTime = parsed.time,
                ),
            )
        }
    }

    fun complete(id: String) {
        viewModelScope.launch { tasks.setCompleted(listOf(id), completed = true) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
