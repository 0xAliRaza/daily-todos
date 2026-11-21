# AGENTS.md — Daily Todos

Guidance for AI coding agents and contributors. `CLAUDE.md` imports this file.

## What this is

Daily Todos is a native Android to-do app for one person's daily use: Kotlin and Jetpack Compose, fully
offline, licensed GPL-3.0-or-later. Its promise is that a reminder always fires. Android only.

- `PLAN.md` is the implementation plan: stack, data model, the reminder engine design, milestones with
  their acceptance checks, the decision log and open questions. Keep it true to the code.
- This file wins where the two disagree; then fix the plan.

## Principles

- **Only what is needed.** A feature earns its place by being used. No speculative features, settings or
  abstractions; see the non-goals below.
- **Offline and private.** No network access, accounts, telemetry, analytics or update checks. Data stays in
  a private database on the device and leaves it only through the user's own export.
- **Native.** Kotlin and Compose, Android's own scheduling, notifications and settings screens.
- **Designed, not left over.** Empty, loading and error states are designed. Light, dark and black are all
  first-class, every pairing meets WCAG AA, and reduced motion is respected.

## Commands

All scripts run under bash (`scripts/env.sh` sets the JDK and Android SDK paths).

| Command | What it does |
| --- | --- |
| `bash scripts/verify.sh` | Every check: detekt, unit tests (`:core` and `:app`), lint, debug build. Run before every commit. |
| `bash scripts/device-test.sh` | Instrumented tests on a connected phone or emulator. |
| `bash scripts/stop-dev.sh` | Stops emulators, the adb server and the Gradle and Kotlin daemons. |
| `bash scripts/release.sh` | Signed release APK, after every check passes. Needs the maintainer's key. |

detekt and lint run with no baseline: every finding fails the build.

## Layout

- `core/`: pure Kotlin, no Android. The quick-add parser (`parse`), models, backup format and codec
  (`backup`), sorting, time and the focus and reminder rules.
- `app/`: the Android app.
  - `data/`: Room database, migrations and repositories. Schemas live in `app/schemas/`.
  - `reminder/`: the one scheduling path (`ReminderScheduler`), alarms, receivers and notifications.
  - `ui/`: Compose screens by area (`tasks`, `calendar`, `focus`, `settings`), shared `components`, and
    `theme` (`ColorTokens.kt` is the only place colours are defined).
- `docs/backup-format.md`: the backup file format.

## Decided scope

**Tasks**

- Lists: simple named groups (Groceries, Events). One built-in default list, "Daily", holds every task not
  filed elsewhere; it can be renamed but not deleted. No nesting, no sections.
- Sub-tasks: one level of checkboxes inside a task. A row folds them behind a "1/3" count that opens them
  in place; they are added only in the task sheet.
- Three priorities, optional. No labels.
- Four tabs: Today, Upcoming, Calendar, Lists. Today and Upcoming show dated tasks from every list; each
  list also has its own screen. Search.
- Upcoming lists the next seven days under a week strip (month, week arrows, the seven days from the week
  start with dots for days with tasks). Runs of empty days fold into one quiet line, and everything after
  the seventh day sits in a "Later" group. Tapping a day jumps the list to it.
- Calendar: Day, 3 days, Week and Month views of dated tasks. Timed tasks sit at their time on an hour grid
  (tasks have no length); tasks with a date and no time sit in a "No time" row above it. Month shows dots
  per day (grey for done tasks) and the picked day's tasks below, then what was done that day with the
  time focused on each, and folds to that day's week as the list scrolls. Past days are greyed.
- Gestures: swipe a task right to open its focus timer, left to delete it (with undo). Tap opens a task,
  long-press starts multi-select, the round box completes.
- Rescheduling: the task sheet's date row, the Reschedule button in multi-select, and the overdue header
  in Today. Each offers today, tomorrow, next week, a picked date or no date, with undo. The add bar's Date
  chip and the task sheet use the same date view.
- Lists show their open counts; a list's menu renames it or deletes it with its tasks (undo right after).
- Undo on every action, naming the task when there is one.
- Multi-select: Select all, and Complete, Reschedule, Move to a list and Delete in a bar that replaces the
  tabs. Drag to reorder; drag handles show only in multi-select.
- Sorting: inside a list, manual order with new tasks added at the bottom. In Today, overdue first (its
  header reschedules them all at once), then timed tasks by time, then the rest in manual order.

**Adding a task**

- One line of natural language carries the date, time, priority and list ("Call bank tomorrow 4pm p1",
  "Milk #groceries"). Date and time parsing is the most important part of the app.
- Recognised words are highlighted while typing; tapping one un-recognises it.
- Date, priority and list can also be picked from chips under the line. A pick writes the same words into
  it ("25 nov", "p1", "#groceries"), so the line stays the one source of truth. The Date chip shows the
  resolved date and time.
- A "#name" that matches no list creates that list when the task is added ("#weekend-trip" makes
  "Weekend trip"); numbers alone ("Issue #42") stay text.
- A task with a date and no time gets the default reminder time (10:00 unless changed).
- A task with no date, added to the default list, gets tomorrow at the default time. It shows as a chip
  while adding, so it can be tapped away. Tasks added to any other list stay undated, so a grocery list
  does not fire a reminder per item. Both defaults can be changed or switched off in settings.

**Reminders**

- One notification per task: the task's title, then when and which list ("Today 4:00 PM · Work", "In 10 min
  · 11:30 AM · Work", "Overdue · Tue 9:00 AM · Daily"), with Complete and Snooze (a fixed 10 minutes).
  Never a summary notification of our own; Android stacking several in the shade is fine.
- Early heads-up: one setting makes timed tasks remind 10, 30 or 60 minutes early, or not (Off). There is
  no per-task choice.
- Overdue: a task left undone reminds again on the following mornings, one notification per task, at its
  own overdue time (separate from the default reminder time). The time can be changed and the whole thing
  switched off in settings.
- A reminder must never be missed because of battery saving: exact scheduling through the system alarm
  path, re-registered after reboot, app update, and time or time-zone change.
- Settings has a direct "Battery optimisation" row that opens the phone's battery setting for this app.
- "Reminder health" checks every permission and battery setting, puts what needs fixing first, shows the
  steps for the phone's maker (Samsung, Xiaomi, Oppo, Vivo, OnePlus, Huawei, Tecno, Infinix, with a
  generic fallback) and can send a test reminder one minute out. The test goes through the one scheduling
  path and adds nothing to the lists.
- Its reminder history lists recent reminders with when each was due and when it fired, so a late or
  missed one can be diagnosed. It stays on the device and stores no titles; the screen looks titles up
  live and shows "Deleted task" for one that is gone.
- Optional setting: let reminders and timer ends break through Do Not Disturb.

**Focus timer**

- The timer always belongs to one task. Swiping a task right opens that task's timer, a screen of its own
  with back; leaving it never stops the timer. The task sheet shows the focused time ("Focused 1 h 20 min ·
  4 rounds") and a Focus button that opens it too.
- Only focus and break. No long break, no round goals, no switch to turn breaks off. Nothing starts by
  itself: each step waits for a tap.
- Ready: the task's title with its check box, the dial, presets 15, 25 and 50 minutes and a fourth with the
  last non-preset length (90 until then), a stepper, a Sound / Vibrate / Both switch, when it would end,
  and one Start focus button. If another task's timer runs, a note says starting here ends it.
- Focusing: Pause or Resume, and Stop. The dial empties as the round runs.
- Break, after focus ends: "Round done · take a break", presets 5, 10 and 15 minutes and the same kind of
  fourth (20 until then), a stepper, Skip break and Start break. The screen turns break green. The end or
  skip of a break goes back to Ready.
- Stopping focus early keeps the time spent, if it is 1 minute or more. Completing the task from the timer
  ticks it off (with undo) and closes the screen. Completing or deleting the task anywhere stops its timer
  quietly and keeps the time spent.
- A now-focusing bar sits above the tab bar, and at the bottom of other screens, while a round runs or a
  break waits: the task's title and the countdown, in the accent for focus and green for a break, with
  Pause on a focus round and Skip on a break. Tapping it returns to the timer.
- End signal: Sound and Both play the phone's default notification sound three times at a focus end and
  twice at a break end; Both adds a short buzz to each. Vibrate-only gives long full-strength buzzes in two
  groups: three and three at a focus end, two and two at a break end. Longer than a notification, not an
  alarm, so it works in a quiet office.
- In vibrate mode the buzz plays whatever is picked; in silent mode it stays quiet. The Do Not Disturb
  override plays it as an alarm regardless.
- Notifications name the task. While a round runs: "Focus · ends at 11:42" (or Break) with a live
  countdown, Pause and Stop (or End break). At a focus end: Start break and Skip break. At a break end:
  Start focus. The end alerts have no Stop. An alert whose next step no longer applies is removed.
- Screen on: one setting, off by default, switched in Settings or by the "Screen on" chip on the timer.
  While on, the screen stays awake on the app's screens as long as focus or a break counts down; paused,
  waiting or idle, the phone sleeps as usual. Only while the app is in front; no wake lock.
- Focus stats: the all-time total, Today, This week and Sessions, a 7-day bar chart, the top 5 tasks
  (deleted ones grouped as "Deleted tasks"), and one comparison from bundled facts ("That's as long as 72
  journeys of sunlight to Earth"). Plain statistics, no points or streaks.
- The end of each round is scheduled like a reminder, through the one scheduling path, so it fires even
  if the app is killed. The countdown on screen is only a display.

**Data**

- Private SQLite database on the device. Android's cloud backup of app data is switched off.
- Export and Import in settings, on demand, as one JSON file (`docs/backup-format.md`). Import accepts only
  the app's own backups.
- Settings shows when the last backup was made. No backup nagging, no automatic backups.

**Settings**

- Groups: Reminders, Reliability (Reminder health with its status, Battery optimisation), Focus,
  Appearance, General and Your data. A "Reminders may be late" banner leads when a health check fails.
- Every choice is the same pill control; on/off settings are switches.
- Focus: Focus stats, the end signal and Screen on. The Do Not Disturb override stays with Reminders.
- Appearance: theme (System, Light, Dark), a black background for the dark theme, and the accent (ink
  blue, violet, teal or graphite).
- General: time format, 12-hour by default; week starts on, following the phone by default, or Monday or
  Sunday; haptic feedback, on by default, for completing a task and the timer's controls.

**Design**

- One accent on plain neutrals: white in light, near-black grey in dark, or pure black for OLED; system
  font. The accent is chrome only (buttons, the add button, selection, the active tab, links, the today
  marker, the now line) and never colours a task's ring, date or calendar block.
- Signal colours, not extra accents: the check-box ring shows priority (P1 red, P2 orange, P3 blue). A due
  date is red when overdue, ink in semi-bold for today, and grey after that; a time already passed today
  stays grey. Red is kept for overdue and deleting. No priority and done are neutral.
- Ticking a task flashes its ring in the accent, then it settles to the done grey.
- Flat lists split by hairlines, no cards. Adding a task starts from a round + button and is one line: the
  text, chips for what was understood, and send.

## Rules that must hold

Where a rule names a test, that test enforces it; a rule that can be checked by a machine should get one.

- The manifest never declares the internet permission (`PermissionTripwireTest`).
- Export then import restores everything without loss (`BackupRoundTripTest`). New backup fields are
  optional with defaults so older files still import.
- Every reminder and every timer end goes through the one scheduling path; no second mechanism.
- Time zone is read only through `DeviceClock` (`DeviceClockTest`).
- The app's name lives only in the `app_name` string; other strings take it as an argument
  (`AppNameTripwireTest`).
- No other to-do app is named, mentioned or compared against in code, docs, screenshots or posts.
- Every colour pairing meets WCAG AA in every accent and mode (`ThemeContrastTest`).
- Database changes ship with a migration and a `MigrationTest` case.
- Screenshots and tests use made-up data only.
- Agents never push, tag, release or publish; the maintainer does. Stop any emulator, adb server or Gradle
  daemon a session started when it is done (`scripts/stop-dev.sh`).

## Non-goals

Repeating tasks (habits belong in a habit app), labels, nested projects and sections, task durations, a
global quick-add or home-screen shortcut flow, keyboard shortcuts, encryption, sync, accounts,
collaboration, AI features, points or streaks, location reminders, importers for other apps' files, and
desktop or web versions.

## Platform and release

- Application id: `com.aliraza.dailytodos` (renamed from Daily Tasks; its backups still import).
- Minimum Android 8.0 (API 26).
- Released as an APK on GitHub; F-Droid maybe later. Not planned for Google Play.
