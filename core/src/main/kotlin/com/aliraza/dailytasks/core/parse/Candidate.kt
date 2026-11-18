package com.aliraza.dailytasks.core.parse

import com.aliraza.dailytasks.core.model.Priority
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** A time as typed. [Ambiguous] is an hour from 1 to 12 with no am or pm; [TimeResolver] settles it. */
internal sealed interface TimeValue {
    data class Exact(val time: LocalTime) : TimeValue

    data class Ambiguous(val hour: Int, val minute: Int) : TimeValue
}

/** A possible match covering tokens [start] until [end] (exclusive). */
internal data class Candidate(
    val kind: SpanKind,
    val start: Int,
    val end: Int,
    val date: LocalDate? = null,
    val time: TimeValue? = null,
    val priority: Priority? = null,
    val listName: String? = null,
    /** [listName] names a list to create, as no existing one matched the tag. */
    val newList: Boolean = false,
)

internal fun interface Matcher {
    fun match(ctx: ParseContext, index: Int): Candidate?
}

internal class ParseContext(
    val tokens: List<Token>,
    val now: LocalDateTime,
    val dayFirst: Boolean,
    val listNames: List<String>,
) {
    val today: LocalDate = now.toLocalDate()

    /** Token index right after the accepted date, so "tomorrow morning" can attach its day part. */
    var dateEnd: Int = -1

    fun word(index: Int): String? = tokens.getOrNull(index)?.word

    /** Only priority and list tags follow, so "Call bank fri p1" still ends with "fri". */
    fun isLastWord(index: Int): Boolean = (index + 1 until tokens.size).all { isTag(tokens[it].word) }

    private fun isTag(word: String) = word in Vocabulary.priorities || word.startsWith("#")
}
