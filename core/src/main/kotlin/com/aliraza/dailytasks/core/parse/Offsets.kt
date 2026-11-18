package com.aliraza.dailytasks.core.parse

import java.time.temporal.ChronoUnit

/** Reads "in N unit" offsets: "in 30 minutes", "in an hour", "in 2h", "in 3 days", "in a week". */
internal object Offsets {
    data class Offset(val amount: Long, val unit: ChronoUnit, val end: Int)

    private val compact = Regex("^(\\d{1,3})([a-z]+)$")
    private val number = Regex("^\\d{1,3}$")
    private val wordAmounts = mapOf("a" to 1L, "an" to 1L, "one" to 1L)

    fun read(ctx: ParseContext, index: Int): Offset? {
        if (ctx.word(index) != "in") return null
        val (amount, unitWord, end) = amountAndUnit(ctx, index + 1) ?: return null
        val unit = unitOf(unitWord) ?: return null
        return if (amount > 0) Offset(amount, unit, end) else null
    }

    /** Either one compact word ("2h") or an amount and a unit word ("2 hours", "an hour"). */
    private fun amountAndUnit(ctx: ParseContext, at: Int): Triple<Long, String?, Int>? {
        val first = ctx.word(at) ?: return null
        val compactMatch = compact.matchEntire(first)
        if (compactMatch != null) {
            val (amount, unit) = compactMatch.destructured
            return Triple(amount.toLong(), unit, at + 1)
        }
        val amount = wordAmounts[first] ?: first.takeIf { number.matches(it) }?.toLong() ?: return null
        return Triple(amount, ctx.word(at + 1), at + 2)
    }

    private fun unitOf(word: String?): ChronoUnit? = when (word) {
        in Vocabulary.minuteUnits -> ChronoUnit.MINUTES
        in Vocabulary.hourUnits -> ChronoUnit.HOURS
        in Vocabulary.dayUnits -> ChronoUnit.DAYS
        in Vocabulary.weekUnits -> ChronoUnit.WEEKS
        else -> null
    }
}
