# Changelog

All notable changes are listed here. The format follows Keep a Changelog, and versions follow semantic
versioning.

## Unreleased

## 0.1.0 — 2026-11-18

The first release.

### Added

- One-line quick add that reads the date, time, priority and list as you type ("Call bank tomorrow
  4pm p1", "Milk #groceries"), with recognised words highlighted and tap-to-undo; pickers for date,
  priority and list; a new "#tag" makes a new list.
- Lists with open counts, rename and delete; one built-in "Daily" list; sub-tasks; three priorities.
- Today (overdue first, with "Reschedule" for all of them) and Upcoming with a week strip and a Day,
  3-day, Week and Month calendar; search; swipe to reschedule or delete; multi-select; drag to reorder;
  undo on every action.
- Reminders that always fire: exact alarms, one notification per task with Done and snooze, early
  heads-up, overdue reminders on following mornings, catch-up after the phone was off, re-arming after
  restart, update and clock changes, and reminders before the first unlock.
- Reminder health: permission and battery checks with steps per phone maker, a test reminder, and a
  history of when each reminder was due and when it fired.
- Focus timer with focus and break rounds whose ends are scheduled like reminders, ending with sound,
  vibration or both until dismissed.
- Settings, and export and import of everything as one JSON file (format version 1, see
  `docs/backup-format.md`).
- Light and dark themes with one ink-blue accent.
