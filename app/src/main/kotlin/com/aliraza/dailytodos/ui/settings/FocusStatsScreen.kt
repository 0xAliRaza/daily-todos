package com.aliraza.dailytodos.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.focus.DayFocus
import com.aliraza.dailytodos.core.focus.FocusStats
import com.aliraza.dailytodos.core.focus.FunComparison
import com.aliraza.dailytodos.core.focus.FunFact
import com.aliraza.dailytodos.core.focus.FunFacts
import com.aliraza.dailytodos.core.focus.TaskFocus
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.EmptyState
import com.aliraza.dailytodos.ui.components.LocalWeekStart
import com.aliraza.dailytodos.ui.components.weekdayShort
import com.aliraza.dailytodos.ui.focus.FocusViewModel
import com.aliraza.dailytodos.ui.focus.cardSurface
import com.aliraza.dailytodos.ui.focus.focusDuration
import com.aliraza.dailytodos.ui.theme.tokens

/** Days before today in the chart are drawn softer, so today's bar reads first. */
private const val PAST_ALPHA = 0.45f

/** Focus time, plainly counted: totals, the last seven days, the tasks it went to, and a comparison. */
@Composable
fun FocusStatsScreen(viewModel: FocusViewModel, padding: PaddingValues) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    if (stats.sessions == 0) {
        EmptyState(
            stringResource(R.string.stats_empty_title),
            stringResource(R.string.stats_empty_body),
            Modifier.padding(padding),
            icon = AppIcons.Timer,
        )
        return
    }
    val comparison = remember(stats.total) { FunFacts.pick(stats.total, viewModel.today()) }
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Hero(stats)
        Tiles(stats)
        Card { Week(stats.lastDays) }
        comparison?.let { Card { Comparison(it) } }
        Card { TopTasks(stats.topTasks) }
    }
}

@Composable
private fun Hero(stats: FocusStats) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.stats_all_time),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(focusDuration(stats.total), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun Tiles(stats: FocusStats) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(stringResource(R.string.stats_today), focusDuration(stats.today))
        Tile(stringResource(R.string.stats_week), focusDuration(stats.thisWeek(LocalWeekStart.current)))
        Tile(stringResource(R.string.stats_sessions), stats.sessions.toString())
    }
}

@Composable
private fun RowScope.Tile(label: String, value: String) {
    Column(
        Modifier.weight(1f).cardSurface(RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().cardSurface(RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun CardTitle(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

/** Seven bars, oldest first; today's is solid. The busiest day sets the height. */
@Composable
private fun Week(days: List<DayFocus>) {
    CardTitle(R.string.stats_last_days)
    val tokens = MaterialTheme.tokens
    val most = days.maxOf { it.focused.toMillis() }.coerceAtLeast(1)
    val summary = stringResource(R.string.stats_last_days)
    Canvas(Modifier.fillMaxWidth().height(96.dp).semantics { contentDescription = summary }) {
        val slot = size.width / days.size
        val bar = slot * BAR_SHARE
        days.forEachIndexed { i, day ->
            val share = day.focused.toMillis().toFloat() / most
            val height = (size.height * share).coerceAtLeast(MIN_BAR.toPx())
            val today = i == days.lastIndex
            drawRoundRect(
                color = barColor(day, today, tokens.divider, tokens.accent),
                topLeft = Offset(slot * i + (slot - bar) / 2, size.height - height),
                size = Size(bar, height),
                cornerRadius = CornerRadius(bar / 2, bar / 2),
            )
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        days.forEach { day ->
            Text(
                weekdayShort(day.day),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Empty days show a stub in the hairline colour; past days are softer than today. */
private fun barColor(day: DayFocus, today: Boolean, empty: Color, accent: Color): Color = when {
    day.focused.isZero -> empty
    today -> accent
    else -> accent.copy(alpha = PAST_ALPHA)
}

private const val BAR_SHARE = 0.5f
private val MIN_BAR = 3.dp

/** "That's as long as 72 journeys of sunlight to Earth", with the figure it rests on. */
@Composable
private fun Comparison(comparison: FunComparison) {
    val count = comparison.count.toInt()
    Text(
        stringResource(R.string.stats_fact_intro),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(count.toString(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.tokens.accentText)
    }
    Text(pluralStringResource(factName(comparison.fact), count, count), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
    Text(
        stringResource(factNote(comparison.fact)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun factName(fact: FunFact): Int = when (fact) {
    FunFact.MOON_LASER -> R.plurals.fact_moon_laser
    FunFact.SUNLIGHT -> R.plurals.fact_sunlight
    FunFact.ISS_ORBIT -> R.plurals.fact_iss_orbit
    FunFact.MAYFLY -> R.plurals.fact_mayfly
    FunFact.APOLLO_11 -> R.plurals.fact_apollo_11
    FunFact.MOON_ORBIT -> R.plurals.fact_moon_orbit
}

private fun factNote(fact: FunFact): Int = when (fact) {
    FunFact.MOON_LASER -> R.string.fact_moon_laser_note
    FunFact.SUNLIGHT -> R.string.fact_sunlight_note
    FunFact.ISS_ORBIT -> R.string.fact_iss_orbit_note
    FunFact.MAYFLY -> R.string.fact_mayfly_note
    FunFact.APOLLO_11 -> R.string.fact_apollo_11_note
    FunFact.MOON_ORBIT -> R.string.fact_moon_orbit_note
}

@Composable
private fun TopTasks(tasks: List<TaskFocus>) {
    CardTitle(R.string.stats_top)
    val scheme = MaterialTheme.colorScheme
    tasks.forEach { task ->
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                task.title ?: stringResource(R.string.stats_deleted),
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.title == null) scheme.onSurfaceVariant else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Text(
                focusDuration(task.focused),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
