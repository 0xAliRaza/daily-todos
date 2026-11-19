package com.aliraza.dailytodos.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.aliraza.dailytodos.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.IllformedLocaleException
import java.util.Locale

/**
 * The system date picker in a dialog, its weeks starting on the app's week start. The picker counts in
 * UTC midnights, so no zone shifts the day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val base = LocalConfiguration.current.locales[0]
    val weekStart = LocalWeekStart.current
    val state = remember(base, weekStart) {
        DatePickerState(
            locale = base.withFirstDay(weekStart),
            initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
    }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) { DatePicker(state) }
}

/**
 * This locale with its weeks starting on [day], through the Unicode "fw" keyword. The picker takes its
 * first weekday from `WeekFields.of(locale)`, which reads the keyword on Android 13 and later; older
 * versions keep the locale's own first day. A locale the builder can't take is returned unchanged.
 */
internal fun Locale.withFirstDay(day: DayOfWeek): Locale = try {
    val code = day.name.take(FW_LENGTH).lowercase(Locale.ROOT)
    Locale.Builder().setLocale(this).setUnicodeLocaleKeyword("fw", code).build()
} catch (_: IllformedLocaleException) {
    this
}

/** A "fw" value is the day's English name cut to three letters: "mon", "sun". */
private const val FW_LENGTH = 3
