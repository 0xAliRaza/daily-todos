package com.aliraza.dailytodos.core.parse

import com.aliraza.dailytodos.core.model.Priority
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * What the quick-add parser needs.
 *
 * @property now the current local date-time; relative words resolve against it.
 * @property listNames the names of existing lists, for `#name`.
 * @property dayFirst the locale writes numeric dates day first (25/12) rather than month first (12/25).
 * @property excluded character ranges the user tapped to un-recognise; nothing overlapping them is matched.
 * @property untitled the line holds no title yet, only words to read (the add bar's start words). Its
 *   words are read even with nothing else beside them, and the title stays empty.
 */
data class QuickAddInput(
    val text: String,
    val now: LocalDateTime,
    val listNames: List<String> = emptyList(),
    val dayFirst: Boolean = true,
    val excluded: Set<IntRange> = emptySet(),
    val untitled: Boolean = false,
)

enum class SpanKind { DATE, TIME, DATE_TIME, PRIORITY, LIST }

/** A recognised part of the typed line; [range] indexes characters of the input text, inclusive. */
data class Span(val kind: SpanKind, val range: IntRange)

/**
 * What the line means. [date] is set whenever [time] is: a time alone resolves to today or tomorrow.
 * [listName] is one of the input list names exactly as given, or, when [newList] is set, the name of a
 * list to create from a tag that matched none ("#weekend-trip" is "Weekend trip").
 */
data class ParseResult(
    val title: String,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val priority: Priority = Priority.NONE,
    val listName: String? = null,
    val spans: List<Span> = emptyList(),
    val newList: Boolean = false,
)
