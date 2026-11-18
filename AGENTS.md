# AGENTS.md — Daily Tasks

Instructions for AI coding agents working on this app. The folder-level `../AGENTS.md` (how I build my
own apps) applies too; this file adds the app's own decisions. `CLAUDE.md` imports this file.

## What this is

Daily Tasks is a native Android to-do app for one person's daily use: Kotlin and Jetpack Compose, fully
offline, licensed GPL-3.0-or-later. Its promise is that a reminder always fires. Android only; there is no
desktop or web version.

Status on 2026-11-18: M0 to M2 done. The reminder engine (M3), the task screens (M4), reminder health
(M5), the focus timer (M6), lists (M7) and settings with backup (M8) are built and wait for the A54 checks.
On the phone the app is named DT. See the milestones in `PLAN.md`.

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
- Views: Today and Upcoming (with a week strip to jump between days) show dated tasks from every list;
  then each list on its own. Search.
- Calendar: Day, 3 days, Week and Month views of dated tasks. Timed tasks sit at their time on an hour
  grid (tasks have no length); Month shows dots per day and the picked day's tasks below, and folds to
  that day's week as the list scrolls. Reached from its own tab and from Upcoming's week strip.
- Gestures: swipe a task right to reschedule (today, tomorrow, next week, a picked date, no date), left
  to delete; both undo. Tap opens a task, long-press starts multi-select, the round box completes.
- Lists show their open counts; a list's menu renames it or deletes it with its tasks (undo right after).
- Undo on every action, multi-select, drag to reorder.
- Sorting: inside a list, manual order with new tasks added at the bottom. In Today, overdue first, then
  timed tasks by time, then the rest in manual order.

**Adding a task**

- One line of natural language carries the date, time, priority and list ("Call bank tomorrow 4pm p1",
  "Milk #groceries"). Date and time parsing is the most important part of the app.
- Recognised words are highlighted while typing; tapping one un-recognises it.
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

- Focus and break rounds, started from a task (runs until the task is done) or on its own.
- The start screen has quick adjusters for focus length and break length, and a switch to turn breaks off.
- End signal: sound, vibrate, or both. Vibrate-only matters for office use. The signal behaves like an
  alarm, not a single blip: it keeps going until dismissed, with a cap so it stops by itself.
- The end of each round is scheduled like a reminder, so it fires even if the app is killed. The countdown
  on screen is only a display.
- Built right after tasks and reminders are solid.

**Data**

- Private SQLite database on the device.
- Export and Import in settings, on demand, as one JSON file. Import accepts only our own backups.
- Settings shows when the last backup was made. No backup nagging, no automatic backups.
- Android's cloud backup of app data is switched off.

**Design**

- One red accent on plain neutrals: white in light, near-black grey in dark; both first-class; system font.
- Signal colours, not extra accents: the check-box ring shows priority (P1 red, P2 orange, P3 blue) and a
  due date is coloured by nearness (overdue red, today green, tomorrow amber, this week purple).
- Flat lists split by hairlines, no cards. Adding a task starts from a round + button and is one
  line: the text, one pill for the due day, and send. Every pairing stays at WCAG AA.

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

- Application id: `com.aliraza.dailytasks`.
- Minimum Android 8.0 (API 26).
- Released on GitHub; F-Droid maybe later. Not planned for Google Play.
