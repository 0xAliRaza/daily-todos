package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.ClockFormat
import java.time.LocalTime

/**
 * The app's one time picker: the clock face, or typed hours and minutes after the keyboard toggle, in
 * the app's clock format. [onClear], when given, adds "Remove time" so a set time can be taken away
 * from the same place it was picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit, onClear: (() -> Unit)? = null) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            TimePanel(initial, onDismiss, onPick, onClear)
        }
    }
}

/** The dialog's content: its title, the clock or typed input, and the buttons. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePanel(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit, onClear: (() -> Unit)?) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = LocalClockFormat.current == ClockFormat.H24,
    )
    var typing by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.time_dialog_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        )
        if (typing) TimeInput(state) else TimePicker(state)
        DialogButtons(
            typing = typing,
            onToggle = { typing = !typing },
            onClear = onClear?.let { clear ->
                {
                    clear()
                    onDismiss()
                }
            },
            onDismiss = onDismiss,
            onOk = {
                onPick(LocalTime.of(state.hour, state.minute))
                onDismiss()
            },
        )
    }
}

/** The keyboard or clock toggle and "Remove time" at the left; Cancel and OK at the right. */
@Composable
private fun DialogButtons(
    typing: Boolean,
    onToggle: () -> Unit,
    onClear: (() -> Unit)?,
    onDismiss: () -> Unit,
    onOk: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggle) {
            Icon(
                if (typing) AppIcons.Clock else AppIcons.Keyboard,
                stringResource(if (typing) R.string.time_use_clock else R.string.time_use_keyboard),
            )
        }
        onClear?.let { TextButton(onClick = it) { Text(stringResource(R.string.time_remove)) } }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            TextButton(onClick = onOk) { Text(stringResource(R.string.action_ok)) }
        }
    }
}
