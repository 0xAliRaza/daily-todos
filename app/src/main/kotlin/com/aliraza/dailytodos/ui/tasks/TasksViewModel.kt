package com.aliraza.dailytodos.ui.tasks

import androidx.annotation.PluralsRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.focus.DoneDay
import com.aliraza.dailytodos.core.model.NewTask
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.core.model.Subtask
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.core.model.TaskList
import com.aliraza.dailytodos.core.parse.ParseResult
import com.aliraza.dailytodos.core.parse.QuickAddInput
import com.aliraza.dailytodos.core.parse.QuickAddParser
import com.aliraza.dailytodos.core.sort.TaskOrdering
import com.aliraza.dailytodos.core.sort.TodaySections
import com.aliraza.dailytodos.data.ListRepository
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.SubtaskProgress
import com.aliraza.dailytodos.data.SubtaskRepository
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.ui.components.DefaultDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Something just done that the snackbar offers to undo. */
data class UndoEvent(@param:PluralsRes val message: Int, val count: Int, val undo: () -> Unit)

/** One day in Upcoming; [tasks] is empty for a free day. */
data class UpcomingDay(val date: LocalDate, val tasks: List<Task>)

/**
 * Every task screen: Today, Upcoming, a list, Search, the task sheet and multi-select. One view model,
 * because the screens share the same streams and the same undo.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TasksViewModel(
    private val tasks: TaskRepository,
    private val subtasks: SubtaskRepository,
    private val listRepo: ListRepository,
    settingsRepo: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    /** The wall-clock minute, so "late" times and the date roll over while the app is open. */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now(clock).withSecond(0).withNano(0))
            delay(Duration.ofSeconds(TICK_SECONDS).toMillis())
        }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, LocalDateTime.now(clock))

    val lists: StateFlow<List<TaskList>> = listRepo.observe().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList(),
    )

    val settings: StateFlow<Settings> = settingsRepo.observe().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Settings(),
    )

    private val ordering = lists.map { all -> TaskOrdering(all.associate { it.id to it.position }) }

    private val openDated = tasks.observeOpenDated()

    private val today = now.map { it.toLocalDate() }.distinctUntilChanged()

    val todaySections: StateFlow<TodaySections?> = combine(openDated, ordering, today) { open, order, date ->
        order.today(open, date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), null)

    /** Every open task dated after today, grouped by day in time order. */
    val upcoming: StateFlow<Map<LocalDate, List<Task>>?> = combine(openDated, ordering, today) { open, order, date ->
        order.day(open.filter { it.dueDate?.isAfter(date) == true }).groupBy { it.dueDate!! }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), null)

    /** Every open dated task, past days included, grouped by day in time order: the calendar's data. */
    val byDay: StateFlow<Map<LocalDate, List<Task>>?> = combine(openDated, ordering) { open, order ->
        order.day(open).groupBy { it.dueDate!! }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), null)

    /** Today's done tasks, for the folded section under Today. */
    val completedToday: StateFlow<List<Task>> = today.flatMapLatest { tasks.observeCompletedOn(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    fun completedInList(listId: String): Flow<List<Task>> = tasks.observeCompletedInList(listId)

    /** What was done on each day from [first] to [last], for the calendar's month. */
    fun doneBetween(first: LocalDate, last: LocalDate): Flow<Map<LocalDate, DoneDay>> =
        tasks.done.observeBetween(first, last)

    /** Open tasks per list id, for the Lists screen. */
    val listCounts: StateFlow<Map<String, Int>> = tasks.observeOpenCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyMap())

    fun listTasks(listId: String): Flow<List<Task>> =
        combine(tasks.observeOpenInList(listId), ordering) { open, order -> order.list(open) }

    val query = MutableStateFlow("")

    val results: StateFlow<List<Task>> = query
        .debounce(SEARCH_DEBOUNCE_MS)
        .flatMapLatest { q -> if (q.isBlank()) flowOf(emptyList()) else tasks.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    private val undoEvents = MutableSharedFlow<UndoEvent>(extraBufferCapacity = 1)
    val undo: SharedFlow<UndoEvent> = undoEvents

    // Quick add

    /** Reads [text] in the add bar; see [QuickAddInput.untitled]. */
    fun parse(text: String, excluded: Set<IntRange>, untitled: Boolean = false): ParseResult = QuickAddParser.parse(
        QuickAddInput(
            text,
            LocalDateTime.now(clock),
            listNames = lists.value.map { it.name },
            excluded = excluded,
            untitled = untitled,
        ),
    )

    /**
     * The date a task typed as [parsed] would get without one of its own: the day picked in Upcoming, or
     * tomorrow for the default list (PLAN.md section 4, rule 3). Null when it stays undated.
     */
    fun defaultDate(
        parsed: ParseResult,
        s: Settings,
        contextListId: String?,
        contextDate: LocalDate?,
        off: Boolean,
    ): DefaultDate? {
        if (off || parsed.date != null || parsed.title.isBlank()) return null
        val time = s.defaultReminderTime.takeIf { s.dateOnlyGetsReminder }
        val date = contextDate ?: LocalDate.now(clock).plusDays(1).takeIf {
            s.undatedGetsTomorrow && targetList(parsed, contextListId)?.isDefault == true
        }
        return date?.let { DefaultDate(it, time) }
    }

    /** The list a typed task goes to; null for a list the tag asks to create, which isn't there yet. */
    private fun targetList(parsed: ParseResult, contextListId: String?): TaskList? {
        if (parsed.newList) return null
        val all = lists.value
        return all.firstOrNull { it.name == parsed.listName }
            ?: all.firstOrNull { it.id == contextListId }
            ?: all.firstOrNull { it.isDefault }
    }

    fun add(parsed: ParseResult, contextListId: String?, defaultDate: DefaultDate?) {
        if (parsed.title.isBlank()) return
        viewModelScope.launch {
            // "#name" that matched no list makes it now; a same-named list made meanwhile is reused.
            val created = parsed.listName?.takeIf { parsed.newList }?.let { name ->
                lists.value.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: listRepo.create(name)
            }
            tasks.addTask(
                NewTask(
                    title = parsed.title,
                    listId = created?.id ?: targetList(parsed, contextListId)?.id,
                    priority = parsed.priority,
                    dueDate = parsed.date ?: defaultDate?.date,
                    dueTime = parsed.time,
                    useDefaultDate = defaultDate != null,
                ),
            )
        }
    }

    // Task actions, each with undo

    fun setDone(ids: Collection<String>, done: Boolean) {
        if (ids.isEmpty()) return
        val batch = ids.toList()
        viewModelScope.launch {
            tasks.setCompleted(batch, done)
            val message = if (done) R.plurals.undo_completed else R.plurals.undo_reopened
            undoEvents.tryEmit(
                UndoEvent(message, batch.size) { viewModelScope.launch { tasks.setCompleted(batch, !done) } },
            )
        }
    }

    fun delete(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val batch = ids.toList()
        viewModelScope.launch {
            tasks.setDeleted(batch, deleted = true)
            undoEvents.tryEmit(
                UndoEvent(
                    R.plurals.undo_deleted,
                    batch.size,
                ) { viewModelScope.launch { tasks.setDeleted(batch, false) } },
            )
        }
    }

    fun reorder(before: List<String>, after: List<String>) {
        if (before == after) return
        viewModelScope.launch {
            tasks.reorder(after)
            undoEvents.tryEmit(
                UndoEvent(R.plurals.undo_reordered, 1) { viewModelScope.launch { tasks.reorder(before) } },
            )
        }
    }

    /** Moves [ids] to [date] (null for no date) keeping their times; undo puts each back where it was. */
    fun reschedule(ids: List<String>, date: LocalDate?) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val before = tasks.updateTasks(ids) { it.movedTo(date) }.associateBy { it.id }
            undoEvents.tryEmit(
                UndoEvent(R.plurals.undo_rescheduled, before.size) {
                    viewModelScope.launch {
                        tasks.updateTasks(before.keys) { now ->
                            val was = before.getValue(now.id)
                            now.copy(dueDate = was.dueDate, dueTime = was.dueTime)
                        }
                    }
                },
            )
        }
    }

    fun update(id: String, change: (Task) -> Task) {
        viewModelScope.launch { tasks.updateTask(id, change) }
    }

    // Task sheet

    fun observeTask(id: String): Flow<Task?> = tasks.observeTask(id)

    fun observeSubtasks(taskId: String): Flow<List<Subtask>> = subtasks.observe(taskId)

    /** Sub-task counts for every task that has some, so rows can show "1/3". */
    val subtaskProgress: StateFlow<Map<String, SubtaskProgress>> = subtasks.observeProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyMap())

    fun addSubtask(taskId: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { subtasks.add(taskId, title.trim()) }
    }

    fun setSubtaskDone(subtask: Subtask, done: Boolean) {
        viewModelScope.launch { subtasks.update(subtask.copy(done = done)) }
    }

    fun deleteSubtask(id: String) {
        viewModelScope.launch { subtasks.delete(id) }
    }

    // Lists

    fun createList(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { listRepo.create(name) }
    }

    /** Deletes a list and its tasks, with undo. The default list is refused further down. */
    fun deleteList(id: String) {
        viewModelScope.launch {
            listRepo.setDeleted(id, deleted = true)
            undoEvents.tryEmit(
                UndoEvent(R.plurals.undo_list_deleted, 1) { viewModelScope.launch { listRepo.setDeleted(id, false) } },
            )
        }
    }

    fun renameList(id: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { listRepo.rename(id, name) }
    }

    private companion object {
        const val STOP_MS = 5_000L
        const val TICK_SECONDS = 15L
        const val SEARCH_DEBOUNCE_MS = 120L
    }
}
