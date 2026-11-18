package com.aliraza.dailytasks.core.parse

import com.aliraza.dailytasks.core.model.Priority

/** Priority (`p1` to `p3`) and list (`#name`) tags. */
internal object TagMatchers {
    val all: List<Matcher> = listOf(Matcher(::priority), Matcher(::list))

    private fun priority(ctx: ParseContext, index: Int): Candidate? {
        val level = Vocabulary.priorities[ctx.word(index)] ?: return null
        return Candidate(SpanKind.PRIORITY, index, index + 1, priority = Priority.fromLevel(level))
    }

    /** `#groceries` matches the list "Groceries"; `#weekendchores` matches "Weekend chores". */
    private fun list(ctx: ParseContext, index: Int): Candidate? {
        val word = ctx.word(index) ?: return null
        if (!word.startsWith("#") || word.length < 2) return null
        val key = word.drop(1)
        val name = ctx.listNames.firstOrNull { normalise(it) == key } ?: return null
        return Candidate(SpanKind.LIST, index, index + 1, listName = name)
    }

    private fun normalise(name: String) = name.lowercase().filterNot { it.isWhitespace() }
}
