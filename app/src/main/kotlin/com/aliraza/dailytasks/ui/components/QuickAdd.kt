package com.aliraza.dailytasks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliraza.dailytasks.R
import com.aliraza.dailytasks.core.parse.ParseResult
import com.aliraza.dailytasks.core.parse.Span
import com.aliraza.dailytasks.core.parse.SpanKind
import java.time.LocalDate
import java.time.LocalTime

/**
 * A tap that drops the cursor inside a recognised word, without changing the text, means "don't read
 * this word". Returns that word's range. The word's edges don't count, so placing the cursor after a
 * word to keep typing never un-recognises it.
 */
internal fun tappedSpan(before: TextFieldValue, after: TextFieldValue, spans: List<IntRange>): IntRange? {
    if (before.text != after.text || before.selection == after.selection || !after.selection.collapsed) return null
    val at = after.selection.start
    return spans.firstOrNull { at > it.first && at <= it.last }
}

/**
 * What is being typed in the add bar, plus the words the user tapped to un-recognise. An exclusion is
 * kept only while the same text still sits at its place, so editing elsewhere does not drop it.
 */
@Stable
class QuickAddState {
    var value by mutableStateOf(TextFieldValue(""))
        private set
    val text: String get() = value.text
    var excluded by mutableStateOf(emptyMap<IntRange, String>())
        private set
    var defaultDateOff by mutableStateOf(false)
        private set

    /** Whether the add bar has the keyboard; while it does, a tap anywhere else closes it. */
    var focused by mutableStateOf(false)
        private set

    /** Whether the add bar is shown. The + button opens it; losing the keyboard closes it, keeping the draft. */
    var open by mutableStateOf(false)
        private set

    fun show() {
        open = true
    }

    /** Called as the field gains or loses focus. Only losing it closes the bar, not the unfocused start. */
    fun focusChanged(isFocused: Boolean) {
        if (focused && !isFocused) open = false
        focused = isFocused
    }

    /**
     * [recognised] holds the ranges of the words read from the text before this edit. A tap inside one
     * un-recognises it; a tap inside a word un-recognised earlier brings it back. Either way the cursor
     * moves to the word's end, so the next tap on it is a fresh tap. [tied] are the date and time words:
     * they make one moment, so a tap on any of them switches them all.
     */
    fun edit(next: TextFieldValue, recognised: List<IntRange> = emptyList(), tied: List<IntRange> = emptyList()) {
        val on = tappedSpan(value, next, recognised)
        val off = tappedSpan(value, next, excluded.keys.toList())
        value = (on ?: off)?.let { next.copy(selection = TextRange(it.last + 1)) } ?: next
        val typed = next.text
        excluded = excluded.filter { (range, word) -> typed.length > range.last && typed.substring(range) == word }
        if (typed.isBlank()) defaultDateOff = false
        on?.let { if (it in tied) tied.forEach(::exclude) else exclude(it) }
        off?.let { if (it in tied) tied.forEach(::include) else include(it) }
    }

    fun exclude(range: IntRange) {
        if (text.length > range.last) excluded = excluded + (range to text.substring(range))
    }

    fun include(range: IntRange) {
        excluded = excluded.filterKeys { it != range }
    }

    /** Switches the due pill: its words, or the default date, off when on and back on when off. */
    fun toggle(due: Due) {
        when {
            due.words.isEmpty() -> defaultDateOff = due.on
            due.on -> due.words.forEach(::exclude)
            else -> due.words.forEach(::include)
        }
    }

    fun clear() {
        value = TextFieldValue("")
        excluded = emptyMap()
        defaultDateOff = false
    }
}

/** The default date a task would get on adding, shown in the date pill so it can be tapped away. */
data class DefaultDate(val date: LocalDate, val time: LocalTime?)

/**
 * What the due pill shows. [on] is false for a date the user switched off: it stays in the pill, greyed,
 * so one tap brings it back. [words] are the typed words behind it (as excluded ranges when off); empty
 * for the default date.
 */
data class Due(val date: LocalDate, val time: LocalTime?, val on: Boolean, val words: List<IntRange>)

/** Typed date and time words the user switched off. While there are any, no default date steps in. */
fun switchedOffWhen(all: ParseResult, excluded: Set<IntRange>): List<IntRange> =
    excluded.filter { off -> all.spans.any { it.kind in WHEN_KINDS && it.range.overlaps(off) } }

private fun IntRange.overlaps(other: IntRange): Boolean = first <= other.last && other.first <= last

/**
 * Picks the due pill. A date read from the text wins. Next comes a typed date the user switched off,
 * greyed. Last is the default date, on or off. [all] is the same text parsed with nothing excluded, and
 * [default] is the default date as it would be if switched on.
 */
fun due(
    parsed: ParseResult,
    all: ParseResult,
    excluded: Set<IntRange>,
    default: DefaultDate?,
    defaultOff: Boolean,
): Due? {
    val read = parsed.spans.filter { it.kind in WHEN_KINDS }.map(Span::range)
    val off = switchedOffWhen(all, excluded)
    val switchedOff = all.date?.takeIf { off.isNotEmpty() }
    return parsed.date?.let { Due(it, parsed.time, on = true, words = read) }
        ?: switchedOff?.let { Due(it, all.time, on = false, words = off) }
        ?: default?.let { Due(it.date, it.time, on = !defaultOff, words = emptyList()) }
}

/**
 * The one-line add bar: a plus, the text, a pill saying when the task will be due, and send. Words the
 * parser read are highlighted in place; tapping one un-recognises it, and tapping the pill drops the date.
 */
@Composable
fun QuickAddBar(
    state: QuickAddState,
    parsed: ParseResult,
    due: Due?,
    today: LocalDate,
    placeholder: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // The bar opens from the + button, so it comes with the keyboard.
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) { requester.requestFocus() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceVariant)
            .heightIn(min = 48.dp)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Add, null, tint = scheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
            if (state.text.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = state.value,
                onValueChange = { state.edit(it, parsed.spans.map(Span::range), tied = due?.words.orEmpty()) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                visualTransformation = SpanHighlight(
                    parsed.spans,
                    SpanStyle(background = scheme.primaryContainer, color = scheme.secondary),
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(requester)
                    .onFocusChanged { state.focusChanged(it.isFocused) },
            )
        }
        if (due != null && state.text.isNotBlank()) DuePill(due, today) { state.toggle(due) }
        if (state.text.isNotBlank()) SendButton(onSubmit)
    }
}

/** When the task will be due. On, it is a filled chip; tapped off, an empty grey outline; tapped again, on. */
@Composable
private fun DuePill(due: Due, today: LocalDate, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    // The day alone keeps the pill short; a typed time is already highlighted in the text.
    val label = dayLabel(due.date, today)
    val full = listOfNotNull(label, due.time?.let(::timeLabel)).joinToString(" ")
    val ink = if (due.on) toneColor(dateTone(due.date, today)) else scheme.onSurfaceVariant
    val action = stringResource(if (due.on) R.string.action_unrecognise else R.string.action_recognise, full)
    Row(
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(CircleShape)
            .background(if (due.on) scheme.surface else Color.Transparent)
            .border(1.dp, if (due.on) Color.Transparent else scheme.outline, CircleShape)
            .clickable(role = Role.Button, onClickLabel = action, onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Today, null, tint = if (due.on) ink else scheme.outline, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = ink,
            maxLines = 1,
        )
    }
}

private val WHEN_KINDS = setOf(SpanKind.DATE, SpanKind.TIME, SpanKind.DATE_TIME)

@Composable
private fun SendButton(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(scheme.primary)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.Send,
            contentDescription = stringResource(R.string.action_add),
            tint = scheme.onPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Paints the recognised words; the text itself is unchanged, so offsets map one to one. */
private class SpanHighlight(private val spans: List<Span>, private val style: SpanStyle) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val painted = buildAnnotatedString {
            append(text)
            spans.filter { it.range.last < text.length }.forEach { addStyle(style, it.range.first, it.range.last + 1) }
        }
        return TransformedText(painted, OffsetMapping.Identity)
    }

    override fun equals(other: Any?): Boolean = other is SpanHighlight && other.spans == spans && other.style == style

    override fun hashCode(): Int = spans.hashCode() * 31 + style.hashCode()
}
