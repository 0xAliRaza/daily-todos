# AGENTS.md — Daily Tasks

Instructions for AI coding agents working on this app. The folder-level `../AGENTS.md` (how I build my
own apps) applies too; this file adds the app's own decisions. `CLAUDE.md` imports this file.

## What this is

Daily Tasks is a native Android to-do app for one person's daily use: Kotlin and Jetpack Compose, fully
offline, licensed GPL-3.0-or-later. Its promise is that a reminder always fires. Android only; there is no
desktop or web version.

Status on 2026-11-18: decisions and plan only, no code yet.

## Start here

- `PLAN.md` is the implementation plan: environment, stack, data model, the reminder engine design,
  milestones with their acceptance checks, and the decisions still waiting on me. Build from it, tick its
  milestones, and keep it true to the code.
- This file wins over the plan where the two disagree; if they do, fix the plan.

## Decided scope

**Tasks**

- Flat, optional lists. No nesting, no sections. A task without a list sits in the Inbox.
- Sub-tasks: one level of checkboxes inside a task.
- Three priorities, optional. No labels.
- Views: Today, Upcoming (with a week strip to jump between days), Inbox and the lists. Search.
- Undo on every action, multi-select, drag to reorder.
- Sorting: inside a list, manual order with new tasks added at the bottom. In Today, overdue first, then
  timed tasks by time, then the rest in manual order.

**Adding a task**

- One line of natural language carries the date, time and priority ("Call bank tomorrow 4pm p1").
  Date and time parsing is the most important part of the app.
- Recognised words are highlighted while typing; tapping one un-recognises it.
- A task with a date and no time gets the default reminder time (10:00 unless changed).
- A task with no date gets tomorrow at the default time. It shows as a chip while adding, so it can be
  tapped away. Both defaults can be changed or switched off in settings.

**Reminders**

- One notification per task, with Complete and Snooze. Never a summary or overview notification of our own.
  (Android stacking several notifications in the shade by itself is acceptable.)
- A reminder must never be missed because of battery saving: exact scheduling through the system alarm
  path, re-registered after reboot, app update, and time or time-zone change.
- A "Reminder health" screen checks every permission and battery setting, shows the steps for the phone's
  maker (Samsung, Xiaomi, Oppo, Vivo, OnePlus, Huawei, Tecno, Infinix, with a generic fallback) and can send
  a test reminder one minute out.
- Optional setting: let reminders and timer ends break through Do Not Disturb.

**Focus timer**

- Focus and break rounds, started from a task (runs until the task is done) or on its own.
- The start screen has quick adjusters for focus length and break length, and a switch to turn breaks off.
- End signal: sound, vibrate, or both. Vibrate-only matters for office use.
- The end of each round is scheduled like a reminder, so it fires even if the app is killed. The countdown
  on screen is only a display.
- Built right after tasks and reminders are solid.

**Data**

- Private SQLite database on the device.
- Export and Import in settings, on demand, as one JSON file. Import accepts only our own backups.
- Settings shows when the last backup was made. No backup nagging, no automatic backups.
- Android's cloud backup of app data is switched off.

**Design**

- Warm orange accent on warm neutrals; light and dark both first-class; system font.
- Bright orange for fills and icons, a darker shade for text, so contrast stays at WCAG AA.

## Rules that must hold

- The manifest never declares the internet permission. Make this a test.
- Export then import restores everything without loss. Make this a test.
- Every reminder and every timer end goes through the one scheduling path; no second mechanism.
- Don't name, mention or compare against any commercial to-do app in code, docs, screenshots or posts.
- Nothing goes public (remote repo, push, release, post) without my explicit yes for that action.
- Test device is a Samsung Galaxy A54. When a session's work is done, stop the emulator, adb and Gradle
  daemons it started.

## Non-goals

Repeating tasks (habits belong in a habit app), labels, nested projects and sections, task durations, a calendar grid, a global quick-add or home-screen
shortcut flow, keyboard shortcuts, encryption, sync, accounts, collaboration, AI features, points or
streaks, location reminders, importers for other apps' files, desktop and web versions.

## Still open

- Exact colour values, proposed with contrast checked when the theme is built.
- Whether to keep lists at all, or have one single task list.

## Platform and release

- Minimum Android 8.0 (API 26).
- Released on GitHub; F-Droid maybe later. Not planned for Google Play.
