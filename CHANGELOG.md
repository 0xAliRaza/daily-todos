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
