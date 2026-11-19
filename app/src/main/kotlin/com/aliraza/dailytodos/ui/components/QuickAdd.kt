package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.Priority
import com.aliraza.dailytodos.core.parse.ParseResult
import com.aliraza.dailytodos.core.parse.QuickAddWords
import com.aliraza.dailytodos.core.parse.Span
import com.aliraza.dailytodos.core.parse.SpanKind
import com.aliraza.dailytodos.ui.theme.tokens
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

    /** A date dialog or menu is open over the bar; it takes the keyboard but mustn't close the bar. */
    var picking by mutableStateOf(false)

    fun show() {
        open = true
    }

    /** Called as the field gains or loses focus. Only losing it closes the bar, not the unfocused start. */
    fun focusChanged(isFocused: Boolean) {
        if (focused && !isFocused && !picking) open = false
        focused = isFocused
    }

    /**
     * Writes [words] picked by hand into the line, in place of the [replacing] ranges that said the same
     * kind of thing, and puts the cursor after them so typing carries on.
     */
    fun put(words: String, replacing: List<IntRange>) {
        val (line, moved) = squeeze(text, replacing)
        val next = listOf(
            line,
            words,
        ).filter { it.isNotEmpty() }.joinToString(" ").let { if (it.isEmpty()) it else "$it " }
        value = TextFieldValue(next, TextRange(next.length))
        // Words switched off stay off at their new place; one that was removed or changed is forgotten.
        excluded = excluded.mapNotNull { (range, word) ->
            val start = moved.getOrNull(range.first) ?: -1
            val end = moved.getOrNull(range.last) ?: -1
            val at = start..end
            (at to word).takeIf { start >= 0 && end >= start && next.length > end && next.substring(at) == word }
        }.toMap()
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

    /** Switches the default date off, or back on. A typed date is switched by tapping its words instead. */
    fun toggleDefaultDate() {
        defaultDateOff = !defaultDateOff
    }

    fun clear() {
        value = TextFieldValue("")
        excluded = emptyMap()
        defaultDateOff = false
        picking = false
    }
}

/** The default date a task would get on adding, shown in the date pill so it can be tapped away. */
data class DefaultDate(val date: LocalDate, val time: LocalTime?)

/**
 * What the due pill shows. [on] is false for a date the user switched off: it stays in the pill, greyed,
 * so one tap brings it back. [words] are the typed words behind it (as excluded ranges when off); empty
 * for the default date.
 */
data class Due(val date: LocalDate, val time: LocalTime?, val on: Boolean, val words: List<IntRange>) {
    /** The default date, not one typed. Only it gets the pill; a typed date shows as highlighted words. */
    val isDefault: Boolean get() = words.isEmpty()
}

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
 *
 * The bar is forgiving to touch: a tap anywhere on it, or on the soft fade just above it, gives the
 * keyboard back to the line instead of falling through to the list behind and closing the bar.
 */
@Composable
fun QuickAddBar(
    state: QuickAddState,
    parsed: ParseResult,
    due: Due?,
    today: LocalDate,
    placeholder: String,
    options: AddOptions,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // The bar opens from the + button, so it comes with the keyboard, and gets it back after a picker.
    val requester = remember { FocusRequester() }
    LaunchedEffect(state.picking) { if (!state.picking) requester.requestFocus() }
    val shape = RoundedCornerShape(BAR_CORNER)
    Column(modifier.fillMaxWidth().keepsTyping(requester)) {
        // The list fades out under the bar, which sets the bar apart and widens what counts as a tap on it.
        Spacer(
            Modifier.fillMaxWidth().height(FADE_HEIGHT)
                .background(Brush.verticalGradient(listOf(Color.Transparent, scheme.background))),
        )
        Column(
            Modifier.fillMaxWidth().background(scheme.background).padding(start = 12.dp, end = 12.dp, bottom = 6.dp)
                .shadow(
                    BAR_ELEVATION,
                    shape,
                    ambientColor = MaterialTheme.tokens.accent,
                    spotColor = MaterialTheme.tokens.accent,
                )
                .clip(shape)
                .background(scheme.surfaceVariant),
        ) {
            Row(
                Modifier.heightIn(min = 54.dp).padding(start = 14.dp, end = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { InputRow(state, parsed, due, today, placeholder, requester, onSubmit) }
            AddOptionsRow(state, parsed, due, today, options)
        }
    }
    // A picker open when the bar goes away (say, a notification opens another screen) mustn't stay "open".
    DisposableEffect(state) { onDispose { state.picking = false } }
}

private val FADE_HEIGHT = 24.dp
private val BAR_CORNER = 28.dp
private val BAR_ELEVATION = 6.dp

/** The line's text: a touch larger than body text, since it is what the bar is for. */
@Composable
private fun lineStyle(): TextStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp)

/**
 * Taps on the bar that no control inside takes (the plus, the gaps, the fade) put the cursor back in the
 * line. Without it they would reach the list behind, whose keyboard guard closes the bar.
 */
private fun Modifier.keepsTyping(requester: FocusRequester): Modifier =
    pointerInput(requester) { detectTapGestures { requester.requestFocus() } }

@Composable
private fun RowScope.InputRow(
    state: QuickAddState,
    parsed: ParseResult,
    due: Due?,
    today: LocalDate,
    placeholder: String,
    requester: FocusRequester,
    onSubmit: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Icon(AppIcons.Add, null, tint = scheme.primary, modifier = Modifier.size(24.dp))
    Spacer(Modifier.width(10.dp))
    Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
        if (state.text.isEmpty()) {
            Text(
                placeholder,
                style = lineStyle(),
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicTextField(
            value = state.value,
            onValueChange = { state.edit(it, parsed.spans.map(Span::range), tied = due?.words.orEmpty()) },
            textStyle = lineStyle().copy(color = scheme.onSurface),
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
    if (due != null && due.isDefault && state.text.isNotBlank()) DuePill(due, today, state::toggleDefaultDate)
    if (state.text.isNotBlank()) SendButton(onSubmit)
}

/**
 * What the add bar's pickers offer: every list by name, and the list the task goes to as things stand
 * (a typed tag, else the open list, else the default), shown on the list chip.
 */
data class AddOptions(val lists: List<String>, val target: String)

/**
 * Date, priority and list, picked by hand. A pick writes the same words the line would have had typed
 * ("tomorrow", "p1", "#groceries") in place of any that said the same, so it is highlighted and can be
 * tapped off like typed words, and the parser stays the one place a line is read.
 */
@Composable
private fun AddOptionsRow(state: QuickAddState, parsed: ParseResult, due: Due?, today: LocalDate, options: AddOptions) {
    var menu by remember { mutableStateOf<AddMenu?>(null) }
    val open = { next: AddMenu ->
        state.picking = true
        menu = next
    }
    val close = {
        menu = null
        state.picking = false
    }
    fun spans(vararg kinds: SpanKind) = parsed.spans.filter { it.kind in kinds }.map(Span::range)
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // The pill in the line shows the date; this chip only picks one.
        OptionChip(AppIcons.Today, stringResource(R.string.add_option_date)) { open(AddMenu.DATE) }
        Box {
            val level = parsed.priority.takeIf { it != Priority.NONE }
            val label = level?.let { "P${it.level}" } ?: stringResource(R.string.add_option_priority)
            OptionChip(AppIcons.Flag, label, tint = level?.let { priorityColor(it) }) { open(AddMenu.PRIORITY) }
            PriorityMenu(menu == AddMenu.PRIORITY, close) { picked ->
                state.put(QuickAddWords.priority(picked).orEmpty(), spans(SpanKind.PRIORITY))
            }
        }
        Box {
            OptionChip(AppIcons.Lists, options.target) { open(AddMenu.LIST) }
            DropdownMenu(menu == AddMenu.LIST, onDismissRequest = close) {
                options.lists.forEach { name ->
                    DropdownMenuItem(text = { Text(name) }, onClick = {
                        QuickAddWords.list(name)?.let { state.put(it, spans(SpanKind.LIST)) }
                        close()
                    })
                }
            }
        }
    }
    if (menu == AddMenu.DATE) {
        DateDialog(due?.date ?: today, onDismiss = close) { day ->
            // A typed "tomorrow 4pm" is one span; picking another day keeps its time.
            val time = parsed.time?.takeIf { parsed.spans.any { it.kind == SpanKind.DATE_TIME } }
            val words = listOfNotNull(QuickAddWords.date(day, today), time?.let(QuickAddWords::time))
            state.put(words.joinToString(" "), spans(SpanKind.DATE, SpanKind.DATE_TIME))
        }
    }
}

private enum class AddMenu { DATE, PRIORITY, LIST }

@Composable
private fun PriorityMenu(expanded: Boolean, onClose: () -> Unit, onPick: (Priority) -> Unit) {
    DropdownMenu(expanded, onDismissRequest = onClose) {
        listOf(Priority.P1, Priority.P2, Priority.P3, Priority.NONE).forEach { level ->
            val label = if (level == Priority.NONE) {
                stringResource(R.string.priority_none)
            } else {
                stringResource(R.string.priority_long, level.level)
            }
            DropdownMenuItem(
                text = { Text(label) },
                leadingIcon = { Icon(AppIcons.Flag, null, tint = priorityColor(level)) },
                onClick = {
                    onPick(level)
                    onClose()
                },
            )
        }
    }
}

/** A small, quiet button under the line; [tint] colours a value that is set (a priority's flag). */
@Composable
private fun OptionChip(icon: ImageVector, label: String, tint: Color? = null, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 36.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint ?: scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, maxLines = 1)
    }
}

/**
 * The default date a task with no typed date gets. On, it is filled with the accent so it reads as set;
 * tapped off, an empty grey outline; tapped again, on.
 */
@Composable
private fun DuePill(due: Due, today: LocalDate, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    // The day alone keeps the pill short; a typed time is already highlighted in the text.
    val label = dayLabel(due.date, today)
    val full = listOfNotNull(label, due.time?.let { timeLabel(it) }).joinToString(" ")
    val tokens = MaterialTheme.tokens
    val ink = if (due.on) tokens.onAccentContainer else scheme.onSurfaceVariant
    val action = stringResource(if (due.on) R.string.action_unrecognise else R.string.action_recognise, full)
    Row(
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(CircleShape)
            .background(if (due.on) tokens.accentContainer else Color.Transparent)
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

/**
 * [text] without the [drop] ranges, runs of spaces made one and the ends trimmed, plus where each old
 * character landed in the result (-1 for one that is gone).
 */
internal fun squeeze(text: String, drop: List<IntRange>): Pair<String, IntArray> {
    val out = StringBuilder()
    val moved = IntArray(text.length) { -1 }
    var afterSpace = true
    text.forEachIndexed { i, c ->
        when {
            drop.any { i in it } -> Unit

            c.isWhitespace() -> {
                if (!afterSpace) out.append(' ')
                afterSpace = true
            }

            else -> {
                moved[i] = out.length
                out.append(c)
                afterSpace = false
            }
        }
    }
    return out.toString().trimEnd() to moved
}

@Composable
private fun SendButton(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(scheme.primary)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.Send,
            contentDescription = stringResource(R.string.action_add),
            tint = scheme.onPrimary,
            modifier = Modifier.size(20.dp),
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
