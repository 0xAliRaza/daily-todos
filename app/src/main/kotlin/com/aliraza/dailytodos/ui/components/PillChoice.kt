package com.aliraza.dailytodos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The app's one segmented control: options on a rounded track, the picked one raised on the surface.
 * The raised part slides to a new pick rather than jumping. Settings, the focus timer and the calendar
 * modes all use it, so every choice looks and moves the same.
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
    // Where each option sits on the track, as (left, width) in pixels, once laid out.
    val spans = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val marker = rememberMarker(spans[options.indexOf(selected)])
    Box(modifier.clip(CircleShape).background(scheme.surfaceVariant).padding(3.dp)) {
        Row(
            Modifier.selectableGroup().drawBehind {
                if (marker.shown) {
                    drawRoundRect(
                        scheme.surface,
                        topLeft = Offset(marker.left.value, 0f),
                        size = Size(marker.width.value, size.height),
                        cornerRadius = CornerRadius(size.height / 2),
                    )
                }
            },
        ) {
            options.forEachIndexed { index, option ->
                val on = option == selected
                val ink by animateColorAsState(if (on) scheme.onSurface else scheme.onSurfaceVariant, label = "pill")
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = ink,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.then(if (fill) Modifier.weight(1f) else Modifier)
                        .onPlaced { spans[index] = it.positionInParent().x to it.size.width.toFloat() }
                        .clip(CircleShape)
                        .selectable(selected = on, role = Role.RadioButton) { onPick(option) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** The raised part's place on the track: it lands on the first pick, then slides to each new one. */
private class Marker {
    val left = Animatable(0f)
    val width = Animatable(0f)
    var shown by mutableStateOf(false)
}

@Composable
private fun rememberMarker(target: Pair<Float, Float>?): Marker {
    val marker = remember { Marker() }
    LaunchedEffect(target) {
        val (left, width) = target ?: return@LaunchedEffect
        if (!marker.shown) {
            marker.left.snapTo(left)
            marker.width.snapTo(width)
            marker.shown = true
        } else {
            launch { marker.left.animateTo(left, Slide) }
            marker.width.animateTo(width, Slide)
        }
    }
    return marker
}

private val Slide = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
