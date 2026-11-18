# Daily Tasks — implementation plan

Read `AGENTS.md` first: it holds the product decisions, the rules that must hold and the non-goals.
This file is how to build it. Tick milestones as they ship and keep the plan true to the code.

Facts marked **(verified)** were checked against Android documentation or this machine on 2026-11-18.
Facts marked **(verify)** are from the planner's memory or community reports and must be confirmed on the
test phone before the design depends on them.

## 1. Environment

- Android Studio 2026.2 is installed. There is no system Java: use the bundled JDK 25 for command-line
  Gradle, `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` **(verified)**.
- SDK at `~/Library/Android/sdk`: platforms `android-36` and `android-37.0`, build-tools `37.0.0`, one
  emulator `Pixel_API_37`. `adb` is at `~/Library/Android/sdk/platform-tools/adb` and is not on `PATH`
  **(verified)**.
- Real test device: Samsung Galaxy A54 (One UI). Reminder reliability is only proven on a real phone; the
  emulator proves logic, not battery behaviour.
- Git is local only. No remote exists and none is added without the owner's explicit yes.
- When a session's work is done, run `./scripts/stop-dev.sh` (section 9) so nothing keeps running.

## 2. Stack

- Kotlin, Jetpack Compose with Material 3, single activity, Navigation Compose.
- `compileSdk` and `targetSdk` 37, `minSdk` 26. `java.time` is available natively from API 26.
- Room (with KSP) for storage, kotlinx.serialization for the backup file, coroutines and Flow.
- Manual dependency injection: one `AppContainer` built in `Application`. No Hilt, no Koin.
- Settings live in a single-row Room table, not DataStore: one storage mechanism, and the backup stays one
  source.
- Versions: take the latest stable AGP, Kotlin, Compose BOM, Room and KSP at scaffold time from the
  official release pages or the Android Studio new-project template. Do not trust remembered version
  numbers. Pin everything in `gradle/libs.versions.toml` and commit the Gradle wrapper.
- Scaffolded on 2026-11-18 with Gradle 9.9.1, AGP 9.5.1 (its built-in Kotlin support, so no
  `kotlin-android` plugin), Kotlin 2.4.21, KSP 2.3.14, Room 2.8.5 and Compose BOM 2026.09.00.
- detekt is pinned to `2.0.0-alpha.6`: the last stable line (1.23) cannot run on JDK 25 or read Kotlin
  2.4. Move to 2.0.0 stable when it ships. Formatting comes from detekt's ktlint wrapper.
- Third-party code beyond AndroidX, Kotlin and Material: none by default. The one candidate is a
  drag-to-reorder helper for lazy lists; compare a small hand-rolled version first, and if a library wins,
  pin it and list it in `THIRD_PARTY_NOTICES.md`.
- Before targeting 37, read the official "behavior changes" pages for Android 15, 16 and 17 (all apps and
  targeting apps) for anything touching alarms, notifications, broadcasts and edge-to-edge. A search on
  2026-11-18 found no alarm or notification changes for API 37, but that search was not exhaustive.

## 3. Modules

| Module | Contains | Tests |
| --- | --- | --- |
| `:core` | Pure Kotlin, no Android imports: domain model, quick-add parser, reminder maths (what is due when), sort rules, backup schema and validation | Fast JVM unit tests |
| `:app` | Room, the scheduler, receivers, notifications, focus timer, Compose UI, settings, export and import | JVM tests (Robolectric where Android classes are needed) and instrumented tests |

Put logic in `:core` whenever it does not need Android. The application id and base package are
`com.aliraza.dailytasks`.

## 4. Data model

Room database in normal (credential-protected) app storage. Export the schema to `schemas/` and commit it;
write a migration test from the first schema change.

- `tasks`: `id` (UUID text), `title`, `notes`, `priority` (0 none, 1 to 3), `due_date` (ISO date, nullable),
  `due_time` (`HH:mm`, nullable), `position` (manual order), `snoozed_until` (epoch ms, nullable),
  `notified_for` (epoch ms of the reminder instant already posted, nullable), `completed_at`, `deleted_at`
  (soft delete for undo), `created_at`, `updated_at`, `list_id` (required).
- `lists`: `id`, `name`, `position`, `is_default`. Exactly one default list, created on first run and named
  "Daily"; it can be renamed but not deleted.
- `subtasks`: `id`, `task_id`, `title`, `done`, `position`. One level only.
- `reminder_log`: `task_id`, due instant, fired-at instant, how it fired (alarm, catch-up, before unlock).
  The newest 200 rows are kept. No titles.
- `settings` (one row): default reminder time (10:00), "undated tasks get tomorrow" (on), "date-only tasks
  get a reminder" (on), early heads-up minutes (0, 10, 30 or 60; default 0), overdue reminders (on), overdue
  reminder time (09:00), Do Not Disturb override (off), focus minutes (25), break minutes (5), breaks
  enabled (on), timer signal (sound, vibrate, both), theme (system, light, dark), last backup time.
- `timer_state` (one row): phase (focus, break, idle), `ends_at`, `task_id` (nullable), paused remainder,
  round number. Persisted so the timer survives the process being killed.

Rules that follow from the model:

1. Dates and times are wall-clock values with no time zone. A reminder set for 16:00 fires at 16:00
   wherever the phone is.
2. The reminder instant of a task is: `snoozed_until` if set; else `due_date` at `due_time` minus the
   early heads-up minutes; else `due_date` at the default reminder time (if that setting is on); else none.
   It is computed, never stored, so changing a setting moves every affected reminder.
3. "Undated tasks get tomorrow" is applied when a task is created in the default list: `due_date` becomes
   tomorrow and `due_time` stays empty, so rule 2 gives it the default time. Tasks created in any other
   list stay undated.
4. A reminder is "already posted" when `notified_for` equals its current instant. Editing the date or time
   changes the instant, which makes it eligible again.
5. Overdue: while a task is undone and its `due_date` is before today, its reminder instant is today at
   the overdue reminder time (if that setting is on). The instant moves forward each day, so rule 4 lets it
   fire once per day until the task is done or re-dated. One notification per task, never a summary.
   Undated tasks are never overdue.

## 5. Reminder engine

This is the product. Everything that must fire at a time — task reminders, snoozes and focus-timer ends —
goes through one class, `ReminderScheduler`. There is no second mechanism.

**How it works**

1. Compute the earliest pending instant across tasks and the timer.
2. Arm exactly one alarm for it with `AlarmManager.setAlarmClock()`. Android treats these as the most
   important alarms: it does not shift them and leaves low-power mode for them **(verified)**. The older
   alternative, `setExactAndAllowWhileIdle()`, may be delayed while the phone is idle and is rate-limited,
   so it is not used.
3. When the alarm fires, a receiver loads everything due at or before now, posts one notification per
   task, records `notified_for`, and arms the next alarm.
4. Catch-up: every time the scheduler runs, anything due in the past and not yet posted is posted at once.
   This covers the phone having been off, or the app having been stopped.
5. Every post writes a `reminder_log` row. The health screen shows it and flags anything that fired more
   than a minute late.

For reference, the best-known open-source Android to-do app schedules with `setExactAndAllowWhileIdle()`
and has no before-unlock handling **(verified in its source, 2026-11-18)**, and its users report late
reminders even with battery optimisation off. That is the failure this design avoids.

**When the scheduler re-runs**: every task or timer write (through the repository, never from UI code),
app process start, `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, time change, time-zone
change, and the exact-alarm permission broadcast on Android 12. Alarms do not survive a reboot or an app
update, so these are required **(verify the app-update case on the phone)**.

**Permissions** (this exact set is pinned by a test, section 9)

- `USE_EXACT_ALARM` on Android 13 and up. It is granted at install and the user is never asked
  **(verified)**. Google Play only allows it for alarm and calendar apps; this app is not released there.
- `SCHEDULE_EXACT_ALARM` with `maxSdkVersion="32"` for Android 12; nothing is needed below 12.
- `POST_NOTIFICATIONS` (asked at runtime on Android 13 and up), `RECEIVE_BOOT_COMPLETED`, `VIBRATE`,
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- Never `INTERNET`.

**Restart before unlock.** After a reboot the normal database is unreadable until the first unlock, and
`BOOT_COMPLETED` only arrives after it; `LOCKED_BOOT_COMPLETED` arrives before **(verified)**. Phones that
restart themselves overnight would otherwise miss every morning reminder. So:

1. The boot and alarm receivers are `directBootAware`.
2. On every run the scheduler writes a small "schedule mirror" (task id, instant, title for the next
   reminders) as a JSON file in device-protected storage.
3. Before unlock, the receiver posts notifications from the mirror and notes what it fired. After unlock it
   reconciles that into the database.
4. Notification actions need the unlocked database: if the user is not unlocked, an action leaves the
   notification in place.
5. Trade-off to state in `SECURITY.md`: the titles of upcoming reminders sit in device-protected storage,
   which is readable before the first unlock. Accepted because encryption is a non-goal.

**Notifications**

- One notification per task. Never call `setGroup` and never post a summary.
- Tapping opens the task. Actions: Done, 10 min, 1 hour (the two snooze lengths are a planner default).
- Channels: `reminders` (high importance), `reminders_dnd` (used when the Do Not Disturb override is on),
  `timer_sound`, `timer_vibrate`, `timer_both`, `timer_running` (low, silent). A channel's sound and
  vibration cannot be changed by the app after it is created, which is why each timer signal has its own.
- Do Not Disturb override **(verify)**: first try a channel whose audio usage is "alarm" with the alarm
  notification category, which community reports say passes Do Not Disturb when alarms are allowed. If that
  fails on the phone, fall back to asking for notification-policy access and `setBypassDnd(true)`.

**Side effect to know about.** `setAlarmClock()` puts the alarm icon in the status bar and shows the time
as the phone's "next alarm" **(verify how One UI shows it)**. The owner has been told; if they reject it,
the fallback is `setExactAndAllowWhileIdle()` plus the battery exemption, with the weaker guarantee
written down.

**Limits that no app can remove** (say so in the README and on the health screen): a user force-stop, and
some makers' background blocking until the app is allowed to autostart. In both cases nothing fires until
the app is opened again; catch-up then posts what was missed.

## 6. Focus timer

1. No foreground service. The end of each round is an entry in `ReminderScheduler`, so it fires even if the
   app is killed.
2. While a round runs, an ongoing notification on `timer_running` shows a system-drawn countdown
   (`setUsesChronometer`, `setChronometerCountDown`, `setWhen(endsAt)`) with Pause and Stop. Nothing has to
   stay alive to keep it counting.
3. Start screen: focus length and break length with quick plus and minus steps and a few presets, and a
   switch for breaks. The last values are remembered.
4. Started from a task, it keeps offering rounds until the task is completed; completing the task stops it.
   It can also run with no task.
5. Planner default for round changes: when focus ends, the alert fires and the break starts by itself; when
   the break ends, the alert offers "Start next focus" and waits. An unattended phone therefore stops after
   one break.
6. End signal follows the setting: sound, vibrate, or both. Verify vibrate-only in silent and vibrate
   ringer modes, and under Do Not Disturb with the override on.
7. The end signal is alarm-like: it repeats until the user dismisses it or taps an action, and stops by
   itself after 60 seconds (planner default). First try the notification "insistent" flag, with the
   stop scheduled through `ReminderScheduler` as a silent replacement of the notification **(verify that
   insistent repeats vibration as well as sound on the A54)**. If it does not, fall back to a short
   foreground service started from the alarm receiver that plays on the alarm stream and vibrates; it
   must stop itself at the cap. Task reminders stay ordinary single-alert notifications.

## 7. Quick-add parser (`:core`)

Input: the typed line, the current date-time, and two locale flags (day-first dates, 24-hour clock).
Output: title, date, time, priority, list, and the character ranges that were recognised (for highlighting).
English only in v1.

| Kind | Recognised |
| --- | --- |
| Relative day | today, tod, tonight, tomorrow, tmrw, tom, day after tomorrow |
| Weekday | full names always; three-letter forms only after "on", "next", "this", or as the last word |
| Offset | in N minutes, hours, days, weeks (and short forms) |
| Date | 25 dec, dec 25, 25 december 2026, 25/12, 25/12/26, 2026-12-25 |
| Time | 4pm, 4:30pm, 16:30, at 4, noon, midnight; morning 09:00, afternoon 14:00, evening 18:00, tonight 20:00 |
| Priority | p1, p2, p3 as whole words |
| List | `#name`, matched case-insensitively against existing list names; an unknown `#word` stays in the title |

Rules:

1. Match whole words only. A month name with no day number is not a date ("March for science").
2. The first date and the first time win; later ones stay in the title.
3. A time with no date means today if it is still ahead, otherwise tomorrow.
4. "at 4" with no am or pm: with no date or today, the next 4 o'clock after now; with another date, 7 to 11
   is morning, 12 is noon, 1 to 6 is afternoon.
5. A date with no year that has already passed means next year. Numeric date order follows the locale flag.
6. The title is the line minus the recognised parts. If that would leave it empty, recognise nothing.
7. Tapping a highlighted part excludes it, and the parser accepts that exclusion on the next pass.
8. The "undated gets tomorrow" default is applied by the domain layer after parsing, not by the parser.

Tests are table-driven, at least 150 rows, and include negatives ("Read Tomorrow book", "sat down with
Sam", "May I call back").

## 8. Screens

1. Today: Overdue, then today's timed tasks by time, then the rest in manual order. Tasks from every list.
2. Upcoming: a week strip at the top, tasks grouped by day. Tasks from every list.
3. Lists: "Daily" first, then the user's lists; create, rename, reorder, delete. A row outside its own list
   shows the list name. Adding from inside a list files the task there. Deleting a list asks first and
   deletes its tasks, with undo (planner default).
4. Add: a bottom sheet with the one-line field, live highlighting, and chips for what was recognised.
5. Task sheet: title, notes, date, time, priority, sub-task checkboxes, "Start focus".
6. Focus timer.
7. Search.
8. Settings, including Reminder health, Export / Import, and a direct "Battery optimisation" row that
   opens the phone's battery setting for this app (the same action as the fix button on the health screen).

Behaviour: swipe to complete; undo snackbar after complete, delete and bulk actions (soft delete, purged
later); long-press for multi-select with bulk complete, re-date, priority and delete; drag to reorder inside
a section; new tasks go to the bottom.

Design: tokens in one file; warm orange accent on warm neutrals; light and dark; no wallpaper-based dynamic
colour; system font; edge-to-edge; start/end layout only (right-to-left safe); works at 200% font size;
TalkBack labels; respects the system's animation-off setting; designed empty states. Propose the exact
palette with contrast ratios computed, and get the owner's yes before building on it.

**Reminder health screen**: one row per check, green or red, each with a fix button.

1. Notifications allowed, and the reminder channels not silenced.
2. Exact alarms allowed (only Android 12 can say no).
3. Battery: exempt from optimisation, and not background-restricted.
4. Maker-specific steps chosen from `Build.MANUFACTURER`: Samsung (sleeping and deep-sleeping apps),
   Xiaomi, Oppo, Vivo, OnePlus, Huawei, Tecno, Infinix, and a generic fallback. Deep links to maker settings
   are best-effort, wrapped so a missing screen falls back to the app's system settings page. Take the
   steps from dontkillmyapp.com and re-check them; do not invent them.
5. "Send a test reminder in 1 minute".
6. Reminder history from `reminder_log`: due time, fired time, and a flag on anything late.

## 9. Checks

Rules a machine can check are checks, not sentences.

- `./scripts/verify.sh` runs everything: formatting and static analysis (detekt with a cognitive-complexity
  limit, Kotlin warnings as errors so shadowing fails the build), Android lint with warnings as errors and
  an empty baseline, all JVM tests, and a debug build.
- Permission tripwire: a test reads the **merged** manifest and compares it with the exact allowed list in
  section 5. A library that adds a permission fails the build. The manifest also carries
  `<uses-permission android:name="android.permission.INTERNET" tools:node="remove"/>` as the fence.
- Backup round trip: export, wipe, import, compare the whole database.
- Migration test for every schema change.
- `./scripts/device-test.sh` runs instrumented tests on the connected phone or emulator.
- `./scripts/stop-dev.sh` stops Gradle daemons, the emulator and the adb server.
- A bug fix ships with a test that was seen failing before the fix.

## 10. Milestones

Logic milestones can be committed when `verify.sh` is green. Anything the owner has to see on the phone
(M4 to M7) waits for their yes before the commit.

- [x] **M0 Skeleton.** (Installed and launched on the emulator on 2026-11-18; installing on the A54 is
  still to do.) Two modules, version catalog, theme token stub, empty Today screen, the three
  scripts, permission tripwire, lint and detekt, `README.md` (what it is, non-goals), `SECURITY.md`,
  `CHANGELOG.md`, `THIRD_PARTY_NOTICES.md`. Done when `verify.sh` is green and the debug build installs on
  the emulator and the A54.
- [ ] **M1 Core.** Domain model, parser with its table tests, reminder maths (next due, catch-up), sort
  rules. Done when the tests pass with no Android dependency in `:core`.
- [ ] **M2 Storage.** Entities, DAOs, repository, settings row, the default list, soft delete, schema
  export.
- [ ] **M3 Reminder engine.** Start with a one-day spike on the A54 answering every **(verify)** in section
  5, and write the answers back into this file. Then the scheduler, receivers, notifications with actions,
  the schedule mirror and catch-up, with a bare add field to create tasks. Done when the matrix below
  passes on the A54.
- [ ] **M4 Task screens.** Section 8 items 1, 2, 4, 5 and 7 (the lists screen is M7).
- [ ] **M5 Reminder health** and the first-run permission flow.
- [ ] **M6 Focus timer.**
- [ ] **M7 Lists.** The lists screen and `#list` in quick add. Until then every task lives in "Daily".
- [ ] **M8 Export, import, settings.** Export through the system "create document" picker and import
  through "open document", so no storage permission is needed. Treat the imported file as hostile: size
  cap, strict schema and version check, length and count caps, and one transaction that replaces
  everything after a confirmation. Re-run the scheduler after import. `allowBackup="false"` and data
  extraction rules that exclude everything.
- [ ] **M9 Release preparation.** Code shrinking, signing with a keystore kept outside the repo, dependency
  verification metadata, icon, screenshots with made-up tasks, final README. Publishing anything needs the
  owner's explicit yes.

**M3 device matrix** (each must pass on the A54, with the reminder landing within a few seconds):

1. Screen off and the phone forced idle (`adb shell dumpsys deviceidle force-idle`).
2. Battery saver on.
3. App swiped away from recents.
4. Reboot, unlock, then the reminder time arrives.
5. Reboot and do not unlock; the reminder time arrives while locked.
6. Phone switched off across the reminder time; it fires after boot.
7. Clock change and time-zone change.
8. App updated (`adb install -r`) with a reminder pending.
9. Five reminders in the same minute give five notifications.
10. Do Not Disturb on: silent with the override off, alerts with it on.
11. App placed in Samsung's sleeping and deep-sleeping lists: record what happens, and the health screen
    must flag it.
12. Force-stop: nothing fires (documented limit); opening the app posts what was missed.
13. A task left undone yesterday reminds at the overdue time today, one notification per task, and again
    tomorrow if still undone.
14. With the early heads-up at 10 minutes, a 16:00 task reminds at 15:50.

The focus timer adds its own check in M6: the end signal keeps going until dismissed and stops by itself at
the cap, in each of the three signal modes.

## 11. Open decisions for the owner

1. **Alarm icon.** Planned as accepted. Show the owner the status-bar icon on the A54 during the M3 spike;
   they can veto it then, which switches to the fallback in section 5.
2. **Planner defaults**, changeable on request: snooze lengths (10 min, 1 hour); named times of day
   (section 7); the timer waiting for a tap after each break (section 6); overdue reminders repeating
   every day at 09:00 until the task is done (section 4); the timer's end signal stopping by itself after
   60 seconds (section 6).

## 12. Sources

- Schedule alarms: https://developer.android.com/training/scheduling/alarms
- Exact alarms denied by default (Android 14): https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- Direct Boot: https://developer.android.com/privacy-and-security/direct-boot
- Android 17 behaviour changes: https://developer.android.com/about/versions/17/behavior-changes-17
- Maker background restrictions: https://dontkillmyapp.com
