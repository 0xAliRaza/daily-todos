package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aliraza.dailytodos.core.time.DAYS_IN_WEEK
import com.aliraza.dailytodos.core.time.endOfWeek
import com.aliraza.dailytodos.core.time.startOfWeek
import com.aliraza.dailytodos.ui.theme.groupLabel
import com.aliraza.dailytodos.ui.theme.tokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit

/**
 * The whole weeks that cover [month], for weeks that start on [weekStart]: from the start of the week
 * holding the 1st to the end of the week holding the last day, so four to six rows of seven.
 */
fun monthDays(month: YearMonth, weekStart: DayOfWeek): List<LocalDate> {
    val first = month.atDay(1).startOfWeek(weekStart)
    val last = month.atEndOfMonth().endOfWeek(weekStart)
    return (0..ChronoUnit.DAYS.between(first, last)).map { first.plusDays(it) }
}

/** How big a month grid's days are: the height of a week and the day's circle. */
class MonthGridSize(val row: Dp, val circle: Dp)

/** The date sheet's grid: roomy circles, no dots. */
val DatePickSize = MonthGridSize(row = 42.dp, circle = 38.dp)

/**
 * A month to pick a day from, with the weekday letters on top, its weeks starting on [LocalWeekStart].
 * Today wears an accent ring and the [picked] day a solid accent fill. Days of the months around it
 * stay blank, so the month reads as one block.
 */
@Composable
fun MonthGrid(
    month: YearMonth,
    picked: LocalDate?,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        WeekdayLetters()
        MonthWeeks(month, picked, today, onPick, DatePickSize)
    }
}

/** One letter per weekday ("M T W T F S S"), from the app's week start. */
@Composable
fun WeekdayLetters(modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val start = LocalWeekStart.current
    Row(modifier.fillMaxWidth().height(WeekdayHeight)) {
        repeat(DAYS_IN_WEEK.toInt()) { i ->
            Text(
                start.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale),
                style = MaterialTheme.typography.groupLabel,
                color = MaterialTheme.tokens.ink2,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The weeks of [month] as rows of [size]'s height. [dots] gives the small marks under a day, such as
 * the calendar's priority dots; the date sheet has none.
 */
@Composable
fun MonthWeeks(
    month: YearMonth,
    picked: LocalDate?,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
    size: MonthGridSize,
    dots: (LocalDate) -> List<Color> = { emptyList() },
) {
    Column {
        monthDays(month, LocalWeekStart.current).chunked(DAYS_IN_WEEK.toInt()).forEach { week ->
            Row(Modifier.fillMaxWidth().height(size.row)) {
                week.forEach { day ->
                    val cell = Modifier.weight(1f)
                    if (YearMonth.from(day) == month) {
                        DayCell(day, DayLook.of(day, picked, today), size.circle, dots(day), cell) { onPick(day) }
                    } else {
                        Box(cell)
                    }
                }
            }
        }
    }
}

/** How a day in the grid is drawn: picked is solid accent, today an accent ring, past days quieter. */
internal enum class DayLook {
    PICKED,
    TODAY,
    PAST,
    PLAIN,
    ;

    companion object {
        fun of(day: LocalDate, picked: LocalDate?, today: LocalDate): DayLook = when {
            day == picked -> PICKED
            day == today -> TODAY
            day.isBefore(today) -> PAST
            else -> PLAIN
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    look: DayLook,
    circle: Dp,
    dots: List<Color>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.tokens
    val ink = when (look) {
        DayLook.PICKED -> tokens.onAccent
        DayLook.TODAY -> tokens.accent
        DayLook.PAST -> tokens.ink2
        DayLook.PLAIN -> tokens.ink
    }
    val label = longDateLabel(day)
    Column(
        modifier.clip(CircleShape)
            .selectable(selected = look == DayLook.PICKED, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(top = DayTopPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(circle).clip(CircleShape)
                .background(if (look == DayLook.PICKED) tokens.accent else Color.Transparent)
                .border(RingWidth, if (look == DayLook.TODAY) tokens.accent else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (look == DayLook.PLAIN || look == DayLook.PAST) FontWeight.Medium else FontWeight.Bold,
                color = ink,
            )
        }
        if (dots.isNotEmpty()) {
            Row(Modifier.padding(top = DotGap), horizontalArrangement = Arrangement.spacedBy(DotGap)) {
                dots.forEach { Box(Modifier.size(DotSize).clip(CircleShape).background(it)) }
            }
        }
    }
}

private val WeekdayHeight = 28.dp
private val DayTopPadding = 2.dp
private val RingWidth = 2.dp
private val DotSize = 6.dp
private val DotGap = 3.dp
