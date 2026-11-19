# Changelog

All notable changes are listed here. The format follows Keep a Changelog, and versions follow semantic
versioning.

## Unreleased

### Changed

- The app is now called Daily Todos, with the id `com.aliraza.dailytodos`. Backups made before the
  rename still import.
- Screens slide in and out, and the back gesture previews the page underneath.
- One date sheet for rescheduling, the add bar's Date chip and the task sheet.
- A clearer Settings screen; every choice uses the same pill control.
- Reminder health shows its status at the top and late reminders in amber.
- A swiped task row never stays half open; a quick flick opens the timer or deletes.
- Empty screens centre above the keyboard instead of being cut off by it, Search included.
- Smoother motion: a swipe past the first or last tab stretches and springs back, the pill controls
  slide to a new choice, and the + button, the add bar, the tab bar and the selection bar animate in
  and out.
- The default list says so in words under its name.

### Added

- A 12-hour or 24-hour clock (12-hour by default).
- Weeks start on the phone's first day by default, or on Monday or Sunday.
- Haptic feedback for completing a task and the timer's controls, with a switch to turn it off.
- The calendar shows what was done on a picked day and how long each task was focused.
- A progress line on the now-focusing bar.
- Drag around the focus dial to set the length, as well as with − and +.
- The add bar starts with the open list's tag and the picked day, so they show in the line.
- Swipe sideways on an empty area to switch tabs; the page follows the finger and tabs slide in order.
- The time picker opens straight from the task sheet, can switch to typing, and can remove the time.
- Times typed without a colon: "429pm", "4p", "1030a", "at 1630" and "1630h". A bare "429" stays text.

## 0.1.0 — 2026-11-18

The first release.

### Added

- One-line quick add that reads the date, time, priority and list as you type ("Call bank tomorrow
  4pm p1", "Milk #groceries"), with recognised words highlighted and tap-to-undo; pickers for date,
  priority and list; a new "#tag" makes a new list.
- Lists with open counts, rename and delete; one built-in "Daily" list; sub-tasks; three priorities.
- Today (overdue first, with "Reschedule" for all of them) and Upcoming with a week strip and a Day,
  3-day, Week and Month calendar in its own tab; search; swipe right to focus on a task, left to delete
  it; multi-select with Reschedule; drag to reorder; undo on every action.
- Reminders that always fire: exact alarms, one notification per task with Done and snooze, early
  heads-up, overdue reminders on following mornings, catch-up after the phone was off, re-arming after
  restart, update and clock changes, and reminders before the first unlock.
- Reminder health: permission and battery checks with steps per phone maker, a test reminder, and a
  history of when each reminder was due and when it fired.
- Focus timer for each task: focus and break rounds whose ends are scheduled like reminders, nothing
  starting by itself, and the time spent kept even when stopped early. A now-focusing bar shows the
  running round on every screen. Ends signal with the phone's notification sound, a long vibration, or
  both; notifications name the task and offer the next step.
- Focus stats: the all-time total, today, this week, sessions, a 7-day chart, the top tasks, and a
  comparison with something that takes a known time.
- Settings, and export and import of everything as one JSON file (format version 2, which includes focus
  sessions; see `docs/backup-format.md`).
- Light and dark themes with one ink-blue accent, and a focus-dial app icon.
