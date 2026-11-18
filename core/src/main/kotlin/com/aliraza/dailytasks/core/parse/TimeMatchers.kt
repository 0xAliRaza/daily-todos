package com.aliraza.dailytasks.core.parse

import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** Matchers that produce a time, or a date and a time together. */
internal object TimeMatchers {
    private val withMeridiem = Regex("^(\\d{1,2})(?:[:.](\\d{2}))?(a\\.?m|p\\.?m)$")
    private val clock = Regex("^(\\d{1,2})(?:([:.])(\\d{2}))?$")
    private val meridiemWords = setOf("am", "a.m", "pm", "p.m")
    private const val HALF_DAY = 12
    private const val LAST_HOUR = 23
    private const val LAST_MINUTE = 59

    /** Date and time at once; tried before plain dates and times so both slots are taken together. */
    val combined: List<Matcher> = listOf(
        Matcher(::tonight),
        Matcher(::thisDayPart),
        Matcher(::timeOffset),
    )

    val all: List<Matcher> = listOf(
        Matcher(::dayPartAfterDate),
        Matcher(::inTheDayPart),
        Matcher(::namedTime),
        Matcher(::clockTime),
    )

    private fun tonight(ctx: ParseContext, index: Int): Candidate? {
        val token = ctx.tokens[index]
        if (token.word != Vocabulary.TONIGHT || (index > 0 && token.isCapitalised)) return null
        return Candidate(SpanKind.DATE_TIME, index, index + 1, ctx.today, TimeValue.Exact(Vocabulary.tonightTime))
    }

    /** "this evening" means today in the evening. */
    private fun thisDayPart(ctx: ParseContext, index: Int): Candidate? {
        if (ctx.word(index) != "this") return null
        val time = Vocabulary.dayParts[ctx.word(index + 1)] ?: return null
        return Candidate(SpanKind.DATE_TIME, index, index + 2, ctx.today, TimeValue.Exact(time))
    }

    /** "in 30 minutes", "in 2 hours": an exact moment from now, to the minute. */
    private fun timeOffset(ctx: ParseContext, index: Int): Candidate? {
        val offset = Offsets.read(ctx, index) ?: return null
        if (offset.unit != ChronoUnit.MINUTES && offset.unit != ChronoUnit.HOURS) return null
        val at = ctx.now.plus(offset.amount, offset.unit).truncatedTo(ChronoUnit.MINUTES)
        return Candidate(
            SpanKind.DATE_TIME,
            index,
            offset.end,
            at.toLocalDate(),
            TimeValue.Exact(at.toLocalTime()),
        )
    }

    /** "tomorrow morning", "friday evening": a day part directly after a recognised date. */
    private fun dayPartAfterDate(ctx: ParseContext, index: Int): Candidate? {
        if (index != ctx.dateEnd) return null
        val time = Vocabulary.dayParts[ctx.word(index)] ?: return null
        return Candidate(SpanKind.TIME, index, index + 1, time = TimeValue.Exact(time))
    }

    /** "in the morning". A day part on its own ("Morning run") is not a time. */
    private fun inTheDayPart(ctx: ParseContext, index: Int): Candidate? {
        if (ctx.word(index) != "in" || ctx.word(index + 1) != "the") return null
        val dayPartAt = index + 2
        val time = Vocabulary.dayParts[ctx.word(dayPartAt)] ?: return null
        return Candidate(SpanKind.TIME, index, dayPartAt + 1, time = TimeValue.Exact(time))
    }

    /** "noon", "at midnight". */
    private fun namedTime(ctx: ParseContext, index: Int): Candidate? {
        val at = if (ctx.word(index) == "at") index + 1 else index
        val time = Vocabulary.namedTimes[ctx.word(at)] ?: return null
        return Candidate(SpanKind.TIME, index, at + 1, time = TimeValue.Exact(time))
    }

    /** "4pm", "4:30 pm", "16:30", "at 4", "at 4.30". A bare number without "at" is not a time. */
    private fun clockTime(ctx: ParseContext, index: Int): Candidate? {
        val hasAt = ctx.word(index) == "at"
        val at = if (hasAt) index + 1 else index
        val word = ctx.word(at) ?: return null
        val inline = withMeridiem.matchEntire(word)?.destructured
        val (end, time) = when {
            inline != null -> (at + 1) to inline.let { (hour, minute, marker) -> meridiem(hour, minute, marker) }
            else -> clockWithoutInlineMeridiem(ctx, at, word, hasAt) ?: return null
        }
        return time?.let { Candidate(SpanKind.TIME, index, end, time = it) }
    }

    private fun clockWithoutInlineMeridiem(
        ctx: ParseContext,
        at: Int,
        word: String,
        hasAt: Boolean,
    ): Pair<Int, TimeValue?>? {
        val (hour, separator, minute) = clock.matchEntire(word)?.destructured ?: return null
        val marker = ctx.word(at + 1)?.takeIf { it in meridiemWords }
        if (marker != null) return (at + 2) to meridiem(hour, minute, marker)
        val counts = separator == ":" || hasAt
        return if (counts) (at + 1) to plain(hour, minute) else null
    }

    /** 12-hour clock: "12am" is midnight, "12pm" is noon. */
    private fun meridiem(hourText: String, minuteText: String, marker: String): TimeValue? {
        val hour = hourText.toInt()
        val minute = minuteText.ifEmpty { "0" }.toInt()
        if (hour !in 1..HALF_DAY || minute > LAST_MINUTE) return null
        val pm = marker.startsWith("p")
        val hour24 = hour % HALF_DAY + if (pm) HALF_DAY else 0
        return TimeValue.Exact(LocalTime.of(hour24, minute))
    }

    /** No am or pm: 0, 13 to 23 and a leading zero ("09:30") are 24-hour; 1 to 12 are ambiguous. */
    private fun plain(hourText: String, minuteText: String): TimeValue? {
        val hour = hourText.toInt()
        val minute = minuteText.ifEmpty { "0" }.toInt()
        if (hour > LAST_HOUR || minute > LAST_MINUTE) return null
        val unambiguous = hour == 0 || hour > HALF_DAY || hourText.startsWith("0")
        return if (unambiguous) TimeValue.Exact(LocalTime.of(hour, minute)) else TimeValue.Ambiguous(hour, minute)
    }
}
