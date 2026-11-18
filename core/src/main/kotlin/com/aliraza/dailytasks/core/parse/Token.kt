package com.aliraza.dailytasks.core.parse

/**
 * One whitespace-separated word of the input.
 *
 * [start] and [end] (exclusive) cover the raw text including punctuation, which is what gets removed from
 * the title. [word] is the lower-cased word without surrounding punctuation, which is what matchers read;
 * [wordStart] and [wordEnd] (exclusive) locate it for highlighting.
 */
internal data class Token(
    val raw: String,
    val start: Int,
    val end: Int,
    val word: String,
    val wordStart: Int,
    val wordEnd: Int,
) {
    /** Starts with a capital letter, which mid-sentence marks a proper noun ("Read Tomorrow book"). */
    val isCapitalised: Boolean get() = raw.firstOrNull { it.isLetter() }?.isUpperCase() == true
}

internal object Tokenizer {
    private val wordPattern = Regex("\\S+")
    private const val LEADING = "(\"'"
    private const val TRAILING = ",;:!?.)\"'"

    fun tokenize(text: String): List<Token> = wordPattern.findAll(text).map { match ->
        val raw = match.value
        val lead = raw.takeWhile { it in LEADING }.length
        val trail = raw.drop(lead).takeLastWhile { it in TRAILING }.length
        val core = raw.substring(lead, raw.length - trail)
        val start = match.range.first
        Token(
            raw = raw,
            start = start,
            end = match.range.last + 1,
            word = core.lowercase(),
            wordStart = start + lead,
            wordEnd = start + raw.length - trail,
        )
    }.toList()
}
