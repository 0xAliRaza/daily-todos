# AGENTS.md — Daily Todos

Instructions for AI coding agents working on this app. The folder-level `../AGENTS.md` (how I build my
own apps) applies too; this file adds the app's own decisions. `CLAUDE.md` imports this file.

## What this is

Daily Todos is a native Android to-do app for one person's daily use: Kotlin and Jetpack Compose, fully
offline, licensed GPL-3.0-or-later. Its promise is that a reminder always fires. Android only; there is no
desktop or web version.

Status on 2026-11-18: M0 to M2 done. The reminder engine (M3), the task screens (M4), reminder health
(M5), the focus timer (M6), lists (M7) and settings with backup (M8) are built and wait for the A54 checks.
See the milestones in `PLAN.md`.

## Start here

- `PLAN.md` is the implementation plan: environment, stack, data model, the reminder engine design,
  milestones with their acceptance checks, and the decisions still waiting on me. Build from it, tick its
  milestones, and keep it true to the code.
- This file wins over the plan where the two disagree; if they do, fix the plan.

## Decided scope

**Tasks**

- Lists: simple named groups (Groceries, Events). One built-in default list, "Daily", holds every task not
  filed elsewhere; it can be renamed but not deleted. No nesting, no sections.
- Sub-tasks: one level of checkboxes inside a task.
- Three priorities, optional. No labels.
- Views: Today and Upcoming show dated tasks from every list; then each list on its own. Search.
  Upcoming lists the days ahead under a week strip (month, week arrows, the seven days from the week start with dots for days
  with tasks); tapping a day jumps the list to it.
- Sub-tasks show under their task by default; the "1/3" chevron folds them.
- Calendar: Day, 3 days, Week and Month views of dated tasks. Timed tasks sit at their time on an hour
  grid (tasks have no length); Month shows dots per day (grey for done tasks) and the picked day's tasks
  below, then what was done that day with the time focused on each, and folds to that day's week as the
  list scrolls. Past days are greyed.
- Four tabs: Today, Upcoming, Calendar, Lists. Upcoming and Calendar are separate tabs.
- Gestures: swipe a task right to open its focus timer, left to delete it (with undo). Tap opens a task,
  long-press starts multi-select, the round box completes.
- Rescheduling: the task sheet's date row, the Reschedule button in multi-select, and the overdue header
  in Today. Each offers today, tomorrow, next week, a picked date or no date, with undo. The add bar's
  Date chip and the task sheet use the same date sheet.
- Lists show their open counts; a list's menu renames it or deletes it with its tasks (undo right after).
- Undo on every action, multi-select, drag to reorder. Drag handles show only in multi-select.
- Sorting: inside a list, manual order with new tasks added at the bottom. In Today, overdue first (its
  header reschedules them all at once), then timed tasks by time, then the rest in manual order.

**Adding a task**

- One line of natural language carries the date, time, priority and list ("Call bank tomorrow 4pm p1",
  "Milk #groceries"). Date and time parsing is the most important part of the app.
- Recognised words are highlighted while typing; tapping one un-recognises it.
- Date, priority and list can also be picked by hand from chips under the line; a pick writes the same
  words into it ("25 nov", "p1", "#groceries"), so the line stays the one source of truth.
- A "#name" that matches no list creates that list when the task is added ("#weekend-trip" makes
  "Weekend trip"); numbers alone ("Issue #42") stay text.
- A task with a date and no time gets the default reminder time (10:00 unless changed).
- A task with no date, added to the default list, gets tomorrow at the default time. It shows as a chip
  while adding, so it can be tapped away. Tasks added to any other list stay undated, so a grocery list
  does not fire a reminder per item. Both defaults can be changed or switched off in settings.

**Reminders**

- One notification per task, with Complete and Snooze. Never a summary or overview notification of our own.
  (Android stacking several notifications in the shade by itself is acceptable.)
- Early heads-up: one setting makes timed tasks remind 0, 10, 30 or 60 minutes before their time. There is
  no per-task choice.
- Overdue: a task left undone reminds again on the following mornings, one notification per task, at its
  own overdue time (separate from the default reminder time). The time can be changed and the whole thing
  switched off in settings.
- A reminder must never be missed because of battery saving: exact scheduling through the system alarm
  path, re-registered after reboot, app update, and time or time-zone change.
- Settings has a direct "Battery optimisation" row that opens the phone's battery setting for this app.
- A "Reminder health" screen checks every permission and battery setting, shows the steps for the phone's
  maker (Samsung, Xiaomi, Oppo, Vivo, OnePlus, Huawei, Tecno, Infinix, with a generic fallback) and can send
  a test reminder one minute out.
- A reminder history on that screen lists recent reminders with when each was due and when it actually
  fired, so a late or missed one can be seen and diagnosed. It stays on the device and stores no titles.
- Optional setting: let reminders and timer ends break through Do Not Disturb.

**Focus timer**

- The timer always belongs to one task. Swiping a task right opens that task's timer, a screen of its
  own with back; leaving it never stops the timer. The task sheet has a Focus row ("Focused 1 h 20 min ·
  4 sessions") with a play button that opens it too.
- Only focus and break. No long break, no round numbers, no switch to turn breaks off. Nothing starts by
  itself: each step waits for a tap.
- Ready to focus: the task's title with its round check box, the dial, presets 15, 25 and 50 minutes and
  a fourth chip with the last length stepped to that is not a preset (90 until then), with a stepper, a Sound / Vibrate / Both switch, the task's focused time, and one Start focus button. If
  another task's timer runs, a note says starting here ends it.
- Focusing: Pause or Resume, and Stop. On a break: Pause or Resume, and End break.
- Break time, after focus ends: "25 min of focus done", break presets 5, 10 and 15 minutes and the same
  kind of fourth chip (20 until then), with a stepper, Skip break and Start break. The end or skip of a break goes back to Ready.
- Stopping focus early keeps the time spent, if it is 1 minute or more. Completing the task from the timer
  ticks it off (with undo) and closes the screen. Completing or deleting the task anywhere stops its timer
  quietly and keeps the time spent.
- A now-focusing bar sits above the tab bar, and at the bottom of other screens, while a round runs or a
  break waits: the task's title and the countdown, in the accent for focus and green for a break ("Break
  time" while the break waits). Tapping it returns to the timer.
- End signal: Sound and Both play the phone's default notification sound three times at a focus end and
  twice at a break end; Both adds a short buzz to each sound. Vibrate-only gives long full-strength buzzes
  in two groups: three and three at a focus end, two and two at a break end. Longer than a notification,
  not an alarm. Vibrate-only matters for office use.
- With the phone in vibrate mode the long buzz plays whatever is picked; in silent mode it stays quiet.
  The Do Not Disturb override plays it as an alarm regardless.
- Notifications name the task. While a round runs: "Focus · ends at 11:42" (or Break) with a live
  countdown, Pause and Stop (or End break). At a focus end: Start break and Skip break. At a break end:
  Start focus. The end alerts have no Stop. An alert whose next step no longer applies is removed.
- Keep screen on: one setting, off by default, switched in the Focus group or by the cup on the timer
  screen. While on, the screen stays awake on any of the app's screens as long as focus or a break counts
  down; paused, waiting or idle, the phone sleeps as usual. Only while the app is in front; no wake lock.
- Settings has a Focus group: Focus stats, the end signal choice and Keep screen on. The Do Not Disturb
  override stays with Reminders.
- Focus stats: the all-time total, Today, This week and Sessions, a 7-day bar chart, the top 5
  tasks (deleted ones grouped as "Deleted tasks"), and one comparison from bundled facts ("That's as long
  as 72 journeys of sunlight to Earth"). Plain statistics, no points or streaks.
- The end of each round is scheduled like a reminder, through the one scheduling path, so it fires even
  if the app is killed. The countdown on screen is only a display.

**Data**

- Private SQLite database on the device.
- Export and Import in settings, on demand, as one JSON file. Import accepts only our own backups.
- Settings shows when the last backup was made. No backup nagging, no automatic backups.

**Settings**

- Groups: Reminders, Reliability (Reminder health with its status, Battery optimisation), Focus, General
  and Your data. Every choice is the same pill control; on/off settings are switches.
- Focus: Focus stats, the end signal, and Keep screen on (off by default).
- General: theme; time format, 12-hour by default; week starts on, following the phone by default, or
  Monday or Sunday; haptic feedback, on by default, for completing a task and the timer's controls.
- Android's cloud backup of app data is switched off.

**Design**

- One ink-blue accent on plain neutrals: white in light, near-black grey in dark; both first-class; system font.
  Red is kept for what is urgent: overdue, P1 and deleting.
- Signal colours, not extra accents: the check-box ring shows priority (P1 red, P2 orange, P3 blue); a due
  date is red when overdue, the accent for today, and grey after that.
- Flat lists split by hairlines, no cards. Adding a task starts from a round + button and is one
  line: the text, a pill for the default day (only when no date is typed), and send. Every pairing stays at WCAG AA.

## Rules that must hold

- The manifest never declares the internet permission. Make this a test.
- Export then import restores everything without loss. Make this a test.
- Every reminder and every timer end goes through the one scheduling path; no second mechanism.
- Don't name, mention or compare against any commercial to-do app in code, docs, screenshots or posts.
- Nothing goes public (remote repo, push, release, post) without my explicit yes for that action.
- Test device is a Samsung Galaxy A54. When a session's work is done, stop the emulator, adb and Gradle
  daemons it started.

## Non-goals

Repeating tasks (habits belong in a habit app), labels, nested projects and sections, task durations, a global quick-add or home-screen
shortcut flow, keyboard shortcuts, encryption, sync, accounts, collaboration, AI features, points or
streaks, location reminders, importers for other apps' files, desktop and web versions.

## Still open

- The questions listed at the end of `PLAN.md`.

## Platform and release

- Application id: `com.aliraza.dailytodos` (renamed from Daily Tasks; its backups still import).
- Minimum Android 8.0 (API 26).
- Released on GitHub; F-Droid maybe later. Not planned for Google Play.
