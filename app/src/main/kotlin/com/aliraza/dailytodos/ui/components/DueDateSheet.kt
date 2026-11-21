package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.ui.theme.body
import com.aliraza.dailytodos.ui.theme.meta
import com.aliraza.dailytodos.ui.theme.sheetTitle
import com.aliraza.dailytodos.ui.theme.tokens
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * The one sheet for picking a due day: the quick days, No date and a month, with the same content the
 * task sheet swaps in. Used to reschedule tasks, where each keeps its own time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DueDateSheet(
    title: String,
    current: LocalDate?,
    today: LocalDate,
    onPick: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    AppSheet(onDismiss) {
        DueDateContent(
            title = title,
            initial = DueChoice(current, null),
            today = today,
            withTime = false,
            onPick = { onPick(it.date) },
            onClose = onDismiss,
        )
    }
}

/** The due date sheet with a Time row as well, as the add bar's Date chip opens it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DueDateSheet(
    title: String,
    current: DueChoice,
    today: LocalDate,
    onPick: (DueChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    AppSheet(onDismiss) {
        DueDateContent(title, current, today, withTime = true, onPick = onPick, onClose = onDismiss)
    }
}

/**
 * Picking a due date in one layer. Today, Tomorrow, Next week and No date apply at once; a day in the
 * month and the time are a draft that Done applies, and [onBack] (when the view sits inside another
 * sheet) drops it. [title] names what is being dated; the headline under it shows the picked day.
 */
@Composable
internal fun DueDateContent(
    title: String,
    initial: DueChoice,
    today: LocalDate,
    withTime: Boolean,
    onPick: (DueChoice) -> Unit,
    onClose: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    val apply = { choice: DueChoice ->
        if (choice != initial) onPick(choice)
        onClose()
    }
    Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        DateHeader(title, draft.date, today, onBack) { apply(draft) }
        quickDates(today, LocalWeekStart.current).forEach { quick ->
            QuickRow(AppIcons.Calendar, stringResource(quick.label), sectionDate(quick.date, today)) {
                apply(draft.copy(date = quick.date))
            }
        }
        QuickRow(AppIcons.NoDate, stringResource(R.string.task_no_date), stringResource(R.string.date_clears)) {
            apply(DueChoice(null, null))
        }
        Box(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp).fillMaxWidth()
                .height(1.dp).background(MaterialTheme.tokens.hairline),
        )
        MonthPicker(draft.date, today) { draft = draft.copy(date = it) }
        if (withTime) {
            TimeLine(
                draft.time,
                onPick = { draft = draft.withTime(it, today) },
                onClear = { draft = draft.withTime(null, today) },
            )
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Back (inside the task sheet), what is being dated over the picked day, and Done. */
@Composable
private fun DateHeader(title: String, date: LocalDate?, today: LocalDate, onBack: (() -> Unit)?, onDone: () -> Unit) {
    val tokens = MaterialTheme.tokens
    Row(
        Modifier.fillMaxWidth()
            .padding(start = if (onBack == null) 20.dp else 8.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        onBack?.let {
            IconButton(onClick = it) {
                Icon(AppIcons.ChevronLeft, stringResource(R.string.action_back), tint = tokens.ink)
            }
            Spacer(Modifier.size(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.meta,
                color = tokens.ink2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                date?.let { sectionDate(it, today) } ?: stringResource(R.string.task_no_date),
                style = MaterialTheme.typography.sheetTitle,
                fontWeight = FontWeight.ExtraBold,
                color = tokens.ink,
            )
        }
        AppButton(stringResource(R.string.date_done), onDone, AppButtons.primary(), Modifier.height(40.dp))
    }
}

@Composable
private fun QuickRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    val tokens = MaterialTheme.tokens
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 48.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = tokens.ink2, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.body, color = tokens.ink, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.body, fontWeight = FontWeight.Medium, color = tokens.ink2)
    }
}

/** The month with its name and arrows; it opens on the picked day's month, else this one. */
@Composable
private fun MonthPicker(picked: LocalDate?, today: LocalDate, onPick: (LocalDate) -> Unit) {
    var month by rememberSaveable { mutableStateOf(YearMonth.from(picked ?: today)) }
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            month.format(MONTH_YEAR),
            style = MaterialTheme.typography.body,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.tokens.ink,
            modifier = Modifier.weight(1f),
        )
        MonthArrow(AppIcons.ChevronLeft, stringResource(R.string.date_previous_month)) { month = month.minusMonths(1) }
        MonthArrow(AppIcons.ChevronRight, stringResource(R.string.date_next_month)) { month = month.plusMonths(1) }
    }
    MonthGrid(month, picked, today, onPick, Modifier.padding(horizontal = 14.dp))
}

@Composable
private fun MonthArrow(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, label, tint = MaterialTheme.tokens.ink, modifier = Modifier.size(18.dp))
    }
}

/** "Time" with the picked time as a chip (tap to change, × to clear), or "Add time". */
@Composable
private fun TimeLine(time: LocalTime?, onPick: (LocalTime) -> Unit, onClear: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val tokens = MaterialTheme.tokens
    Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(1.dp).background(tokens.hairline))
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 20.dp, end = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(AppIcons.Clock, null, tint = tokens.ink2, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.date_time), style = MaterialTheme.typography.body, modifier = Modifier.weight(1f))
        AppChip(
            label = time?.let { timeLabel(it) } ?: stringResource(R.string.date_add_time),
            onClick = { open = true },
            set = time != null,
            onClear = onClear.takeIf { time != null },
            clearLabel = stringResource(R.string.date_clear_time),
        )
    }
    if (open) {
        TimeDialog(time ?: LocalTime.of(DEFAULT_PICK_HOUR, 0), onDismiss = { open = false }, onPick = onPick)
    }
}

/** Where the clock starts when a time is first picked. */
internal const val DEFAULT_PICK_HOUR = 9

private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
