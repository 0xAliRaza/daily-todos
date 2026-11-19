package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The app's one segmented control: options on a rounded track, the picked one raised on the surface.
 * Settings, the focus timer and the calendar modes all use it, so every choice looks the same.
 */
@Composable
fun <T> PillChoice(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onPick: (T) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier.clip(CircleShape).background(scheme.surfaceVariant).padding(3.dp).selectableGroup()) {
        options.forEach { option ->
            val on = option == selected
            Text(
                label(option),
                style = MaterialTheme.typography.labelLarge,
                color = if (on) scheme.onSurface else scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.then(if (fill) Modifier.weight(1f) else Modifier)
                    .clip(CircleShape).background(if (on) scheme.surface else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton) { onPick(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
