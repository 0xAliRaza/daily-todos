package com.aliraza.dailytasks.core.parse

import com.aliraza.dailytasks.core.model.Priority

/**
 * Reads one typed line into a title, date, time, priority and list (PLAN.md section 7).
 *
 * Words are scanned left to right. At each word the matchers are tried in order and the first one whose
 * result still fits is taken: the first date and the first time win, and later ones stay in the title.
 */
object QuickAddParser {
    private val matchers: List<Matcher> =
        TagMatchers.all + TimeMatchers.combined + DateMatchers.all + TimeMatchers.all

    private val edgePunctuation = charArrayOf(',', ';', ':', '-', ' ')

    fun parse(input: QuickAddInput): ParseResult {
        val tokens = Tokenizer.tokenize(input.text)
        val ctx = ParseContext(tokens, input.now, input.dayFirst, input.listNames)
        val accepted = scan(ctx, input.excluded)
        val title = titleWithout(tokens, accepted)
        if (accepted.isEmpty() || title.isEmpty()) return ParseResult(title = input.text.trim())

        val date = accepted.firstNotNullOfOrNull { it.date }
        val time = accepted.firstNotNullOfOrNull { it.time }
        val (resolvedDate, resolvedTime) = TimeResolver.resolve(date, time, input.now)
        return ParseResult(
            title = title,
            date = resolvedDate,
            time = resolvedTime,
            priority = accepted.firstNotNullOfOrNull { it.priority } ?: Priority.NONE,
            listName = accepted.firstNotNullOfOrNull { it.listName },
            spans = accepted.map { Span(it.kind, charRange(tokens, it)) },
        )
    }

    private fun scan(ctx: ParseContext, excluded: Set<IntRange>): List<Candidate> {
        val accepted = mutableListOf<Candidate>()
        var index = 0
        while (index < ctx.tokens.size) {
            val candidate = matchers.firstNotNullOfOrNull { matcher ->
                matcher.match(ctx, index)?.takeIf { fits(it, accepted) && !isExcluded(ctx, it, excluded) }
            }
            if (candidate == null) {
                index++
                continue
            }
            accepted += candidate
            if (candidate.kind == SpanKind.DATE) ctx.dateEnd = candidate.end
            index = candidate.end
        }
        return accepted
    }

    /** Each slot (date, time, priority, list) is filled at most once. */
    private fun fits(candidate: Candidate, accepted: List<Candidate>): Boolean {
        val hasDate = accepted.any { it.date != null }
        val hasTime = accepted.any { it.time != null }
        return when (candidate.kind) {
            SpanKind.DATE -> !hasDate
            SpanKind.TIME -> !hasTime
            SpanKind.DATE_TIME -> !hasDate && !hasTime
            SpanKind.PRIORITY -> accepted.none { it.kind == SpanKind.PRIORITY }
            SpanKind.LIST -> accepted.none { it.kind == SpanKind.LIST }
        }
    }

    private fun isExcluded(ctx: ParseContext, candidate: Candidate, excluded: Set<IntRange>): Boolean {
        val range = charRange(ctx.tokens, candidate)
        return excluded.any { it.first <= range.last && range.first <= it.last }
    }

    private fun charRange(tokens: List<Token>, candidate: Candidate): IntRange =
        tokens[candidate.start].wordStart until tokens[candidate.end - 1].wordEnd

    private fun titleWithout(tokens: List<Token>, accepted: List<Candidate>): String {
        val consumed = accepted.flatMap { it.start until it.end }.toSet()
        return tokens.filterIndexed { index, _ -> index !in consumed }
            .joinToString(" ") { it.raw }
            .trim(*edgePunctuation)
    }
}
