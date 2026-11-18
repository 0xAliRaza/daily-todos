# Daily Tasks

A to-do app for Android that does one thing well: it remembers for you, and its reminders always fire.

Type a task in one line — `Call bank tomorrow 4pm p1`, `Milk #groceries` — and the date, time, priority
and list are picked out as you type. Every dated task reminds you on time, even with battery saving on,
after a restart, or when the phone was switched off across the reminder time.

**Status:** first release, 0.1.0. It is used daily, and rough edges get fixed as they show up.

## What it does

- Lists (one built-in "Daily" list plus your own), sub-tasks, three optional priorities.
- Today and Upcoming views across all lists, with a week strip and a Day, 3-day, Week and Month calendar;
  search; undo on every action.
- Swipe a task right to reschedule it, left to delete it; reschedule every overdue task at once.
- Pick the date, priority or list by hand while adding, or just type them.
- One notification per task with Done and Snooze; overdue tasks remind again the next morning.
- A Reminder health screen that checks every permission and battery setting for your phone's maker and
  keeps a history of when each reminder was due and when it actually fired.
- A focus timer whose round ends are scheduled like reminders, with sound, vibrate-only or both.
- Export and import of everything as one JSON file.

## What it does not do

Repeating tasks, labels, nested projects or sections, task durations, home-screen quick add, keyboard
shortcuts, encryption, sync, accounts, collaboration, AI features, points or streaks, location reminders,
importing other apps' files, and desktop or web versions.

## Install

1. On your phone, open the [latest release](https://github.com/0xAliRaza/daily-tasks/releases/latest) and
   download `daily-tasks-<version>.apk`.
2. Open the downloaded file. Android asks to allow installing from your browser or file manager the first
   time; allow it for that app, then tap Install.
3. Open Daily Tasks (shown as DT), allow notifications, and check Settings → Reminder health once.

Each release lists the APK's SHA-256 checksum and the signing certificate, so you can check the file.
Updates install over the old version and keep your tasks, as long as they come from these releases.

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
./scripts/release.sh       # all checks, then the signed release APK in build/release/
```

The debug APK lands in `app/build/outputs/apk/debug/`. Release signing needs the maintainer's key, which
is kept outside the repo.

## Licence

GPL-3.0-or-later. See `LICENSE`.
