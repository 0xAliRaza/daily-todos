package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliraza.dailytodos.R
import java.time.LocalDate

/**
 * The one sheet for picking a due date: the quick days, a date picker, and "No date" while there is a
 * [current] date to remove. Each day shows its weekday and its due colour, so "Next week" reads as "Mon".
 * Used to reschedule, by the add bar's Date chip and by a task's date row.
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
    var picking by remember { mutableStateOf(false) }
    val pick = { date: LocalDate? ->
        onPick(date)
        onDismiss()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        quickDates(today, LocalWeekStart.current).forEach { quick ->
            val ink = toneColor(dateTone(quick.date, today))
            DayOption(AppIcons.Today, stringResource(quick.label), weekdayShort(quick.date), ink) { pick(quick.date) }
        }
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        DayOption(AppIcons.Calendar, stringResource(R.string.date_pick), null, muted) { picking = true }
        if (current != null) {
            DayOption(AppIcons.Close, stringResource(R.string.task_no_date), null, muted) { pick(null) }
        }
        Spacer(Modifier.navigationBarsPadding().padding(bottom = 12.dp))
    }
    if (picking) {
        DateDialog(current ?: today, onDismiss = { picking = false }) { pick(it) }
    }
}

@Composable
private fun DayOption(icon: ImageVector, label: String, weekday: String?, ink: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 52.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        weekday?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
