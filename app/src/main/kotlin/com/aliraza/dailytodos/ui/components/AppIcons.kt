package com.aliraza.dailytodos.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The few icons the app uses, as Material Symbols outlines on a 24-unit grid. Kept in code so the app
 * carries no icon library.
 */
object AppIcons {
    val Add = icon("Add", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")
    val Close = icon(
        "Close",
        "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z",
    )
    val Check = icon("Check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z")
    val Back = icon("Back", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z")
    val ChevronLeft = icon("ChevronLeft", "M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z")
    val ChevronRight = icon("ChevronRight", "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z")
    val ChevronDown = icon("ChevronDown", "M16.59,8.59L12,13.17 7.41,8.59 6,10l6,6 6,-6z")
    val Checklist = icon(
        "Checklist",
        "M22,7h-9v2h9V7zM22,15h-9v2h9V15zM5.54,11L2,7.46l1.41,-1.41l2.12,2.12l4.24,-4.24l1.41,1.41L5.54,11z" +
            "M5.54,19L2,15.46l1.41,-1.41l2.12,2.12l4.24,-4.24l1.41,1.41L5.54,19z",
    )
    val Clock = icon(
        "Clock",
        "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8," +
            "-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z",
    )
    val Delete = icon("Delete", "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z")
    val DragHandle = icon("DragHandle", "M20,9H4v2h16V9zM4,15h16v-2H4v2z")
    val Flag = icon("Flag", "M14.4,6L14,4H5v17h2v-7h5.6l0.4,2h7V6z")
    val Send = icon("Send", "M4,12l1.41,1.41L11,7.83V20h2V7.83l5.58,5.59L20,12l-8,-8 -8,8z")
    val Bell = icon(
        "Bell",
        "M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2zm6,-6v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67," +
            "-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z",
    )
    val Today = icon(
        "Today",
        "M19,3h-1V1h-2v2H8V1H6v2H5c-1.11,0 -1.99,0.9 -1.99,2L3,19c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V5c0," +
            "-1.1 -0.9,-2 -2,-2zm0,16H5V8h14v11zM7,10h5v5H7z",
    )

    /** A calendar with a turning arrow: move to another day. */
    val Reschedule = icon(
        "Reschedule",
        "M21,12V6c0,-1.1 -0.9,-2 -2,-2h-1V2h-2v2H8V2H6v2H5C3.9,4 3,4.9 3,6v14c0,1.1 0.9,2 2,2h7v-2H5V10h14v2H21z" +
            "M15.64,20c0.43,1.45 1.77,2.5 3.36,2.5c1.93,0 3.5,-1.57 3.5,-3.5s-1.57,-3.5 -3.5,-3.5c-0.95,0 -1.82," +
            "0.38 -2.45,1H18V18h-4v-4h1.5v1.43C16.4,14.55 17.64,14 19,14c2.76,0 5,2.24 5,5s-2.24,5 -5,5" +
            "c-2.42,0 -4.44,-1.72 -4.9,-4H15.64z",
    )
    val Upcoming = icon(
        "Upcoming",
        "M17,10H7v2h10v-2zM19,3h-1V1h-2v2H8V1H6v2H5c-1.11,0 -1.99,0.9 -1.99,2L3,19c0,1.1 0.89,2 2,2h14c1.1,0 2," +
            "-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM19,19H5V8h14v11zM14,14H7v2h7v-2z",
    )
    val Calendar = icon(
        "Calendar",
        "M19,4h-1V2h-2v2H8V2H6v2H5c-1.11,0 -1.99,0.9 -1.99,2L3,20c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V6c0," +
            "-1.1 -0.9,-2 -2,-2zm0,16H5V10h14v10zM9,14H7v-2h2v2zm4,0h-2v-2h2v2zm4,0h-2v-2h2v2zm-8,4H7v-2h2v2z" +
            "m4,0h-2v-2h2v2zm4,0h-2v-2h2v2z",
    )
    val Lists = icon(
        "Lists",
        "M3,13h2v-2H3v2zm0,4h2v-2H3v2zm0,-8h2V7H3v2zm4,4h14v-2H7v2zm0,4h14v-2H7v2zM7,7v2h14V7H7z",
    )
    val More = icon(
        "More",
        "M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2" +
            " -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z",
    )
    val Search = icon(
        "Search",
        "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 " +
            "9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5," +
            "11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z",
    )
    val Timer = icon(
        "Timer",
        "M15,1H9v2h6V1zm-4,13h2V8h-2v6zm8.03,-6.61l1.42,-1.42c-0.43,-0.51 -0.9,-0.99 -1.41,-1.41l-1.42,1.42" +
            "C16.07,4.74 14.12,4 12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9 9,-4.03 9,-9c0,-2.12 -0.74,-4.07 -1.97," +
            "-5.61zM12,20c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z",
    )
    val Settings = icon(
        "Settings",
        "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 " +
            "0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 " +
            "-1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 " +
            "-0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74," +
            "8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03," +
            "1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 " +
            "1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36," +
            "-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 " +
            "0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6," +
            "1.62 3.6,3.6S13.98,15.6 12,15.6z",
    )
    val Edit = icon(
        "Edit",
        "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34" +
            "c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z",
    )
    val Play = icon("Play", "M8,5v14l11,-7z")
    val Pause = icon("Pause", "M6,19h4V5H6v14zm8,-14v14h4V5h-4z")
    val Stop = icon("Stop", "M6,6h12v12H6z")
    val Remove = icon("Remove", "M19,13H5v-2h14v2z")
    val Info = icon(
        "Info",
        "M11,7h2v2h-2zm0,4h2v6h-2zm1,-9C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zm0,18" +
            "c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8z",
    )

    /** An exclamation mark in a ring: something to look at. */
    val Alert = icon(
        "Alert",
        "M11,15h2v2h-2zm0,-8h2v6h-2zm0.99,-5C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 " +
            "11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8z",
    )
    val ArrowUp = icon("ArrowUp", "M4,12l1.41,1.41L11,7.83V20h2V7.83l5.58,5.59L20,12l-8,-8 -8,8z")
    val Pin = icon(
        "Pin",
        "M16,9V4h1c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H7C6.45,2 6,2.45 6,3s0.45,1 1,1h1v5c0,1.66 -1.34,3 -3,3v2" +
            "h5.97v7l1,1 1,-1v-7H19v-2c-1.66,0 -3,-1.34 -3,-3z",
    )

    /** A keyboard: type the time instead of turning the clock. */
    val Keyboard = icon(
        "Keyboard",
        "M20,5H4C2.9,5 2.01,5.9 2.01,7L2,17c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V7C22,5.9 21.1,5 20,5z" +
            "M11,8h2v2h-2V8zM11,11h2v2h-2V11zM8,8h2v2H8V8zM8,11h2v2H8V11zM7,13H5v-2h2V13zM7,10H5V8h2V10z" +
            "M16,17H8v-2h8V17zM16,13h-2v-2h2V13zM16,10h-2V8h2V10zM19,13h-2v-2h2V13zM19,10h-2V8h2V10z",
    )

    private fun icon(name: String, path: String): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
        .addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
        .build()
}
