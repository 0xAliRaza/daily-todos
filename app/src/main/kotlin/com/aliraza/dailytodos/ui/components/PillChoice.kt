package com.aliraza.dailytodos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliraza.dailytodos.ui.theme.button
import com.aliraza.dailytodos.ui.theme.tokens
import kotlinx.coroutines.launch

/**
 * The app's one selection style: options on a full-width 44 dp track, the picked one a solid fill with a
 * check and bold on-colour text. The fill slides to a new pick rather than jumping. Settings, the focus
 * timer's presets and signal, and the calendar modes all use it, so every choice looks and moves the same.
 */
@Composable
fun <T> PillChoice(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onPick: (T) -> Unit,
    modifier: Modifier = Modifier,
    tone: PillTone = PillTone.ACCENT,
    check: Boolean = true,
) {
    val tokens = MaterialTheme.tokens
    val (on, onInk) = when (tone) {
        PillTone.ACCENT -> tokens.accent to tokens.onAccent
        PillTone.REST -> tokens.rest to tokens.onRest
    }
    // Where each option sits on the track, as (left, width) in pixels, once laid out.
    val spans = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val marker = rememberMarker(spans[options.indexOf(selected)])
    val markerColor by animateColorAsState(on, label = "pillTone")
    Box(
        modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(tokens.fill).padding(4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().fillMaxHeight().selectableGroup().drawBehind {
                if (marker.shown) {
                    drawRoundRect(
                        markerColor,
                        topLeft = Offset(marker.left.value, 0f),
                        size = Size(marker.width.value, size.height),
                        cornerRadius = CornerRadius(SEGMENT_RADIUS.toPx()),
                    )
                }
            },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            options.forEachIndexed { index, option ->
                val picked = option == selected
                Segment(
                    text = label(option),
                    picked = picked,
                    ink = if (picked) onInk else tokens.ink2,
                    check = check,
                    modifier = Modifier.weight(1f)
                        .onPlaced { spans[index] = it.positionInParent().x to it.size.width.toFloat() }
                        .clip(RoundedCornerShape(SEGMENT_RADIUS))
                        .selectable(selected = picked, role = Role.RadioButton) { onPick(option) },
                )
            }
        }
    }
}

@Composable
private fun Segment(text: String, picked: Boolean, ink: Color, check: Boolean, modifier: Modifier) {
    val color by animateColorAsState(ink, label = "pill")
    Row(
        modifier.fillMaxHeight().padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (picked && check) {
            Icon(AppIcons.Check, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.button,
            fontWeight = if (picked) FontWeight.Bold else FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val SEGMENT_RADIUS = 18.dp

/** The fill's place on the track: it lands on the first pick, then slides to each new one. */
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
