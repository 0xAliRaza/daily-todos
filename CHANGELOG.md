# Changelog

All notable changes are listed here. The format follows Keep a Changelog, and versions follow semantic
versioning.

## Unreleased

### Added

- Project skeleton: the `:core` and `:app` modules, design tokens, an empty Today screen, the verify,
  device-test and stop-dev scripts, and the permission tripwire.
- Quick-add parser for dates, times, priorities and lists, with highlighting ranges and tap-to-exclude.
- Reminder rules: default and overdue times, early heads-up, snooze, catch-up of missed reminders.
- Sort rules for Today, Upcoming days and lists.
- Local storage: tasks, lists with the default "Daily" list, sub-tasks, settings, reminder history and
  timer state, with soft delete for undo.
- Reminder engine: one exact alarm-clock alarm for the earliest reminder, one notification per task with
  Done, 10 min and 1 hour, catch-up after the phone was off, re-arming after restart, app update and
  clock or time-zone changes, and reminders from a device-protected copy before the first unlock.
- Backup format (version 1, documented in `docs/backup-format.md`) with export, strict import checks and
  a full replace on import. The settings screen and file pickers come later.
