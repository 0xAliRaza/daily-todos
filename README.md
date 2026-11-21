# Daily Todos

A to-do app for Android that does one thing well: it remembers for you, and its reminders always fire.

Type a task in one line — `Call bank tomorrow 4pm p1`, `Milk #groceries` — and the date, time, priority
and list are picked out as you type. Every dated task reminds you on time, even with battery saving on,
after a restart, or when the phone was switched off across the reminder time.

**Status:** 0.2.1, a fresh design with accents and a true black theme. It is used daily, and rough
edges get fixed as they show up.

## Why

Daily Todos stays small on purpose:

- **A reminder is a promise.** It fires on time or the app tells you why it couldn't.
- **Adding a task takes one line.** No forms; the words you type are the settings.
- **Your tasks stay on your phone.** No account, no cloud, no tracking. Export them whenever you like.
- **Every feature earns its place** by being used daily. The list of things it does not do is part of
  the design.

## What it does

- Lists (one built-in "Daily" list plus your own), sub-tasks, three optional priorities.
- Four tabs: Today, Upcoming (with a week strip), a Day, 3-day, Week and Month Calendar, and Lists;
  swipe sideways to move between them; search; undo on every action.
- Swipe a task right to focus on it, left to delete it. Reschedule from the task, from a multi-select, or
  every overdue task at once.
- Pick the date, priority or list by hand while adding, or just type them.
- One notification per task, naming when and which list, with Complete and Snooze; overdue tasks remind
  again the next morning.
- A Reminder health screen that checks every permission and battery setting for your phone's maker and
  keeps a history of when each reminder was due and when it actually fired.
- A focus timer for each task, with focus and break rounds whose ends are scheduled like reminders and
  signalled by sound, a long vibration or both. A bar shows the running round on every screen.
- Drag around the focus dial to set a round's length; switch Caffeinated on (the Screen on chip on the timer)
  to keep the screen awake while a round counts down.
- Focus stats: total focus time, today, this week, a 7-day chart and the tasks you focused on most.
- The calendar shows what was done on a picked day and how long each task was focused.
- Light, dark or pure black for OLED screens, in one of four accents: ink blue, violet, teal or graphite.
- A 12-hour or 24-hour clock, the week starting on the phone's first day or on Monday or Sunday, and
  haptic feedback you can switch off.
- Export and import of everything as one JSON file.

## How to use

1. **Add:** tap **+** and type one line. Recognised words are highlighted as you type; tap one to keep
   it as plain text.

   | You type | You get |
   | --- | --- |
   | `Call bank tomorrow 4pm p1` | Tomorrow at 4 PM, priority 1 |
   | `Dentist friday 1030a` | This Friday at 10:30 AM |
   | `Milk #groceries` | Filed under Groceries, no reminder |
   | `Pay rent 1 dec 9am` | 1 December at 9 AM |
   | `Plan trip #weekend-trip` | Makes the list "Weekend trip" |

   A task in the Daily list without a date gets tomorrow at your default time (10:00), shown as a pill
   you can tap away. Lists of your own stay undated, so a grocery list never buzzes per item.
2. **Act on a task:** tap the round box to complete it, tap the task to edit it, swipe it right to
   focus on it, swipe it left to delete it, long-press to select several. Every action can be undone.
3. **Move around:** the tabs at the bottom, or swipe sideways on an empty part of the screen.
4. **Focus:** pick or drag a length on the dial, start, and take a break when the round ends. The mug
   at the top keeps the screen on while a round runs.
5. **Make sure reminders arrive:** open Settings → Reminder health once. It checks permissions and
   battery settings, shows the steps for your phone's maker, and can send a test reminder.
6. **Back up:** Settings → Export saves everything as one JSON file; Import restores it.

## What it does not do

Repeating tasks, labels, nested projects or sections, task durations, home-screen quick add, keyboard
shortcuts, encryption, sync, accounts, collaboration, AI features, points or streaks, location reminders,
importing other apps' files, and desktop or web versions.

## Install

1. On your phone, open the [latest release](https://github.com/0xAliRaza/daily-todos/releases/latest) and
   download `daily-todos-<version>.apk`.
2. Open the downloaded file. Android asks to allow installing from your browser or file manager the first
   time; allow it for that app, then tap Install.
3. Open Daily Todos, allow notifications, and check Settings → Reminder health once.

Updates install over the old version and keep your tasks, as long as they come from these releases.

### Check the download

Each release lists the APK's SHA-256 checksum. On a computer, compare it with:

```sh
shasum -a 256 daily-todos-0.2.1.apk          # macOS and Linux
certutil -hashfile daily-todos-0.2.1.apk SHA256   # Windows
```

Every release is signed with the same key. To check the signature, run `apksigner` from the Android
SDK build tools; the certificate's SHA-256 must be:

```sh
apksigner verify --print-certs daily-todos-0.2.1.apk
# Signer #1 certificate SHA-256 digest:
# 8c979ffda619eb7701524bcfc866c040969ffc0b9e18170f98a0733801ec39ba
```

Android also refuses to update the app from an APK signed with a different key, so once installed, a
tampered update cannot replace it.

## Privacy

The app has no internet permission, so it cannot talk to the network: no accounts, analytics, crash
reporting or update checks. Your tasks live in a private database on the phone, Android's cloud backup is
switched off, and the only way data leaves the device is an export you make yourself.

## Known limits

No app can fire a reminder after you force-stop it, or while a phone maker's background blocking holds it
until it is allowed to autostart. In both cases the missed reminders are posted as soon as the app is
opened again. The Reminder health screen shows how to lift the block on your phone.

## Building

Requirements: Android Studio (its bundled JDK is used) and the Android SDK with platform 37. The scripts
are written for macOS; on other systems run the Gradle tasks directly with `JAVA_HOME` pointing at a JDK
17 or newer.

### Your own APK

```sh
git clone https://github.com/0xAliRaza/daily-todos.git
cd daily-todos
./gradlew :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`; copy it to a phone and open it, or run
`adb install app/build/outputs/apk/debug/app-debug.apk`. It is signed with your machine's debug key,
so it cannot update a copy installed from these releases: export your tasks, uninstall that copy, then
install yours and import.

For a signed release build with a key of your own:

```sh
keytool -genkeypair -keystore my-release.jks -alias daily-tasks -keyalg RSA -keysize 4096 -validity 10000
DT_KEYSTORE=$PWD/my-release.jks DT_KEYSTORE_PASSWORD='your password' ./gradlew :app:assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

Use the same password for the key and the keystore, and keep the `.jks` file out of the repo.

### Development

```sh
./scripts/verify.sh        # static analysis, lint, all JVM tests and a debug build
./scripts/device-test.sh   # instrumented tests on a connected phone or emulator
./scripts/stop-dev.sh      # stop the emulator, adb and the Gradle daemons
./scripts/release.sh       # all checks, then the signed release APK in build/release/
```

`release.sh` signs with the maintainer's key, which is kept outside the repo.

## Licence

GPL-3.0-or-later. See `LICENSE`.
