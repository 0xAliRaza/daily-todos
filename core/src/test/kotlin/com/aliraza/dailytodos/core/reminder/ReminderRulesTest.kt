package com.aliraza.dailytodos.core.reminder

import com.aliraza.dailytodos.core.model.HeadsUp
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.core.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderRulesTest {
    private val zone = ZoneId.of("Europe/Zurich")
    private val today = LocalDate.of(2026, 11, 18)
    private val yesterday = today.minusDays(1)
    private val tomorrow = today.plusDays(1)
    private val settings = Settings()

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Instant =
        LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(zone).toInstant()

    private fun time(hour: Int, minute: Int = 0) = LocalTime.of(hour, minute)

    @Test
    fun timedTaskRemindsAtItsTime() {
        val t = task(date = today, time = time(16))
        assertEquals(at(today, 16), ReminderRules.next(t, settings, at(today, 8), zone))
    }

    @Test
    fun headsUpMovesTimedRemindersEarlier() {
        val t = task(date = today, time = time(16))
        val early = settings.copy(headsUp = HeadsUp.TEN)
        assertEquals(at(today, 15, 50), ReminderRules.next(t, early, at(today, 8), zone))
    }

    @Test
    fun headsUpCanCrossMidnight() {
        val t = task(date = tomorrow, time = time(0, 5))
        val early = settings.copy(headsUp = HeadsUp.TEN)
        assertEquals(at(today, 23, 55), ReminderRules.next(t, early, at(today, 8), zone))
    }

    @Test
    fun headsUpDoesNotApplyToDateOnlyTasks() {
        val t = task(date = today)
        val early = settings.copy(headsUp = HeadsUp.SIXTY)
        assertEquals(at(today, 10), ReminderRules.next(t, early, at(today, 8), zone))
    }

    @Test
    fun dateOnlyTaskUsesTheDefaultTimeAndFollowsItsChanges() {
        val t = task(date = today)
        assertEquals(at(today, 10), ReminderRules.next(t, settings, at(today, 8), zone))
        val later = settings.copy(defaultReminderTime = time(11, 30))
        assertEquals(at(today, 11, 30), ReminderRules.next(t, later, at(today, 8), zone))
    }

    @Test
    fun dateOnlyTaskHasNoReminderOnItsDayWhenThatIsSwitchedOff() {
        val t = task(date = today)
        val noDateOnly = settings.copy(dateOnlyGetsReminder = false)
        // The overdue reminder is its own setting, so the next morning still reminds.
        assertEquals(at(tomorrow, 9), ReminderRules.next(t, noDateOnly, at(today, 8), zone))
        assertNull(ReminderRules.next(t, noDateOnly.copy(overdueReminders = false), at(today, 8), zone))
    }

    @Test
    fun undatedDoneAndDeletedTasksNeverRemind() {
        val now = at(today, 8)
        assertNull(ReminderRules.next(task(), settings, now, zone))
        assertNull(ReminderRules.next(task(date = today, completedAt = now), settings, now, zone))
        assertNull(ReminderRules.next(task(date = today, deletedAt = now), settings, now, zone))
    }

    @Test
    fun postedReminderIsNotRepeatedTheSameDay() {
        val t = task(date = today, time = time(16), notifiedFor = at(today, 16))
        assertEquals(at(tomorrow, 9), ReminderRules.next(t, settings, at(today, 16, 1), zone))
        assertNull(ReminderRules.due(t, settings, at(today, 23), zone))
    }

    @Test
    fun overdueTaskRemindsEachMorningUntilDone() {
        val t = task(date = yesterday, time = time(16), notifiedFor = at(yesterday, 16))
        assertEquals(at(today, 9), ReminderRules.next(t, settings, at(today, 7), zone))
        assertEquals(at(today, 9), ReminderRules.due(t, settings, at(today, 9), zone))

        val postedToday = t.copy(notifiedFor = at(today, 9))
        assertEquals(at(tomorrow, 9), ReminderRules.next(postedToday, settings, at(today, 9, 1), zone))
    }

    @Test
    fun overdueReminderFollowsItsOwnTimeAndCanBeSwitchedOff() {
        val t = task(date = yesterday, notifiedFor = at(yesterday, 10))
        val custom = settings.copy(overdueReminderTime = time(7, 15))
        assertEquals(at(today, 7, 15), ReminderRules.next(t, custom, at(today, 6), zone))
        assertNull(ReminderRules.next(t, settings.copy(overdueReminders = false), at(today, 6), zone))
    }

    @Test
    fun missedReminderTodayIsCaughtUpAtOnce() {
        // The phone was off at 16:00 and is back at 17:00.
        val t = task(date = today, time = time(16))
        assertEquals(at(today, 16), ReminderRules.due(t, settings, at(today, 17), zone))
    }

    @Test
    fun reminderMissedOnAnEarlierDayBecomesTheOverdueReminder() {
        val t = task(date = yesterday, time = time(16))
        assertNull(ReminderRules.due(t, settings, at(today, 8), zone))
        assertEquals(at(today, 9), ReminderRules.next(t, settings, at(today, 8), zone))
    }

    @Test
    fun severalMissedInstantsFoldIntoOneNotification() {
        val t = task(date = today, time = time(9), snoozedUntil = null)
        val now = at(tomorrow, 10)
        // Its own reminder (today 09:00) is superseded by the overdue one (tomorrow 09:00).
        assertEquals(at(tomorrow, 9), ReminderRules.due(t, settings, now, zone))
    }

    @Test
    fun pendingSnoozeReplacesTheComputedReminder() {
        val t = task(date = today, time = time(10), notifiedFor = at(today, 10), snoozedUntil = at(today, 10, 10))
        assertEquals(at(today, 10, 10), ReminderRules.next(t, settings, at(today, 10, 1), zone))

        val snoozePosted = t.copy(notifiedFor = at(today, 10, 10))
        assertEquals(at(tomorrow, 9), ReminderRules.next(snoozePosted, settings, at(today, 10, 11), zone))
    }

    @Test
    fun snoozeIntoTomorrowWinsOverTheOverdueMorning() {
        val t = task(date = today, time = time(16), notifiedFor = at(today, 16), snoozedUntil = at(tomorrow, 12))
        assertEquals(at(tomorrow, 12), ReminderRules.next(t, settings, at(today, 16, 1), zone))
    }

    @Test
    fun reschedulingToAPastTimeTodayDoesNotPingAtOnce() {
        val typedAtNoon = task(date = today, time = time(8))
        val saved = ReminderRules.rescheduled(typedAtNoon, settings, at(today, 12), zone)
        assertEquals(at(today, 8), saved.notifiedFor)
        assertNull(ReminderRules.due(saved, settings, at(today, 12), zone))
        assertEquals(at(tomorrow, 9), ReminderRules.next(saved, settings, at(today, 12), zone))
    }

    @Test
    fun reschedulingClearsOldMarksSoTheNewTimeFires() {
        val posted = task(date = today, time = time(9), notifiedFor = at(today, 9), snoozedUntil = at(today, 9, 10))
        val moved = ReminderRules.rescheduled(posted.copy(dueTime = time(16)), settings, at(today, 12), zone)
        assertNull(moved.snoozedUntil)
        assertNull(moved.notifiedFor)
        assertEquals(at(today, 16), ReminderRules.next(moved, settings, at(today, 12), zone))
    }

    @Test
    fun aTimeSetInsideTheHeadsUpWindowStillRemindsAtTheTaskTime() {
        val early = settings.copy(headsUp = HeadsUp.TEN)
        val typedAt2331 = task(date = today, time = time(23, 33))
        val saved = ReminderRules.rescheduled(typedAt2331, early, at(today, 23, 31), zone)
        assertNull(ReminderRules.due(saved, early, at(today, 23, 31), zone))
        assertEquals(at(today, 23, 33), ReminderRules.next(saved, early, at(today, 23, 31), zone))

        val posted = saved.copy(notifiedFor = at(today, 23, 33))
        assertEquals(at(tomorrow, 9), ReminderRules.next(posted, early, at(today, 23, 33), zone))
    }

    @Test
    fun aHeadsUpStillAheadRemindsOnceAndNotAgainAtTheTaskTime() {
        val early = settings.copy(headsUp = HeadsUp.TEN)
        val saved = ReminderRules.rescheduled(task(date = today, time = time(16)), early, at(today, 8), zone)
        assertEquals(at(today, 15, 50), ReminderRules.next(saved, early, at(today, 8), zone))

        val posted = saved.copy(notifiedFor = at(today, 15, 50))
        assertEquals(at(tomorrow, 9), ReminderRules.next(posted, early, at(today, 15, 50), zone))
    }

    @Test
    fun raisingTheHeadsUpPastItsEarlyMomentRemindsAtOnce() {
        val t = task(date = today, time = time(16))
        val earlier = settings.copy(headsUp = HeadsUp.THIRTY)
        assertEquals(at(today, 15, 30), ReminderRules.due(t, earlier, at(today, 15, 45), zone))
    }

    @Test
    fun wallClockTimeFollowsTheTimeZone() {
        val t = task(date = today, time = time(16))
        val newYork = ZoneId.of("America/New_York")
        val expected = LocalDateTime.of(today, time(16)).atZone(newYork).toInstant()
        assertEquals(expected, ReminderRules.next(t, settings, at(today, 8), newYork))
    }

    @Test
    fun timeInTheSpringForwardGapFiresAfterTheGap() {
        val gapDay = LocalDate.of(2027, 3, 28)
        val t = task(date = gapDay, time = time(2, 30))
        val expected = LocalDateTime.of(gapDay, time(3, 30)).atZone(zone).toInstant()
        assertEquals(expected, ReminderRules.next(t, settings, at(gapDay.minusDays(1), 12), zone))
    }
}
