package com.aliraza.dailytodos.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.aliraza.dailytodos.ui.theme.barTitle
import com.aliraza.dailytodos.ui.theme.screenTitle
import com.aliraza.dailytodos.ui.theme.tokens
import kotlin.math.roundToInt

/** The action row over a large title: back or nothing at the start, actions at the end. */
val HeaderRowHeight = 56.dp

/** The bar a large header folds into, with the title at 20 sp. */
val HeaderBarHeight = 64.dp

/**
 * How far a page's large header has folded, driven by the page's own scrolling through [connection]:
 * scrolling the content up folds the title away first, and the title unfolds again once the content is
 * back at its top. A partly folded header settles to the nearer end when a fling stops.
 *
 * Sizes are in px and set by [LargeHeader] as it measures, so the fold follows the font scale.
 */
@Stable
class HeaderCollapse {
    /** How far the header has folded, from 0 (open) to [range]. */
    var offset by mutableFloatStateOf(0f)
        private set

    /** How much the header can fold: the open height less the folded bar. */
    var range by mutableFloatStateOf(0f)
        private set

    /** 0 while open, 1 once folded into the bar. */
    val fraction: Float get() = if (range > 0f) offset / range else 0f

    /** Sets how far the header can fold, keeping what is already folded inside it. */
    fun fitRange(px: Float) {
        val next = px.coerceAtLeast(0f)
        if (next != range) {
            range = next
            offset = offset.coerceIn(0f, next)
        }
    }

    /** Folds by [delta] px (negative unfolds), as far as the header allows; returns the part it used. */
    fun fold(delta: Float): Float {
        val next = (offset + delta).coerceIn(0f, range)
        val used = next - offset
        offset = next
        return used
    }

    // The px sizes the header last measured. Plain fields: they are written and read while laying out.
    internal var rowPx = 0f
    internal var barPx = 0f
    internal var blockPx = 0f

    /** The header as it stands now; see [headerFrame]. */
    internal fun frame(): HeaderFrame = headerFrame(rowPx, barPx, blockPx, offset)

    /** Snaps a partly folded header to whichever end is nearer. */
    suspend fun settle() {
        if (offset <= 0f || offset >= range) return
        val target = if (fraction < SETTLE_AT) 0f else range
        animate(offset, target) { value, _ -> offset = value }
    }

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        // Content moving up (negative y) folds the header before the content itself scrolls.
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0f) Offset(0f, -fold(-available.y)) else Offset.Zero

        // What the content could not use moving down, being at its top, unfolds the header.
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (available.y > 0f) Offset(0f, -fold(-available.y)) else Offset.Zero

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settle()
            return Velocity.Zero
        }
    }

    private companion object {
        const val SETTLE_AT = 0.5f
    }
}

@Composable
fun rememberHeaderCollapse(): HeaderCollapse = remember { HeaderCollapse() }

/**
 * One moment of a header's fold, in px: its whole [height], the action [row]'s height, how much of the
 * large title block shows ([block]), and the opacity of the large [titleAlpha] and of the bar title
 * ([barTitleAlpha]). Open it is the row over the whole block; folded it is the bar alone.
 */
internal data class HeaderFrame(
    val height: Float,
    val row: Float,
    val block: Float,
    val titleAlpha: Float,
    val barTitleAlpha: Float,
)

/** How much a header with a [row], a folded [bar] and a large title [block] (all px) can fold. */
internal fun headerRange(row: Float, bar: Float, block: Float): Float = (row + block - bar).coerceAtLeast(0f)

/**
 * The header folded by [offset] px. The row grows from [row] to [bar] while the block shrinks to
 * nothing, so the height falls evenly with the offset. The large title fades over the first half of the
 * fold, the bar title over the second, so the two never show at once.
 */
internal fun headerFrame(row: Float, bar: Float, block: Float, offset: Float): HeaderFrame {
    val range = headerRange(row, bar, block)
    if (range <= 0f) return HeaderFrame(row + block, row, block, titleAlpha = 1f, barTitleAlpha = 0f)
    val fraction = (offset / range).coerceIn(0f, 1f)
    val height = row + block - range * fraction
    val rowNow = row + (bar - row).coerceAtLeast(0f) * fraction
    return HeaderFrame(
        height = height,
        row = rowNow,
        block = (height - rowNow).coerceAtLeast(0f),
        titleAlpha = (1f - fraction * 2f).coerceIn(0f, 1f),
        barTitleAlpha = (fraction * 2f - 1f).coerceIn(0f, 1f),
    )
}

/**
 * The app's one page header: a 56 dp action row ([navigation] at the start, [actions] at the end) over
 * a 32 sp [title] and an optional [subtitle] (15 sp, ink-2). With a [collapse] attached to the page's
 * scrolling, it folds into a 64 dp bar with the title at 20 sp. Opaque up to the screen's top edge, so
 * rows scrolled up pass under it.
 *
 * With [large] off it is the action row alone, the [title] (if any) in the row at 20 sp.
 */
@Composable
fun LargeHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    collapse: HeaderCollapse? = null,
    large: Boolean = true,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val tokens = MaterialTheme.tokens
    val state = collapse ?: remember { HeaderCollapse() }
    Column(
        modifier.fillMaxWidth().background(tokens.background).statusBarsPadding()
            .drawBehind {
                // A hairline fades in under the folded bar, where rows start to pass beneath it.
                val alpha = if (large) state.fraction else 0f
                if (alpha > 0f) {
                    val y = size.height - HAIRLINE / 2
                    drawLine(tokens.hairline.copy(alpha = alpha), Offset(0f, y), Offset(size.width, y), HAIRLINE)
                }
            },
    ) {
        ActionRow(title, large, state, navigation, actions)
        if (large) TitleBlock(title, subtitle, state)
    }
}

@Composable
private fun ActionRow(
    title: String,
    large: Boolean,
    state: HeaderCollapse,
    navigation: (@Composable () -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().foldedRow(state, large).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigation?.invoke()
        Text(
            title,
            style = MaterialTheme.typography.barTitle,
            color = MaterialTheme.tokens.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = if (navigation == null) 16.dp else 4.dp)
                .graphicsLayer { alpha = if (large) state.frame().barTitleAlpha else 1f },
        )
        actions()
    }
}

@Composable
private fun TitleBlock(title: String, subtitle: String?, state: HeaderCollapse) {
    val tokens = MaterialTheme.tokens
    Column(
        Modifier.fillMaxWidth().foldedBlock(state)
            .graphicsLayer { alpha = state.frame().titleAlpha }
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.screenTitle,
            color = tokens.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = tokens.ink2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** The action row at its height for the fold: 56 dp open, 64 dp folded. */
private fun Modifier.foldedRow(state: HeaderCollapse, large: Boolean): Modifier = layout { measurable, constraints ->
    state.rowPx = HeaderRowHeight.toPx()
    state.barPx = HeaderBarHeight.toPx()
    val height = (if (large) state.frame().row else state.rowPx).roundToInt()
    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
    layout(placeable.width, height) { placeable.place(0, 0) }
}

/** The large title block, measured whole and shown only as far as the fold leaves it, sliding up. */
private fun Modifier.foldedBlock(state: HeaderCollapse): Modifier = clipToBounds().layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    state.blockPx = placeable.height.toFloat()
    state.fitRange(headerRange(state.rowPx, state.barPx, state.blockPx))
    val shown = state.frame().block.roundToInt().coerceIn(0, placeable.height)
    layout(placeable.width, shown) { placeable.place(0, shown - placeable.height) }
}

private const val HAIRLINE = 1f
