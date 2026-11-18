package com.aliraza.dailytasks.core.parse

import com.aliraza.dailytasks.core.model.Priority

/** Priority (`p1` to `p3`) and list (`#name`) tags. */
internal object TagMatchers {
    val all: List<Matcher> = listOf(Matcher(::priority), Matcher(::list))

    private fun priority(ctx: ParseContext, index: Int): Candidate? {
        val level = Vocabulary.priorities[ctx.word(index)] ?: return null
        return Candidate(SpanKind.PRIORITY, index, index + 1, priority = Priority.fromLevel(level))
    }

    /** A new list's name from its tag: letters, digits, `-` and `_`, with at least one letter. */
    private val newListTag = Regex("^#(?=[\\p{L}\\p{N}_-]*\\p{L})[\\p{L}\\p{N}_-]{1,$MAX_NAME}$")

    /**
     * `#groceries` matches the list "Groceries"; `#weekendchores` matches "Weekend chores". A tag that
     * matches none names a new list, made when the task is added: `#weekend-trip` is "Weekend trip".
     * Numbers alone ("Issue #42") are not tags.
     */
    private fun list(ctx: ParseContext, index: Int): Candidate? {
        val token = ctx.tokens.getOrNull(index) ?: return null
        val word = token.word
        if (!word.startsWith("#") || word.length < 2) return null
        val key = word.drop(1)
        val existing = ctx.listNames.firstOrNull { normalise(it) == key }
        val typed = token.raw.substring(token.wordStart - token.start, token.wordEnd - token.start)
        return when {
            existing != null -> Candidate(SpanKind.LIST, index, index + 1, listName = existing)

            newListTag.matches(typed) ->
                Candidate(SpanKind.LIST, index, index + 1, listName = newListName(typed), newList = true)

            else -> null
        }
    }

    /** "#weekend-trip" to "Weekend trip": separators become spaces, the first letter a capital. */
    private fun newListName(tag: String): String =
        tag.drop(1).replace('-', ' ').replace('_', ' ').trim().replaceFirstChar { it.uppercase() }

    private const val MAX_NAME = 40

    private fun normalise(name: String) = name.lowercase().filterNot { it.isWhitespace() }
}
