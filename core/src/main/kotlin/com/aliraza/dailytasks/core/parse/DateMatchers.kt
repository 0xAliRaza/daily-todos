package com.aliraza.dailytasks.core.parse

import java.time.DateTimeException
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

/** Matchers that produce a date, each with an optional introducing word ("on", "by", "next"). */
internal object DateMatchers {
    private val dayNumber = Regex("^(\\d{1,2})(st|nd|rd|th)?$")
    private val year = Regex("^20\\d{2}$")
    private val numeric = Regex("^(\\d{1,2})/(\\d{1,2})(?:/(\\d{2}|\\d{4}))?$")
    private val iso = Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$")
    private const val CENTURY = 2000
    private const val MAX_DAY = 31

    val all: List<Matcher> = listOf(
        Matcher { ctx, i -> prefixed(ctx, i, Vocabulary.datePrefixes) { at, _ -> dayAfterTomorrow(ctx, at) } },
        Matcher { ctx, i -> prefixed(ctx, i, Vocabulary.datePrefixes) { at, p -> relativeDay(ctx, at, p) } },
        Matcher { ctx, i -> prefixed(ctx, i, Vocabulary.weekdayPrefixes) { at, p -> weekday(ctx, at, p) } },
        Matcher { ctx, i -> prefixed(ctx, i, Vocabulary.datePrefixes) { at, _ -> dayMonth(ctx, at) } },
        Matcher { ctx, i -> prefixed(ctx, i, Vocabulary.datePrefixes) { at, _ -> monthDay(ctx, at) } },
        Matcher { ctx, i -> prefixed(ctx, i, Vocabulary.datePrefixes) { at, _ -> numericDate(ctx, at) } },
        Matcher { ctx, i -> prefixed(ctx, i, Vocabulary.datePrefixes) { at, _ -> isoDate(ctx, at) } },
        Matcher(::dayOffset),
    )

    /** A matched date: the token index after it and the date. */
    private data class Hit(val end: Int, val date: LocalDate)

    private inline fun prefixed(
        ctx: ParseContext,
        index: Int,
        prefixes: Set<String>,
        body: (at: Int, prefix: String?) -> Hit?,
    ): Candidate? {
        val prefix = ctx.word(index)?.takeIf { it in prefixes }
        val at = if (prefix != null) index + 1 else index
        val hit = body(at, prefix) ?: return null
        return Candidate(SpanKind.DATE, index, hit.end, date = hit.date)
    }

    private fun dayAfterTomorrow(ctx: ParseContext, at: Int): Hit? {
        val start = if (ctx.word(at) == "the") at + 1 else at
        val words = DAY_AFTER_TOMORROW.indices.map { ctx.word(start + it) }
        return if (words == DAY_AFTER_TOMORROW) Hit(start + words.size, ctx.today.plusDays(2)) else null
    }

    private fun relativeDay(ctx: ParseContext, at: Int, prefix: String?): Hit? {
        val token = ctx.tokens.getOrNull(at) ?: return null
        if (at > 0 && token.isCapitalised) return null
        val offset = Vocabulary.relativeDays[token.word]
            ?: Vocabulary.shortRelativeDays[token.word]?.takeIf { prefix != null || ctx.isLastWord(at) }
            ?: return null
        return Hit(at + 1, ctx.today.plusDays(offset))
    }

    private fun weekday(ctx: ParseContext, at: Int, prefix: String?): Hit? {
        val word = ctx.word(at) ?: return null
        val day = Vocabulary.fullWeekdays[word]
            ?: Vocabulary.shortWeekdays[word]?.takeIf {
                prefix != null || ctx.isLastWord(
                    at,
                ) || TimeMatchers.startsAt(ctx, at + 1)
            }
            ?: return null
        var ahead = (day.value - ctx.today.dayOfWeek.value + DAYS_IN_WEEK) % DAYS_IN_WEEK
        if (ahead == 0 && prefix == "next") ahead = DAYS_IN_WEEK
        return Hit(at + 1, ctx.today.plusDays(ahead.toLong()))
    }

    /** "25 dec", "25th of december 2026". */
    private fun dayMonth(ctx: ParseContext, at: Int): Hit? {
        val day = dayOf(ctx.word(at)) ?: return null
        val monthAt = if (ctx.word(at + 1) == "of") at + 2 else at + 1
        val month = Vocabulary.months[ctx.word(monthAt)] ?: return null
        return withYear(ctx, monthAt + 1, month, day)
    }

    /** "dec 25", "december 25th 2026". A month name alone is never a date. */
    private fun monthDay(ctx: ParseContext, at: Int): Hit? {
        val month = Vocabulary.months[ctx.word(at)] ?: return null
        val day = dayOf(ctx.word(at + 1)) ?: return null
        return withYear(ctx, at + 2, month, day)
    }

    private fun withYear(ctx: ParseContext, yearAt: Int, month: Month, day: Int): Hit? {
        val explicitYear = ctx.word(yearAt)?.takeIf { year.matches(it) }?.toInt()
        val date = if (explicitYear != null) {
            dateOrNull(explicitYear, month.value, day)
        } else {
            nextOccurrence(ctx.today, month.value, day)
        }
        val end = if (explicitYear != null) yearAt + 1 else yearAt
        return date?.let { Hit(end, it) }
    }

    /** "25/12", "25/12/26", "25/12/2026"; "12/25" when the locale writes the month first. */
    private fun numericDate(ctx: ParseContext, at: Int): Hit? {
        val match = numeric.matchEntire(ctx.word(at) ?: return null) ?: return null
        val (first, second, typedYear) = match.destructured
        val (day, month) = if (ctx.dayFirst) first.toInt() to second.toInt() else second.toInt() to first.toInt()
        val date = when (typedYear.length) {
            0 -> nextOccurrence(ctx.today, month, day)
            2 -> dateOrNull(CENTURY + typedYear.toInt(), month, day)
            else -> dateOrNull(typedYear.toInt(), month, day)
        }
        return date?.let { Hit(at + 1, it) }
    }

    private fun isoDate(ctx: ParseContext, at: Int): Hit? {
        val match = iso.matchEntire(ctx.word(at) ?: return null) ?: return null
        val (year, month, day) = match.destructured
        return dateOrNull(year.toInt(), month.toInt(), day.toInt())?.let { Hit(at + 1, it) }
    }

    /** "in 3 days", "in a week", "in 2w". Minute and hour offsets are in [TimeMatchers]. */
    private fun dayOffset(ctx: ParseContext, index: Int): Candidate? {
        val offset = Offsets.read(ctx, index) ?: return null
        val days = when (offset.unit) {
            ChronoUnit.DAYS -> offset.amount
            ChronoUnit.WEEKS -> offset.amount * DAYS_IN_WEEK
            else -> return null
        }
        return Candidate(SpanKind.DATE, index, offset.end, date = ctx.today.plusDays(days))
    }

    private fun dayOf(word: String?): Int? =
        word?.let { dayNumber.matchEntire(it) }?.groupValues?.get(1)?.toInt()?.takeIf { it in 1..MAX_DAY }

    /** The date this year, or next year if it has passed (or does not exist this year, like 29 feb). */
    private fun nextOccurrence(today: LocalDate, month: Int, day: Int): LocalDate? =
        (0..LEAP_CYCLE).firstNotNullOfOrNull { ahead ->
            dateOrNull(today.year + ahead, month, day)?.takeIf { !it.isBefore(today) }
        }

    private fun dateOrNull(year: Int, month: Int, day: Int): LocalDate? = try {
        LocalDate.of(year, month, day)
    } catch (_: DateTimeException) {
        null
    }

    private val DAY_AFTER_TOMORROW = listOf("day", "after", "tomorrow")
    private const val DAYS_IN_WEEK = 7
    private const val LEAP_CYCLE = 4
}
