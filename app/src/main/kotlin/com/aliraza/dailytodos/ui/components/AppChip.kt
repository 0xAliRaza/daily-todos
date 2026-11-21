package com.aliraza.dailytodos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliraza.dailytodos.ui.theme.button
import com.aliraza.dailytodos.ui.theme.tokens

/**
 * The app's one chip: a 36 dp filled button (48 dp to touch) with an icon and a label. Once [set], it
 * shows what was understood in accent-container colours, or the break's green with [tone] REST.
 * [iconTint] colours a value's icon, such as a
 * priority's flag. [onClear] adds a trailing × that clears the value without opening the picker.
 */
@Composable
fun AppChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    set: Boolean = false,
    iconTint: Color? = null,
    onClear: (() -> Unit)? = null,
    clearLabel: String? = null,
    tone: PillTone = PillTone.ACCENT,
) {
    val tokens = MaterialTheme.tokens
    val (setFill, setInk) = when (tone) {
        PillTone.ACCENT -> tokens.accentContainer to tokens.onAccentContainer
        PillTone.REST -> tokens.restContainer to tokens.onRestContainer
    }
    val container by animateColorAsState(if (set) setFill else tokens.fill, label = "chip")
    val ink by animateColorAsState(if (set) setInk else tokens.ink, label = "chipInk")
    Box(modifier.heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier.height(36.dp).clip(CircleShape).background(container)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = if (icon == null) 14.dp else 10.dp, end = if (onClear == null) 14.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(it, null, tint = iconTint ?: ink, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.button,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            onClear?.let { clear ->
                Box(
                    Modifier.padding(start = 2.dp).size(28.dp).clip(CircleShape)
                        .clickable(role = Role.Button, onClick = clear)
                        .semantics { clearLabel?.let { contentDescription = it } },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Close, null, tint = ink, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
