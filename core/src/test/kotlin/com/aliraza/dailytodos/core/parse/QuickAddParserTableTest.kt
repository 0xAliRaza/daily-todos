package com.aliraza.dailytodos.core.parse

import com.aliraza.dailytodos.core.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * One row per typed line. "Now" is Wednesday 2026-11-18 at 10:30, so "friday" is 2026-11-20 and a time
 * of day before 10:30 has already passed today.
 */
@RunWith(Parameterized::class)
class QuickAddParserTableTest(private val row: Row) {

    data class Row(
        val input: String,
        val title: String,
        val date: String? = null,
        val time: String? = null,
        val priority: Int = 0,
        val list: String? = null,
        val dayFirst: Boolean = true,
        val newList: Boolean = false,
        val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    ) {
        override fun toString() = "\"$input\"" + if (weekStart == DayOfWeek.MONDAY) "" else " ($weekStart)"
    }

    @Test
    fun parses() {
        val result = QuickAddParser.parse(
            QuickAddInput(row.input, NOW, listNames = LISTS, dayFirst = row.dayFirst, weekStart = row.weekStart),
        )
        val expected = listOf(row.title, row.date?.let(LocalDate::parse), row.time?.let(LocalTime::parse))
        val actual = listOf(result.title, result.date, result.time)
        assertEquals("title, date, time", expected, actual)
        assertEquals("priority", Priority.fromLevel(row.priority), result.priority)
        assertEquals("list", row.list, result.listName)
        assertEquals("new list", row.newList, result.newList)
    }

    companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 18, 10, 30)
        val LISTS = listOf("Daily", "Groceries", "Events", "Weekend chores")
        private const val TODAY = "2026-11-18"
        private const val YESTERDAY = "2026-11-17"
        private const val TOMORROW = "2026-11-19"
        private const val FRIDAY = "2026-11-20"

        private fun none(input: String, dayFirst: Boolean = true) = Row(input, input, dayFirst = dayFirst)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun rows(): List<Row> =
            relativeDays + nextWeek + weekdays + dates + times + dayParts + offsets + tags + mixed + sentences

        private val relativeDays = listOf(
            Row("Call bank tomorrow", "Call bank", TOMORROW),
            Row("Call bank today", "Call bank", TODAY),
            Row("Call bank tod", "Call bank", TODAY),
            Row("Call bank tom", "Call bank", TOMORROW),
            Row("Call bank tmrw", "Call bank", TOMORROW),
            Row("Call bank day after tomorrow", "Call bank", "2026-11-20"),
            Row("Call bank the day after tomorrow", "Call bank", "2026-11-20"),
            Row("tomorrow call bank", "call bank", TOMORROW),
            Row("Call bank by tomorrow", "Call bank", TOMORROW),
            Row("Pay rent tonight", "Pay rent", TODAY, "20:00"),
            Row("Call bank tomorrow tomorrow", "Call bank tomorrow", TOMORROW),
            Row("Call bank by tod", "Call bank", TODAY),
            // A short form as the last word wins over a name typed in lower case; tapping it un-recognises it.
            Row("call tom", "call", TOMORROW),
            none("Call Tom"),
            none("Ask tom about it"),
            none("Read Tomorrow book"),
            none("Watch Tonight show"),
            none("Tomorrow"),
            none("tomorrow"),
            none("tonight"),
            none("Tomorrowland tickets"),
            Row("Call bank yesterday", "Call bank", YESTERDAY),
            Row("Call bank by yesterday", "Call bank", YESTERDAY),
            Row("yesterday call bank", "call bank", YESTERDAY),
            Row("Call bank yesterday 4pm", "Call bank", YESTERDAY, "16:00"),
            none("Read Yesterday book"),
            none("yesterday"),
        )

        /** "next week" is the start of next week, the same day as the date sheet's Next week. */
        private val nextWeek = listOf(
            Row("Plan trip next week", "Plan trip", "2026-11-23"),
            Row("Plan trip by next week", "Plan trip", "2026-11-23"),
            Row("next week plan trip", "plan trip", "2026-11-23"),
            Row("Plan trip next week 9am", "Plan trip", "2026-11-23", "09:00"),
            Row("Plan trip next week #events", "Plan trip", "2026-11-23", list = "Events"),
            Row("Plan trip next week", "Plan trip", "2026-11-22", weekStart = DayOfWeek.SUNDAY),
            Row("Plan trip next week", "Plan trip", "2026-11-21", weekStart = DayOfWeek.SATURDAY),
            none("Read Next Week magazine"),
            none("Plan trip next weekend"),
            none("next week"),
        )

        private val weekdays = listOf(
            Row("Gym friday", "Gym", FRIDAY),
            Row("Gym Friday", "Gym", FRIDAY),
            Row("Gym on friday", "Gym", FRIDAY),
            Row("Gym this friday", "Gym", FRIDAY),
            Row("Gym next friday", "Gym", FRIDAY),
            Row("Gym by friday", "Gym", FRIDAY),
            Row("Gym wednesday", "Gym", TODAY),
            Row("Gym next wednesday", "Gym", "2026-11-25"),
            Row("Gym monday", "Gym", "2026-11-23"),
            Row("Gym tuesday", "Gym", "2026-11-24"),
            Row("Gym thursday", "Gym", TOMORROW),
            Row("Gym saturday", "Gym", "2026-11-21"),
            Row("Gym sunday", "Gym", "2026-11-22"),
            Row("Gym fri", "Gym", FRIDAY),
            Row("Gym on fri", "Gym", FRIDAY),
            Row("Gym next mon", "Gym", "2026-11-23"),
            Row("Gym this sat", "Gym", "2026-11-21"),
            Row("Gym fri p1", "Gym", FRIDAY, priority = 1),
            Row("Gym sat", "Gym", "2026-11-21"),
            Row("Gym thurs", "Gym", TOMORROW),
            Row("Gym tues", "Gym", "2026-11-24"),
            Row("Gym wed", "Gym", TODAY),
            Row("on monday call mom", "call mom", "2026-11-23"),
            Row("Gym friday monday", "Gym monday", FRIDAY),
            none("sat down with Sam"),
            none("Sun cream"),
            none("Fri night drinks"),
            none("Thu"),
            none("Wed plans with Ana"),
            none("Mon Cheri chocolates"),
        )

        private val dates = listOf(
            Row("Dentist 25 dec", "Dentist", "2026-12-25"),
            Row("Dentist dec 25", "Dentist", "2026-12-25"),
            Row("Dentist 25 December", "Dentist", "2026-12-25"),
            Row("Dentist 25 december 2026", "Dentist", "2026-12-25"),
            Row("Dentist december 25 2027", "Dentist", "2027-12-25"),
            Row("Dentist 25th dec", "Dentist", "2026-12-25"),
            Row("Dentist dec 25th", "Dentist", "2026-12-25"),
            Row("Dentist 25th of december", "Dentist", "2026-12-25"),
            Row("Dentist 1st jan", "Dentist", "2027-01-01"),
            Row("Dentist 2nd feb", "Dentist", "2027-02-02"),
            Row("Dentist 3rd mar", "Dentist", "2027-03-03"),
            Row("Dentist on 25 dec", "Dentist", "2026-12-25"),
            Row("Dentist by dec 1", "Dentist", "2026-12-01"),
            Row("Dentist 25/12", "Dentist", "2026-12-25"),
            Row("Dentist 25/12/26", "Dentist", "2026-12-25"),
            Row("Dentist 25/12/2027", "Dentist", "2027-12-25"),
            Row("Dentist 2026-12-25", "Dentist", "2026-12-25"),
            Row("Dentist 1 jan", "Dentist", "2027-01-01"),
            Row("Dentist 18 nov", "Dentist", TODAY),
            Row("Dentist 17 nov", "Dentist", "2027-11-17"),
            Row("Dentist 29 feb", "Dentist", "2028-02-29"),
            Row("Dentist 3 may", "Dentist", "2027-05-03"),
            Row("Dentist May 3", "Dentist", "2027-05-03"),
            Row("Dentist sept 5", "Dentist", "2027-09-05"),
            Row("Dentist sep 5", "Dentist", "2027-09-05"),
            Row("Dentist 5 june", "Dentist", "2027-06-05"),
            Row("Dentist 5/1", "Dentist", "2027-01-05"),
            Row("Dentist 5/1", "Dentist", "2027-05-01", dayFirst = false),
            Row("Dentist 12/25", "Dentist", "2026-12-25", dayFirst = false),
            Row("Dentist 25 dec 25 jan", "Dentist 25 jan", "2026-12-25"),
            Row("25 dec dentist", "dentist", "2026-12-25"),
            Row("Dentist 2025-01-01", "Dentist", "2025-01-01"),
            none("Dentist 12/25"),
            none("Dentist 31 apr"),
            none("Dentist 30/2"),
            none("Dentist 32 dec"),
            none("Dentist 2026-02-30"),
            none("March for science"),
            none("May I call back"),
            none("Buy 2 apples"),
            none("Room 101"),
            none("Chapter 11 review"),
            none("Buy 1.5 kg flour"),
            none("Dec"),
        )

        private val times = listOf(
            Row("Call bank 4pm", "Call bank", TODAY, "16:00"),
            Row("Call bank 4 pm", "Call bank", TODAY, "16:00"),
            Row("Call bank 4PM", "Call bank", TODAY, "16:00"),
            Row("Call bank 4:30pm", "Call bank", TODAY, "16:30"),
            Row("Call bank 4.30pm", "Call bank", TODAY, "16:30"),
            Row("Call bank 4:30 pm", "Call bank", TODAY, "16:30"),
            Row("Call bank 4p.m.", "Call bank", TODAY, "16:00"),
            Row("Call bank 4 p.m.", "Call bank", TODAY, "16:00"),
            Row("Call bank at 4pm", "Call bank", TODAY, "16:00"),
            Row("Call bank 16:30", "Call bank", TODAY, "16:30"),
            Row("Call bank 9am", "Call bank", TOMORROW, "09:00"),
            Row("Call bank 11am", "Call bank", TODAY, "11:00"),
            Row("Call bank 10:30am", "Call bank", TOMORROW, "10:30"),
            Row("Call bank 09:30", "Call bank", TOMORROW, "09:30"),
            Row("Call bank 10:30", "Call bank", TODAY, "22:30"),
            Row("Call bank 4:15", "Call bank", TODAY, "16:15"),
            Row("Call bank at 4", "Call bank", TODAY, "16:00"),
            Row("Call bank at 11", "Call bank", TODAY, "11:00"),
            Row("Call bank at 10", "Call bank", TODAY, "22:00"),
            Row("Call bank at 16", "Call bank", TODAY, "16:00"),
            Row("Call bank at 0", "Call bank", TOMORROW, "00:00"),
            Row("Call bank at 4:30", "Call bank", TODAY, "16:30"),
            Row("Call bank at 4.30", "Call bank", TODAY, "16:30"),
            Row("Call bank tomorrow at 4", "Call bank", TOMORROW, "16:00"),
            Row("Call bank tomorrow at 9", "Call bank", TOMORROW, "09:00"),
            Row("Call bank tomorrow at 7", "Call bank", TOMORROW, "07:00"),
            Row("Call bank tomorrow at 11", "Call bank", TOMORROW, "11:00"),
            Row("Call bank tomorrow at 12", "Call bank", TOMORROW, "12:00"),
            Row("Call bank tomorrow at 6", "Call bank", TOMORROW, "18:00"),
            Row("Call bank tomorrow at 1", "Call bank", TOMORROW, "13:00"),
            Row("Call bank today at 4", "Call bank", TODAY, "16:00"),
            Row("Call bank today at 9", "Call bank", TODAY, "21:00"),
            Row("Call bank today at 11", "Call bank", TODAY, "11:00"),
            Row("Call bank at noon", "Call bank", TODAY, "12:00"),
            Row("Call bank noon", "Call bank", TODAY, "12:00"),
            Row("Call bank midnight", "Call bank", TOMORROW, "00:00"),
            Row("Call bank 12am", "Call bank", TOMORROW, "00:00"),
            Row("Call bank 12pm", "Call bank", TODAY, "12:00"),
            Row("Call bank 4pm 5pm", "Call bank 5pm", TODAY, "16:00"),
            Row("Call bank 4pm tomorrow", "Call bank", TOMORROW, "16:00"),
            Row("tomorrow 4pm call bank", "call bank", TOMORROW, "16:00"),
            Row("Call bank friday 9am", "Call bank", FRIDAY, "09:00"),
            Row("Call bank 25 dec 3pm", "Call bank", "2026-12-25", "15:00"),
            Row("Call bank 25 dec 2026 at 3pm", "Call bank", "2026-12-25", "15:00"),
            Row("Call bank 429pm", "Call bank", TODAY, "16:29"),
            Row("Call bank 429 pm", "Call bank", TODAY, "16:29"),
            Row("Call bank 1230pm", "Call bank", TODAY, "12:30"),
            Row("Call bank 1130am", "Call bank", TODAY, "11:30"),
            Row("Call bank 4p", "Call bank", TODAY, "16:00"),
            Row("Call bank 429p", "Call bank", TODAY, "16:29"),
            Row("Call bank 1030a", "Call bank", TOMORROW, "10:30"),
            Row("Call bank 11a", "Call bank", TODAY, "11:00"),
            Row("Call bank tomorrow 4p", "Call bank", TOMORROW, "16:00"),
            Row("Call bank at 429", "Call bank", TODAY, "16:29"),
            Row("Call bank at 1630", "Call bank", TODAY, "16:30"),
            Row("Call bank at 0930", "Call bank", TOMORROW, "09:30"),
            Row("Call bank 1630h", "Call bank", TODAY, "16:30"),
            Row("Call bank 930h", "Call bank", TOMORROW, "09:30"),
            none("Call bank 429"),
            none("Room 429"),
            none("Buy 120 screws"),
            none("Call bank at 470"),
            none("Call bank 1330pm"),
            none("Call bank 4 p"),
            none("Call bank 2500h"),
            none("Call bank 13pm"),
            none("Call bank 0am"),
            none("Call bank 4"),
            none("Call bank 25:00"),
            none("Call bank 4:75"),
            none("Buy 4.30 worth of stamps"),
            none("Meet at the cafe"),
            none("4pm"),
        )

        private val dayParts = listOf(
            Row("Run tomorrow morning", "Run", TOMORROW, "09:00"),
            Row("Run tomorrow afternoon", "Run", TOMORROW, "14:00"),
            Row("Run tomorrow evening", "Run", TOMORROW, "18:00"),
            Row("Run friday evening", "Run", FRIDAY, "18:00"),
            Row("Run 25 dec morning", "Run", "2026-12-25", "09:00"),
            Row("Run this evening", "Run", TODAY, "18:00"),
            Row("Run this morning", "Run", TODAY, "09:00"),
            Row("Run this afternoon", "Run", TODAY, "14:00"),
            Row("Run in the evening", "Run", TODAY, "18:00"),
            Row("Run in the morning", "Run", TOMORROW, "09:00"),
            Row("Run tomorrow in the afternoon", "Run", TOMORROW, "14:00"),
            Row("Evening walk tomorrow", "Evening walk", TOMORROW),
            none("Morning run"),
            none("Afternoon tea with Lea"),
            none("Run this week"),
        )

        private val offsets = listOf(
            Row("Stretch in 30 minutes", "Stretch", TODAY, "11:00"),
            Row("Stretch in 30 min", "Stretch", TODAY, "11:00"),
            Row("Stretch in 30 mins", "Stretch", TODAY, "11:00"),
            Row("Stretch in 30m", "Stretch", TODAY, "11:00"),
            Row("Stretch in 2 hours", "Stretch", TODAY, "12:30"),
            Row("Stretch in 2 hrs", "Stretch", TODAY, "12:30"),
            Row("Stretch in 2h", "Stretch", TODAY, "12:30"),
            Row("Stretch in an hour", "Stretch", TODAY, "11:30"),
            Row("Stretch in one hour", "Stretch", TODAY, "11:30"),
            Row("Stretch in 15 hours", "Stretch", TOMORROW, "01:30"),
            Row("Stretch in 3 days", "Stretch", "2026-11-21"),
            Row("Stretch in 1 day", "Stretch", TOMORROW),
            Row("Stretch in a week", "Stretch", "2026-11-25"),
            Row("Stretch in 2 weeks", "Stretch", "2026-12-02"),
            Row("Stretch in 2w", "Stretch", "2026-12-02"),
            Row("Stretch in 3d", "Stretch", "2026-11-21"),
            Row("Arrive in 3 days at 4pm", "Arrive", "2026-11-21", "16:00"),
            none("Stretch in 0 days"),
            none("Live in the moment"),
            none("Stretch in 5"),
            none("Stretch in a while"),
            none("Check in 2 bags"),
        )

        private val tags = listOf(
            Row("Call bank p1", "Call bank", priority = 1),
            Row("Call bank P2", "Call bank", priority = 2),
            Row("p3 call bank", "call bank", priority = 3),
            Row("Call bank p1 p2", "Call bank p2", priority = 1),
            Row("Call bank p1,", "Call bank", priority = 1),
            Row("Milk #groceries", "Milk", list = "Groceries"),
            Row("Milk #Groceries", "Milk", list = "Groceries"),
            Row("#groceries milk", "milk", list = "Groceries"),
            Row("Mop floor #weekendchores", "Mop floor", list = "Weekend chores"),
            Row("Milk #groceries #events", "Milk #events", list = "Groceries"),
            Row("Milk #daily", "Milk", list = "Daily"),
            none("Call bank p4"),
            none("Call bank p0"),
            none("app1 review"),
            Row("Milk #unknown", "Milk", list = "Unknown", newList = true),
            Row("Flights #nyTrip tomorrow", "Flights", TOMORROW, list = "NyTrip", newList = true),
            Row("Pack #weekend-trip", "Pack", list = "Weekend trip", newList = true),
            none("Issue #42"),
            none("Milk # groceries"),
            none("#groceries"),
            none("p1"),
        )

        /** A date and a time anywhere in the line make one moment; a clock time beats a day part. */
        private val sentences = listOf(
            Row("Take out trash on wednesday evening 9pm", "Take out trash", TODAY, "21:00"),
            Row("Take out trash wednesday evening", "Take out trash", TODAY, "18:00"),
            Row("Wednesday when Sam comes, do the dishes at 9pm", "when Sam comes, do the dishes", TODAY, "21:00"),
            Row("Run tomorrow evening at 7", "Run", TOMORROW, "19:00"),
            Row("Run at 7 in the evening tomorrow", "Run", TOMORROW, "19:00"),
            Row("Call mum evening 8pm", "Call mum", TODAY, "20:00"),
            Row("Dinner tomorrow night", "Dinner", TOMORROW, "21:00"),
            Row("Gym fri 7:30am", "Gym", FRIDAY, "07:30"),
            Row("Gym sat 9am", "Gym", "2026-11-21", "09:00"),
            Row("Call tom at 5", "Call tom", TODAY, "17:00"),
            Row("Morning run 7am", "Morning run", TOMORROW, "07:00"),
            Row("Night shift tomorrow", "Night shift", TOMORROW),
        )

        private val mixed = listOf(
            Row("Call bank tomorrow 4pm p1", "Call bank", TOMORROW, "16:00", priority = 1),
            Row("Milk #groceries tomorrow", "Milk", TOMORROW, list = "Groceries"),
            Row("Concert #events friday 8pm p2", "Concert", FRIDAY, "20:00", priority = 2, list = "Events"),
            Row("p1 #events dec 31 at 11pm party", "party", "2026-12-31", "23:00", priority = 1, list = "Events"),
            Row("Call bank, tomorrow", "Call bank", TOMORROW),
            Row("Call bank tomorrow.", "Call bank", TOMORROW),
            Row("tomorrow: call bank", "call bank", TOMORROW),
            Row("Call bank (tomorrow)", "Call bank", TOMORROW),
            Row("  Call   bank   tomorrow  ", "Call bank", TOMORROW),
            Row("Email Sam about friday's plan tomorrow", "Email Sam about friday's plan", TOMORROW),
            Row("Submit report by friday 5pm", "Submit report", FRIDAY, "17:00"),
            Row("Book flight in 2 weeks p2", "Book flight", "2026-12-02", priority = 2),
            none(""),
            none("Just a plain task"),
        )
    }
}
