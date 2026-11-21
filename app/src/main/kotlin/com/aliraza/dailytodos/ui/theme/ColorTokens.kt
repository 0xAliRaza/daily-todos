package com.aliraza.dailytodos.ui.theme

import androidx.compose.ui.graphics.Color
import com.aliraza.dailytodos.core.model.Accent

/**
 * Design tokens: the only place colours are defined.
 *
 * Three layers, assembled by [tokensFor]:
 * - **Neutrals** per mode (light, dark, dark on black): the page, the sheet one step above it, the fill
 *   used as the track of pills and chips, hairlines, three inks and the inverse surface of snackbars.
 * - **Accent** per accent and mode. The accent is chrome only: buttons, the add button, selection, the
 *   active tab, links, the today marker in pickers and the calendar's now line. It never colours a
 *   task's ring, date or calendar block, so it never competes with the P3 blue.
 * - **Signals**, one meaning each: P1 to P3 rings and dots, danger (an overdue date, deleting), the
 *   break green. Red is kept for overdue and destructive; a time already passed today stays grey.
 *
 * `ThemeContrastTest` fails if any pairing in any accent and mode drops below WCAG AA.
 */
data class ColorTokens(
    val background: Color,
    /** Sheets and dialogs: one step above the page so stacked layers separate. */
    val sheet: Color,
    /** The track of pills, chips and filled fields. */
    val fill: Color,
    /** Lines between rows. Decorative, so it has no contrast requirement. */
    val hairline: Color,
    val ink: Color,
    /** Secondary text: meta lines, hints. */
    val ink2: Color,
    /** Rings and disabled controls only; never text. Graphic, so 3:1. */
    val ink3: Color,
    /** The snackbar's surface, and its text. */
    val inverse: Color,
    val onInverse: Color,
    /** The share of black laid over the page behind a sheet. */
    val scrimAlpha: Float,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    /** An action on the inverse surface, such as the snackbar's Undo. */
    val inverseAction: Color,
    /** Recognised words in the add bar, under [onAccentContainer] text. */
    val highlight: Color,
    /** Check-box rings, calendar block edges and month dots, P1 to P3. Graphics, so 3:1. */
    val priority1: Color,
    val priority2: Color,
    val priority3: Color,
    /** Overdue dates, Delete and error rows. Text, so 4.5:1. */
    val danger: Color,
    val onDanger: Color,
    /** A warning banner's surface, under ink and [danger] text. */
    val dangerContainer: Color,
    /** The focus timer's break: its arc, chips, button and the now-focusing bar. Text, so 4.5:1. */
    val rest: Color,
    val onRest: Color,
    val restContainer: Color,
    val onRestContainer: Color,
    /** Something to look at that isn't urgent: a setting to fix. Text, so 4.5:1. */
    val warning: Color,
)

/** The colours that change with the mode but not with the accent. */
private data class Neutrals(
    val background: Color,
    val sheet: Color,
    val fill: Color,
    val hairline: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val inverse: Color,
    val onInverse: Color,
    val scrimAlpha: Float,
)

/** One accent in one mode. */
private data class AccentSet(
    val accent: Color,
    val onAccent: Color,
    val container: Color,
    val onContainer: Color,
    val inverseAction: Color,
    val highlight: Color,
)

private data class Signals(
    val priority1: Color,
    val priority2: Color,
    val priority3: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val rest: Color,
    val onRest: Color,
    val restContainer: Color,
    val onRestContainer: Color,
    val warning: Color,
)

private val LightNeutrals = Neutrals(
    background = Color(0xFFFFFFFF),
    sheet = Color(0xFFFFFFFF),
    fill = Color(0xFFF2F2F3),
    hairline = Color(0xFFEBEBEB),
    ink = Color(0xFF202020),
    ink2 = Color(0xFF666666),
    ink3 = Color(0xFF8C8C8C),
    inverse = Color(0xFF2A2A2A),
    onInverse = Color(0xFFFFFFFF),
    scrimAlpha = 0.32f,
)

private val DarkNeutrals = Neutrals(
    background = Color(0xFF1E1E1E),
    sheet = Color(0xFF2A2A2A),
    fill = Color(0xFF2E2E2E),
    hairline = Color(0xFF333333),
    ink = Color(0xFFEDEDED),
    ink2 = Color(0xFFADADAD),
    ink3 = Color(0xFF8C8C8C),
    inverse = Color(0xFFEDEDED),
    onInverse = Color(0xFF202020),
    scrimAlpha = 0.6f,
)

/** Dark on a pure black page for OLED screens. Every step keeps its distance; inks are unchanged. */
private val BlackNeutrals = DarkNeutrals.copy(
    background = Color(0xFF000000),
    sheet = Color(0xFF141414),
    fill = Color(0xFF1F1F1F),
    hairline = Color(0xFF262626),
    scrimAlpha = 0.7f,
)

private val LightSignals = Signals(
    priority1 = Color(0xFFD1453B),
    priority2 = Color(0xFFC76E00),
    priority3 = Color(0xFF246FE0),
    danger = Color(0xFFC3392F),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFFBEAE8),
    rest = Color(0xFF058527),
    onRest = Color(0xFFFFFFFF),
    restContainer = Color(0xFFE4F4E8),
    onRestContainer = Color(0xFF04661D),
    warning = Color(0xFFB45309),
)

private val DarkSignals = Signals(
    priority1 = Color(0xFFFF7B72),
    priority2 = Color(0xFFF2994A),
    priority3 = Color(0xFF6FA3FF),
    danger = Color(0xFFFF8A80),
    onDanger = Color(0xFF1A1A1A),
    dangerContainer = Color(0xFF3A2321),
    rest = Color(0xFF5FCF7E),
    onRest = Color(0xFF0B2414),
    restContainer = Color(0xFF173A22),
    onRestContainer = Color(0xFFB5EBC4),
    warning = Color(0xFFF5B23D),
)

/** One accent in one mode, in [AccentSet]'s order: accent, on-accent, container, on-container, inverse, highlight. */
private fun accentOf(vararg argb: Long) = AccentSet(
    accent = Color(argb[0]),
    onAccent = Color(argb[1]),
    container = Color(argb[2]),
    onContainer = Color(argb[3]),
    inverseAction = Color(argb[4]),
    highlight = Color(argb[5]),
)

/** Every accent's light and dark set, from the design's Tokens board. */
private val Accents: Map<Accent, Pair<AccentSet, AccentSet>> = mapOf(
    Accent.INK_BLUE to (
        accentOf(0xFF2563EB, 0xFFFFFFFF, 0xFFEAF1FE, 0xFF1E3A8A, 0xFF93B4FF, 0xFFDCE7FD) to
            accentOf(0xFF7AA2FF, 0xFF0E1A33, 0xFF1F2A44, 0xFFC7D7FF, 0xFF1D4ED8, 0xFF26385F)
        ),
    Accent.VIOLET to (
        accentOf(0xFF6D28D9, 0xFFFFFFFF, 0xFFF1EBFE, 0xFF4C1D95, 0xFFC4B0FF, 0xFFE7DCFD) to
            accentOf(0xFFB69CFF, 0xFF1E1238, 0xFF2B2340, 0xFFDDD0FF, 0xFF5B21B6, 0xFF3A2E5C)
        ),
    Accent.TEAL to (
        accentOf(0xFF0F766E, 0xFFFFFFFF, 0xFFE2F3F0, 0xFF134E4A, 0xFF5FD4C8, 0xFFCDEDE8) to
            accentOf(0xFF4CC9BC, 0xFF062B27, 0xFF163733, 0xFFB6EDE6, 0xFF115E59, 0xFF1E4A44)
        ),
    Accent.GRAPHITE to (
        accentOf(0xFF2B2B2E, 0xFFFFFFFF, 0xFFE8E8EA, 0xFF1F1F21, 0xFFFFFFFF, 0xFFDEDEE1) to
            accentOf(0xFFEDEDED, 0xFF1A1A1A, 0xFF3A3A3C, 0xFFF2F2F2, 0xFF1A1A1A, 0xFF4A4A4D)
        ),
)

private fun accentSet(accent: Accent, dark: Boolean): AccentSet =
    Accents.getValue(accent).let { (light, night) -> if (dark) night else light }

/** The token set for one mode and accent. [black] only applies in dark. */
fun tokensFor(dark: Boolean, black: Boolean = false, accent: Accent = Accent.INK_BLUE): ColorTokens {
    val n = when {
        !dark -> LightNeutrals
        black -> BlackNeutrals
        else -> DarkNeutrals
    }
    val a = accentSet(accent, dark)
    val s = if (dark) DarkSignals else LightSignals
    return ColorTokens(
        background = n.background,
        sheet = n.sheet,
        fill = n.fill,
        hairline = n.hairline,
        ink = n.ink,
        ink2 = n.ink2,
        ink3 = n.ink3,
        inverse = n.inverse,
        onInverse = n.onInverse,
        scrimAlpha = n.scrimAlpha,
        accent = a.accent,
        onAccent = a.onAccent,
        accentContainer = a.container,
        onAccentContainer = a.onContainer,
        inverseAction = a.inverseAction,
        highlight = a.highlight,
        priority1 = s.priority1,
        priority2 = s.priority2,
        priority3 = s.priority3,
        danger = s.danger,
        onDanger = s.onDanger,
        dangerContainer = s.dangerContainer,
        rest = s.rest,
        onRest = s.onRest,
        restContainer = s.restContainer,
        onRestContainer = s.onRestContainer,
        warning = s.warning,
    )
}

val LightTokens: ColorTokens = tokensFor(dark = false)
val DarkTokens: ColorTokens = tokensFor(dark = true)
