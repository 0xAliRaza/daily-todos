# Changelog

All notable changes are listed here. The format follows Keep a Changelog, and versions follow semantic
versioning.

## Unreleased

### Changed

- A new look: a bolder type scale, a large title that collapses on scroll, one outlined icon set, one
  filled selection style everywhere, sheets with handles that open to full height, and a new app icon,
  the through-ring.
- Pick an accent (ink blue, violet, teal or graphite) and a pure black background for the dark theme.
- Reminders read better: the task's title, then when and which list, with Complete and Snooze (10 min).
- Today pins Completed at the bottom when everything is done, with "See tomorrow". Upcoming folds empty
  days and groups everything after the week under Later. The calendar gets a "No time" row.
- Multi-select can move tasks to another list and select all. Undo names the task.
- The add bar grows to three lines and its Date chip shows the date and time it understood.
- Sub-tasks start folded in lists.

### Fixed

- The test reminder no longer adds a task to the Daily list.
- "yesterday" and "next week" are understood when adding a task.
- "Week starts on" no longer shows the phone's day twice; the date picker follows it on every Android
  version.

## 0.1.0 — 2026-11-19

The first release.

### Added

- One-line quick add that reads the date, time, priority and list as you type ("Call bank tomorrow
  4pm p1", "Milk #groceries"), with recognised words highlighted and tap-to-undo; pickers for date,
  priority and list; a new "#tag" makes a new list.
- Lists with open counts, rename and delete; one built-in "Daily" list; sub-tasks; three priorities.
- Today (overdue first, with "Reschedule" for all of them) and Upcoming with a week strip and a Day,
  3-day, Week and Month calendar in its own tab; search; swipe right to focus on a task, left to delete
  it; multi-select with Reschedule; drag to reorder; undo on every action. Swipe sideways on an empty
  area to move between tabs. Times typed without a colon ("429pm", "4p", "1030a", "at 1630") work too.
- The calendar shows what was done on a picked day and how long each task was focused.
- Reminders that always fire: exact alarms, one notification per task with Done and snooze, early
  heads-up, overdue reminders on following mornings, catch-up after the phone was off, re-arming after
  restart, update and clock changes, and reminders before the first unlock.
- Reminder health: permission and battery checks with steps per phone maker, a test reminder, and a
  history of when each reminder was due and when it fired.
- Focus timer for each task: focus and break rounds whose ends are scheduled like reminders, nothing
  starting by itself, and the time spent kept even when stopped early. A now-focusing bar shows the
  running round on every screen. Ends signal with the phone's notification sound, a long vibration, or
  both; notifications name the task and offer the next step. Drag around the dial to set a round's
  length. Caffeinated keeps the screen on while a round counts down.
- Focus stats: the all-time total, today, this week, sessions, a 7-day chart, the top tasks, and a
  comparison with something that takes a known time.
- Settings: a 12-hour or 24-hour clock, the week start (the phone's, Monday or Sunday), haptic feedback,
  and export and import of everything as one JSON file (format version 2, which includes focus sessions;
  see `docs/backup-format.md`).
- Light and dark themes with one ink-blue accent, and a focus-dial app icon.
