package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The month under a page title, read like the subtitle Today shows there ("November 2026"), with Today
 * (when [onToday] is set) and small arrows at its right. It sits tight under the title, so a date view
 * gets its controls without a row of its own.
 */
@Composable
fun DateNavLine(
    text: String,
    onToday: (() -> Unit)?,
    previous: DateStep,
    next: DateStep,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        onToday?.let { TodayButton(it, Modifier.padding(end = 4.dp)) }
        StepArrow(AppIcons.ChevronLeft, previous)
        StepArrow(AppIcons.ChevronRight, next)
    }
}

/** One of a date line's arrows: what it says to a screen reader, whether it can be used, what it does. */
class DateStep(val label: String, val enabled: Boolean = true, val onClick: () -> Unit)

@Composable
private fun StepArrow(icon: ImageVector, step: DateStep) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.size(ArrowSize).clip(CircleShape)
            .clickable(enabled = step.enabled, role = Role.Button, onClickLabel = step.label, onClick = step.onClick)
            .semantics { contentDescription = step.label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            null,
            tint = if (step.enabled) scheme.onSurfaceVariant else scheme.outlineVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** As small as the section headers' buttons, so the line stays about as tall as a line of text. */
private val ArrowSize = 36.dp
