package com.aliraza.dailytodos.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The app's icons, drawn the one way the design asks for: outlined, 2-unit strokes with round caps and
 * joins on a 24-unit grid. Only the selected tab is filled ([selectedTab]). Kept in code so the app
 * carries no icon library; the shapes are the design boards' own.
 */
object AppIcons {
    val Add = icon("Add", stroke("M12 5v14M5 12h14"))
    val Close = icon("Close", stroke("M7 7l10 10M17 7L7 17"))
    val Check = icon("Check", stroke("M5 12.5l4.5 4.5L19 7.5"))
    val Back = icon("Back", stroke("M19 12H5.5M11 5.5L4.5 12l6.5 6.5"))
    val ChevronLeft = icon("ChevronLeft", stroke("M14.5 6l-6 6 6 6"))
    val ChevronRight = icon("ChevronRight", stroke("M9.5 6l6 6-6 6"))
    val ChevronDown = icon("ChevronDown", stroke("M7 10l5 5 5-5"))
    val ChevronUp = icon("ChevronUp", stroke("M6 14.5l6-6 6 6"))
    val Subtasks = icon("Subtasks", stroke("M6 4v9a3 3 0 0 0 3 3h4"), stroke("M14,16a3,3 0 1,0 6,0a3,3 0 1,0 -6,0z"))
    val Clock = icon("Clock", stroke("M3.5,12a8.5,8.5 0 1,0 17,0a8.5,8.5 0 1,0 -17,0z"), stroke("M12 7.5V12l3 2"))
    val Delete = icon("Delete", stroke("M4.5 7h15M9.5 7V4.5h5V7M6.5 7l1 13h9l1-13"))
    val DragHandle = icon(
        "DragHandle",
        stroke("M9 6.5h.01M15 6.5h.01M9 12h.01M15 12h.01M9 17.5h.01M15 17.5h.01", width = 3f),
    )
    val Flag = icon("Flag", stroke("M6 21V4M6 4.5h11l-2.5 4.5L17 13.5H6"))
    val Send = icon("Send", stroke("M4.5 12L20 4.5 16 20l-4-6.5zM12 13.5l8-9"))
    val Bell = icon("Bell", stroke("M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 2h-15zM10 21h4"))
    val Today = icon(
        "Today",
        stroke(
            "M6.5,5.5h11a2.5,2.5 0 0,1 2.5,2.5v10a2.5,2.5 0 0,1 -2.5,2.5h-11a2.5,2.5 0 0,1" +
                " -2.5,-2.5v-10a2.5,2.5 0 0,1 2.5,-2.5z",
        ),
        stroke("M4 10h16M8.5 3.5v4M15.5 3.5v4"),
        fill("M9,13h2a1,1 0 0,1 1,1v2a1,1 0 0,1 -1,1h-2a1,1 0 0,1 -1,-1v-2a1,1 0 0,1 1,-1z"),
    )
    val Calendar = icon(
        "Calendar",
        stroke(
            "M6.5,5.5h11a2.5,2.5 0 0,1 2.5,2.5v10a2.5,2.5 0 0,1 -2.5,2.5h-11a2.5,2.5 0 0,1" +
                " -2.5,-2.5v-10a2.5,2.5 0 0,1 2.5,-2.5z",
        ),
        stroke("M4 10h16M8.5 3.5v4M15.5 3.5v4"),
    )
    val NoDate = icon(
        "NoDate",
        stroke(
            "M6.5,5.5h11a2.5,2.5 0 0,1 2.5,2.5v10a2.5,2.5 0 0,1 -2.5,2.5h-11a2.5,2.5 0 0,1" +
                " -2.5,-2.5v-10a2.5,2.5 0 0,1 2.5,-2.5z",
        ),
        stroke("M4 10h16M8.5 3.5v4M15.5 3.5v4M9.5 13l5 5M14.5 13l-5 5"),
    )
    val Reschedule = icon(
        "Reschedule",
        stroke(
            "M20 11V7.5a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2V18a2 2 0 0 0 2 2h5M4 10h16M8.5 3.5v4M15.5 3.5v4" +
                "M14.5 17h6.5M18.5 14.5L21 17l-2.5 2.5",
        ),
    )
    val Upcoming = icon(
        "Upcoming",
        stroke(
            "M6.5,5.5h11a2.5,2.5 0 0,1 2.5,2.5v10a2.5,2.5 0 0,1 -2.5,2.5h-11a2.5,2.5 0 0,1" +
                " -2.5,-2.5v-10a2.5,2.5 0 0,1 2.5,-2.5z",
        ),
        stroke("M4 10h16M8.5 3.5v4M15.5 3.5v4M8.5 15h6.5M12.8 12.8L15 15l-2.2 2.2"),
    )
    val Lists = icon("Lists", stroke("M9.5 7h10M9.5 12h10M9.5 17h10M4.5 7h.5M4.5 12h.5M4.5 17h.5"))
    val More = icon("More", stroke("M12 5.5h.01M12 12h.01M12 18.5h.01", width = 3f))
    val Search = icon("Search", stroke("M4.5,11a6.5,6.5 0 1,0 13,0a6.5,6.5 0 1,0 -13,0z"), stroke("M16 16l4.5 4.5"))
    val Timer = icon(
        "Timer",
        stroke("M4.5,13.5a7.5,7.5 0 1,0 15,0a7.5,7.5 0 1,0 -15,0z"),
        stroke("M12 13.5V10M9.5 3h5"),
    )
    val Settings = icon(
        "Settings",
        stroke(
            "M12 3.5l1.6 2.2 2.7-.4.9 2.6 2.5 1.1-.4 2.7L21 12l-1.7 2.1.4 2.7-2.5 1.1-.9" +
                " 2.6-2.7-.4L12 20.5l-1.6-2.2-2.7.4-.9-2.6-2.5-1.1.4-2.7L3 12l1.7-2.1-.4-2.7 2.5-1.1.9-2.6 2.7.4z",
        ),
        stroke("M9,12a3,3 0 1,0 6,0a3,3 0 1,0 -6,0z"),
    )
    val Play = icon("Play", fill("M8 5.5v13l10.5-6.5z"), stroke("M8 5.5v13l10.5-6.5z"))
    val Pause = icon("Pause", stroke("M9 6v12M15 6v12", width = 2.6f))
    val Stop = icon("Stop", fill("M8.5,6.5h7a2,2 0 0,1 2,2v7a2,2 0 0,1 -2,2h-7a2,2 0 0,1 -2,-2v-7a2,2 0 0,1 2,-2z"))
    val Skip = icon("Skip", stroke("M6 6l7 6-7 6zM17 6v12"))
    val Alert = icon(
        "Alert",
        stroke("M3.5,12a8.5,8.5 0 1,0 17,0a8.5,8.5 0 1,0 -17,0z"),
        stroke("M12 7.5v5.5M12 16.5h.01"),
    )
    val Alarm = icon(
        "Alarm",
        stroke("M4.5,13a7.5,7.5 0 1,0 15,0a7.5,7.5 0 1,0 -15,0z"),
        stroke("M12 9.5V13l2.5 1.5M4 5l3-2.5M20 5l-3-2.5"),
    )
    val Snooze = icon(
        "Snooze",
        stroke("M4.5,13a7.5,7.5 0 1,0 15,0a7.5,7.5 0 1,0 -15,0z"),
        stroke("M9.5 10.5h5l-5 5h5M4 5l3-2.5M20 5l-3-2.5"),
    )
    val Battery = icon(
        "Battery",
        stroke("M5,7.5h12a2,2 0 0,1 2,2v5a2,2 0 0,1 -2,2h-12a2,2 0 0,1 -2,-2v-5a2,2 0 0,1 2,-2z"),
        stroke("M21.5 10.5v3M6.5 11.5v1"),
    )
    val Moon = icon("Moon", stroke("M19.5 14.5A7.5 7.5 0 0 1 9.5 4.5a7.5 7.5 0 1 0 10 10z"))
    val Move = icon("Move", stroke("M4 7h9M4 12h7M4 17h9M15 12h6M18.5 9.5L21 12l-2.5 2.5"))
    val Notes = icon("Notes", stroke("M5 6h14M5 11h14M5 16h9"))
    val ArrowUp = icon("ArrowUp", stroke("M12 19V5.5M6 11l6-6 6 6"))
    val Cup = icon(
        "Cup",
        stroke("M5 9.5h11V15a4 4 0 0 1-4 4H9a4 4 0 0 1-4-4zM16 11.5h1.5a2.5 2.5 0 0 1 0 5H16M8.5 3.5V6M12 3.5V6"),
    )
    val CalendarGrid = icon(
        "CalendarGrid",
        stroke(
            "M6.5,5.5h11a2.5,2.5 0 0,1 2.5,2.5v10a2.5,2.5 0 0,1 -2.5,2.5h-11a2.5,2.5 0 0,1" +
                " -2.5,-2.5v-10a2.5,2.5 0 0,1 2.5,-2.5z",
        ),
        stroke("M4 10h16M8.5 3.5v4M15.5 3.5v4"),
        stroke("M8 14h.01M12 14h.01M16 14h.01M8 17.5h.01M12 17.5h.01", width = 2.6f),
    )

    /** Minus, for a stepper. */
    val Remove = icon("Remove", stroke("M5 12h14"))

    /** A pencil: rename. */
    val Edit = icon("Edit", stroke("M4.5 19.5h4l10-10-4-4-10 10z"), stroke("M13 7l4 4"))

    /** A keyboard: type the time instead of picking it on the dial. */
    val Keyboard = icon(
        "Keyboard",
        stroke(
            "M5.5,6h13a2.5,2.5 0 0,1 2.5,2.5v7a2.5,2.5 0 0,1 -2.5,2.5h-13a2.5,2.5 0 0,1 -2.5,-2.5v-7" +
                "a2.5,2.5 0 0,1 2.5,-2.5z",
        ),
        stroke("M7 10h.01M10.5 10h.01M14 10h.01M17.5 10h.01M8.5 14.5h7", width = 2.4f),
    )

    /** The mug filled in: the screen is kept on. */
    val CupFilled = icon(
        "CupFilled",
        fill("M5 9.5h11V15a4 4 0 0 1-4 4H9a4 4 0 0 1-4-4z"),
        stroke("M5 9.5h11V15a4 4 0 0 1-4 4H9a4 4 0 0 1-4-4zM16 11.5h1.5a2.5 2.5 0 0 1 0 5H16M8.5 3.5V6M12 3.5V6"),
    )

    /**
     * The app's mark, the through-ring: a ring broken where the tick passes through it. The launcher
     * icon's 108-unit shape cropped to its middle 72 units, so the ring's centre is the icon's centre.
     */
    val Mark = icon(
        "Mark",
        stroke("M59.91 33.91A24 24 0 1 1 40.17 12.36", width = 7f),
        stroke("M23 34.5l9.5 9.5 25-27", width = 7f),
        viewport = 72f,
    )

    /** A tab's icon. */
    enum class Tab { TODAY, UPCOMING, CALENDAR, LISTS }

    fun tab(tab: Tab): ImageVector = when (tab) {
        Tab.TODAY -> Today
        Tab.UPCOMING -> Upcoming
        Tab.CALENDAR -> CalendarGrid
        Tab.LISTS -> Lists
    }

    /**
     * The selected tab's filled icon: the body in [ink], the inner detail cut out in [knock] (the colour
     * of the pill behind it). Drawn with its own colours, so show it with an unspecified tint.
     */
    fun selectedTab(tab: Tab, ink: Color, knock: Color): ImageVector {
        val body = "M6.5 5h11A2.5 2.5 0 0 1 20 7.5v11a2.5 2.5 0 0 1-2.5 2.5h-11A2.5 2.5 0 0 1 4" +
            " 18.5v-11A2.5 2.5 0 0 1 6.5 5z"
        val rings = "M8.5 3v3.5M15.5 3v3.5"
        val parts = when (tab) {
            Tab.TODAY -> listOf(
                fill(body, ink),
                stroke(rings, color = ink),
                fill(
                    "M8.7,12h2.6a1.2,1.2 0 0,1 1.2,1.2v2.6a1.2,1.2 0 0,1 -1.2,1.2h-2.6a1.2,1.2 0 0,1 -1.2,-1.2v-2.6" +
                        "a1.2,1.2 0 0,1 1.2,-1.2z",
                    knock,
                ),
            )

            Tab.UPCOMING -> listOf(
                fill(body, ink),
                stroke(rings, color = ink),
                stroke("M8.5 15h6.5M12.8 12.8L15 15l-2.2 2.2", color = knock),
            )

            Tab.CALENDAR -> listOf(
                fill(body, ink),
                stroke(rings, color = ink),
                stroke("M8 13h.01M12 13h.01M16 13h.01M8 16.5h.01M12 16.5h.01", width = 2.8f, color = knock),
            )

            Tab.LISTS -> listOf(
                stroke("M9.5 7h10M9.5 12h10M9.5 17h10", width = 2.6f, color = ink),
                fill("M3.3,7a1.7,1.7 0 1,0 3.4,0a1.7,1.7 0 1,0 -3.4,0z", ink),
                fill("M3.3,12a1.7,1.7 0 1,0 3.4,0a1.7,1.7 0 1,0 -3.4,0z", ink),
                fill("M3.3,17a1.7,1.7 0 1,0 3.4,0a1.7,1.7 0 1,0 -3.4,0z", ink),
            )
        }
        return icon("Selected$tab", *parts.toTypedArray())
    }

    /** One drawn part of an icon: a stroked outline or a filled shape. */
    private class Part(val data: String, val filled: Boolean, val width: Float, val color: Color)

    private fun stroke(data: String, width: Float = STROKE, color: Color = Color.Black) = Part(
        data,
        false,
        width,
        color,
    )

    private fun fill(data: String, color: Color = Color.Black) = Part(data, true, 0f, color)

    private fun icon(name: String, vararg parts: Part, viewport: Float = GRID): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, viewport, viewport).apply {
            parts.forEach { part ->
                if (part.filled) {
                    addPath(addPathNodes(part.data), pathFillType = PathFillType.NonZero, fill = SolidColor(part.color))
                } else {
                    addPath(
                        addPathNodes(part.data),
                        stroke = SolidColor(part.color),
                        strokeLineWidth = part.width,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }
        }.build()

    private const val GRID = 24f
    private const val STROKE = 2f
}
