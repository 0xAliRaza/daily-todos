package com.aliraza.dailytasks.core.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * A task about to be created, usually from the quick-add line.
 *
 * @property listId the list it goes into; null means the default list.
 * @property useDefaultDate false when the user tapped away the "tomorrow" chip.
 */
data class NewTask(
    val title: String,
    val listId: String? = null,
    val priority: Priority = Priority.NONE,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val notes: String = "",
    val useDefaultDate: Boolean = true,
) {
    /**
     * PLAN.md section 4, rule 3: an undated task created in the default list gets tomorrow, so the default
     * reminder time applies. Tasks in any other list stay undated, so a grocery list does not ring per item.
     */
    fun effectiveDueDate(inDefaultList: Boolean, settings: Settings, today: LocalDate): LocalDate? =
        dueDate ?: today.plusDays(1).takeIf { useDefaultDate && inDefaultList && settings.undatedGetsTomorrow }
}
