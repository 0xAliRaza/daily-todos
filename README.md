# Daily Tasks

A to-do app for Android that does one thing well: it remembers for you, and its reminders always fire.

Type a task in one line — `Call bank tomorrow 4pm p1`, `Milk #groceries` — and the date, time, priority
and list are picked out as you type. Every dated task reminds you on time, even with battery saving on,
after a restart, or when the phone was switched off across the reminder time.

**Status:** early development. Nothing is released yet.

## What it does

- Lists (one built-in "Daily" list plus your own), sub-tasks, three optional priorities.
- Today and Upcoming views across all lists, search, undo on every action.
- One notification per task with Done and Snooze; overdue tasks remind again the next morning.
- A Reminder health screen that checks every permission and battery setting for your phone's maker and
  keeps a history of when each reminder was due and when it actually fired.
- A focus timer whose round ends are scheduled like reminders, with sound, vibrate-only or both.
- Export and import of everything as one JSON file.

## What it does not do

Repeating tasks, labels, nested projects or sections, task durations, a calendar grid, home-screen quick
add, keyboard shortcuts, encryption, sync, accounts, collaboration, AI features, points or streaks,
location reminders, importing other apps' files, and desktop or web versions.

## Privacy

The app has no internet permission, so it cannot talk to the network: no accounts, analytics, crash
reporting or update checks. Your tasks live in a private database on the phone, Android's cloud backup is
switched off, and the only way data leaves the device is an export you make yourself.

## Known limits

No app can fire a reminder after you force-stop it, or while a phone maker's background blocking holds it
until it is allowed to autostart. In both cases the missed reminders are posted as soon as the app is
opened again. The Reminder health screen shows how to lift the block on your phone.

## Building

Requirements: Android Studio (its bundled JDK is used) and the Android SDK with platform 37.

```sh
./scripts/verify.sh        # static analysis, lint, all JVM tests and a debug build
./scripts/device-test.sh   # instrumented tests on a connected phone or emulator
./scripts/stop-dev.sh      # stop the emulator, adb and the Gradle daemons
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## Licence

GPL-3.0-or-later. See `LICENSE`.
