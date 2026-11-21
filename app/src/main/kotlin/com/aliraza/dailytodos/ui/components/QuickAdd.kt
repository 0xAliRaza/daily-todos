package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import com.aliraza.dailytodos.core.model.ClockFormat
import com.aliraza.dailytodos.core.model.Priority
import com.aliraza.dailytodos.core.parse.ParseResult
import com.aliraza.dailytodos.core.parse.QuickAddWords
import com.aliraza.dailytodos.core.parse.Span
import com.aliraza.dailytodos.core.parse.SpanKind
import com.aliraza.dailytodos.ui.theme.tokens
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

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

    /** A date sheet or menu is open over the bar; it takes the keyboard but mustn't close the bar. */
    var picking by mutableStateOf(false)

    /** The words a fresh line starts with, from where the bar was opened; see [show]. */
    private var start = ""

    /** The line holds the start words and nothing typed. */
    private val onlyStart: Boolean get() = start.isNotEmpty() && text.trim() == start

    /**
     * The line read by [parse], whose last argument says the line is untitled. While it holds only the
     * start words it is: they are highlighted and fill the chips like any recognised words, and with no
     * title of their own there is nothing to add.
     */
    fun read(excluded: Set<IntRange>, parse: (String, Set<IntRange>, Boolean) -> ParseResult): ParseResult =
        parse(text, excluded, onlyStart)

    /**
     * Opens the bar. [words] say where it was opened from (the open list's tag, the picked day), and
     * every fresh line starts with them, after a space and with the cursor before it, so a typed title
     * goes first. A draft of the user's own is kept; a line holding only earlier start words is replaced.
     */
    fun show(words: String = "") {
        val untouched = text.isBlank() || onlyStart
        start = words
        open = true
        if (untouched) fresh()
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

    /**
     * "No date" from the date sheet: removes the typed date and time [words] and keeps the default date
     * out, so the task is added undated. With no typed date it only switches the default off.
     */
    fun noDate(words: List<IntRange>) {
        if (words.isNotEmpty()) put("", words)
        defaultDateOff = true
    }

    /** Ends a pick and starts the next line afresh, with the same start words. */
    fun clear() {
        fresh()
        picking = false
    }

    private fun fresh() {
        value = startLine(start)
        excluded = emptyMap()
        defaultDateOff = false
    }
}

/** A line holding only [words], after a space and with the cursor before it; empty for no words. */
private fun startLine(words: String): TextFieldValue =
    if (words.isEmpty()) TextFieldValue("") else TextFieldValue(" $words", TextRange.Zero)

/**
 * The words a line starts with when the bar opens: the tag of the list screen it opened on, and the
 * picked [date] of Upcoming or the calendar, both as a pick from the chips would write them. [listName]
 * is null on the default list, whose tasks need no tag. A [date] equal to [defaultDay], which the
 * default-day chip offers anyway, is left to that chip.
 */
fun startWords(listName: String?, date: LocalDate?, today: LocalDate, defaultDay: LocalDate?): String = listOfNotNull(
    listName?.let(QuickAddWords::list),
    date?.takeIf { it != defaultDay }?.let { QuickAddWords.date(it, today) },
).joinToString(" ")

/** The default date a task would get on adding, shown in the default-day chip so it can be tapped away. */
data class DefaultDate(val date: LocalDate, val time: LocalTime?)

/**
 * When the task will be due, as the chips show it. [on] is false for a date the user switched off: the
 * default-day chip stays, unset, so one tap brings it back. [words] are the typed words behind it (as
 * excluded ranges when off); empty for the default date.
 */
data class Due(val date: LocalDate, val time: LocalTime?, val on: Boolean, val words: List<IntRange>) {
    /** The default date, not one typed. Only it gets the default-day chip; a typed date fills the Date chip. */
    val isDefault: Boolean get() = words.isEmpty()
}

/** Typed date and time words the user switched off. While there are any, no default date steps in. */
fun switchedOffWhen(all: ParseResult, excluded: Set<IntRange>): List<IntRange> =
    excluded.filter { off -> all.spans.any { it.kind in WHEN_KINDS && it.range.overlaps(off) } }

private fun IntRange.overlaps(other: IntRange): Boolean = first <= other.last && other.first <= last

/**
 * Picks when the task is due. A date read from the text wins. Next comes a typed date the user switched off,
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
 * What the Date chip says about a date read from the line: "Fri 27 Nov, 8:00 PM", or the day alone.
 * Null while no typed date is switched on; the chip then just offers a date, and the default date has
 * a chip of its own.
 */
internal fun dateChipLabel(
    due: Due?,
    today: LocalDate,
    clock: ClockFormat,
    locale: Locale = Locale.getDefault(),
): String? = due?.takeIf { it.on && !it.isDefault }?.let { typed ->
    val time = typed.time?.let { formatTime(it, clock, locale) }
    listOfNotNull(sectionDate(typed.date, today), time).joinToString(", ")
}

/**
 * The add bar: the line, which grows to three lines before it scrolls, and send; under it the chips.
 * Words the parser read are highlighted in place, and tapping one un-recognises it. The chips show what
 * was understood, and a pick from them writes the same words into the line.
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
    val tokens = MaterialTheme.tokens
    // The bar opens from the + button, so it comes with the keyboard, and gets it back after a picker.
    val requester = remember { FocusRequester() }
    LaunchedEffect(state.picking) { if (!state.picking) requester.requestFocus() }
    val shape = RoundedCornerShape(topStart = BAR_CORNER, topEnd = BAR_CORNER)
    Column(modifier.fillMaxWidth().keepsTyping(requester)) {
        // The list fades out under the bar, which sets the bar apart and widens what counts as a tap on it.
        Spacer(
            Modifier.fillMaxWidth().height(FADE_HEIGHT)
                .background(Brush.verticalGradient(listOf(Color.Transparent, tokens.background))),
        )
        Column(Modifier.fillMaxWidth().shadow(BAR_ELEVATION, shape).clip(shape).background(tokens.sheet)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, top = 10.dp, end = 12.dp, bottom = 2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                LineField(state, parsed, due, placeholder, requester, onSubmit, Modifier.weight(1f))
                // Words alone, typed or started with, make no task, so there is nothing to send yet.
                if (parsed.title.isNotBlank()) SendButton(onSubmit)
            }
            AddOptionsRow(state, parsed, due, today, options)
        }
    }
    // A picker open when the bar goes away (say, a notification opens another screen) mustn't stay "open".
    DisposableEffect(state) { onDispose { state.picking = false } }
}

private val FADE_HEIGHT = 24.dp
private val BAR_CORNER = 28.dp
private val BAR_ELEVATION = 6.dp

/** As tall as the send button, so a one-line field sits level with it. */
private val FIELD_MIN_HEIGHT = 48.dp

/** The line grows this many lines before it scrolls, so the title stays in view. */
private const val MAX_LINES = 3

/** The line's text: a touch larger than body text, since it is what the bar is for. */
@Composable
private fun lineStyle(): TextStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp)

/**
 * Taps on the bar that no control inside takes (the gaps, the fade) put the cursor back in the line.
 * Without it they would reach the list behind, whose keyboard guard closes the bar.
 */
private fun Modifier.keepsTyping(requester: FocusRequester): Modifier =
    pointerInput(requester) { detectTapGestures { requester.requestFocus() } }

/**
 * The typed line. It never scrolls by itself: the box around it does once it passes [MAX_LINES], so the
 * highlights drawn behind the words always sit under them.
 */
@Composable
private fun LineField(
    state: QuickAddState,
    parsed: ParseResult,
    due: Due?,
    placeholder: String,
    requester: FocusRequester,
    onSubmit: () -> Unit,
    modifier: Modifier,
) {
    val tokens = MaterialTheme.tokens
    val style = lineStyle()
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val most = maxOf(lineHeight * MAX_LINES, FIELD_MIN_HEIGHT)
    Box(modifier.heightIn(max = most).verticalScroll(rememberScrollState())) {
        Box(Modifier.heightIn(min = FIELD_MIN_HEIGHT), contentAlignment = Alignment.CenterStart) {
            if (state.text.isEmpty()) {
                Text(placeholder, style = style, color = tokens.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = state.value,
                // A pasted line break would make a second line the parser never reads, so it becomes a space.
                onValueChange = { next ->
                    val flat = next.copy(text = next.text.replace('\n', ' '))
                    state.edit(flat, parsed.spans.map(Span::range), due?.words.orEmpty())
                },
                textStyle = style.copy(color = tokens.ink),
                cursorBrush = SolidColor(tokens.accent),
                visualTransformation = SpanHighlight(
                    parsed.spans,
                    SpanStyle(color = tokens.onAccentContainer, fontWeight = FontWeight.Medium),
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                onTextLayout = { layout = it },
                modifier = Modifier.fillMaxWidth().focusRequester(requester)
                    .onFocusChanged { state.focusChanged(it.isFocused) }
                    .drawBehind { layout?.let { drawTokens(it, parsed.spans, tokens.highlight) } },
            )
        }
    }
}

/**
 * What the add bar's pickers offer: every list by name, and the list the task goes to as things stand
 * (a typed tag, else the open list, else the default), shown on the list chip.
 */
data class AddOptions(val lists: List<String>, val target: String)

/**
 * The default day (when no date is typed), date, priority and list. A pick writes the same words the
 * line would have had typed ("tomorrow", "p1", "#groceries") in place of any that said the same, so it is
 * highlighted and can be tapped off like typed words, and the parser stays the one place a line is read.
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
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (due != null && due.isDefault && parsed.title.isNotBlank()) {
            AppChip(dayLabel(due.date, today), state::toggleDefaultDate, icon = AppIcons.Today, set = due.on)
        }
        DateChip(due, today, onOpen = { open(AddMenu.DATE) }, onClear = { state.noDate(due?.words.orEmpty()) })
        Box {
            val level = parsed.priority.takeIf { it != Priority.NONE }
            AppChip(
                label = level?.let { stringResource(R.string.priority_short, it.level) }
                    ?: stringResource(R.string.add_option_priority),
                onClick = { open(AddMenu.PRIORITY) },
                icon = AppIcons.Flag,
                set = level != null,
                iconTint = level?.let { priorityColor(it) },
            )
            PriorityMenu(menu == AddMenu.PRIORITY, close) { picked ->
                state.put(QuickAddWords.priority(picked).orEmpty(), spans(SpanKind.PRIORITY))
            }
        }
        Box {
            AppChip(options.target, { open(AddMenu.LIST) }, icon = AppIcons.Lists, set = parsed.listName != null)
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
        DueDateSheet(
            title = stringResource(R.string.due_date_title),
            current = DueChoice(due?.takeIf { it.on }?.date, parsed.time),
            today = today,
            onPick = { choice -> state.putDue(choice, spans(*WHEN_KINDS.toTypedArray()), due, today) },
            onDismiss = close,
        )
    }
}

/** The Date chip: the typed date and time once read, with × to take them out of the line. */
@Composable
private fun DateChip(due: Due?, today: LocalDate, onOpen: () -> Unit, onClear: () -> Unit) {
    val label = dateChipLabel(due, today, LocalClockFormat.current)
    AppChip(
        label = label ?: stringResource(R.string.add_option_date),
        onClick = onOpen,
        icon = AppIcons.Calendar,
        set = label != null,
        onClear = onClear.takeIf { label != null },
        clearLabel = stringResource(R.string.add_chip_clear_date),
    )
}

/**
 * Writes a picked date and time into the line in place of the date and time words [read] from it. No
 * date takes the typed words out and keeps the default date out too, so the task is added undated.
 */
private fun QuickAddState.putDue(choice: DueChoice, read: List<IntRange>, due: Due?, today: LocalDate) {
    val date = choice.date ?: return noDate(due?.words.orEmpty())
    val words = listOfNotNull(QuickAddWords.date(date, today), choice.time?.let(QuickAddWords::time))
    put(words.joinToString(" "), read)
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
    val tokens = MaterialTheme.tokens
    Box(
        modifier = Modifier
            .padding(start = 10.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(tokens.accent)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.ArrowUp,
            contentDescription = stringResource(R.string.action_add),
            tint = tokens.onAccent,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Colours the recognised words; the text itself is unchanged, so offsets map one to one. */
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

/** The rounded tint behind each recognised word, drawn line by line so a wrapped one is tinted on both. */
private fun DrawScope.drawTokens(layout: TextLayoutResult, spans: List<Span>, color: Color) {
    val padX = TOKEN_PAD_X.toPx()
    val insetY = TOKEN_INSET_Y.toPx()
    val radius = CornerRadius(TOKEN_RADIUS.toPx())
    val length = layout.layoutInput.text.length
    spans.filter { it.range.last < length }.forEach { span ->
        tokenBoxes(layout, span.range).forEach { box ->
            drawRoundRect(
                color,
                topLeft = Offset(box.left - padX, box.top + insetY),
                size = Size(box.width + padX * 2, box.height - insetY * 2),
                cornerRadius = radius,
            )
        }
    }
}

/** The box [range] covers on each line it runs over, without the trailing space a wrap leaves. */
private fun tokenBoxes(layout: TextLayoutResult, range: IntRange): List<Rect> {
    val first = layout.getLineForOffset(range.first)
    val last = layout.getLineForOffset(range.last)
    return (first..last).map { line ->
        val start = if (line == first) range.first else layout.getLineStart(line)
        val lineEnd = layout.getLineEnd(line, visibleEnd = true) - 1
        val end = if (line == last) range.last else lineEnd.coerceAtLeast(start)
        val a = layout.getBoundingBox(start)
        val b = layout.getBoundingBox(end)
        Rect(minOf(a.left, b.left), layout.getLineTop(line), maxOf(a.right, b.right), layout.getLineBottom(line))
    }
}

private val TOKEN_PAD_X = 3.dp
private val TOKEN_INSET_Y = 2.dp
private val TOKEN_RADIUS = 4.dp
