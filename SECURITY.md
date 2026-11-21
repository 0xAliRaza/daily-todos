# Security

## Reporting a problem

Please report security problems privately through the repository's security advisory form rather than in
a public issue. Include the Android version, the phone model and the steps to reproduce.

## Design

- **No network.** The manifest never declares the internet permission; a test reads the merged manifest
  and fails the build if any permission outside the allowed set appears.
- **Least privilege.** Exact alarms, notifications, boot completion, vibration and the battery
  optimisation prompt — nothing else. No storage permission: export and import go through the system file
  picker.
- **Local data only.** Tasks are stored in the app's private database. Android cloud backup and device
  transfer are switched off.
- **Imports are hostile.** An imported file is size-capped, strictly validated against the backup schema
  and version, and applied in one transaction only after confirmation.
- **No secrets, no logging of content.** The app holds no credentials. Logs and the reminder history never
  contain task titles.

## Accepted trade-off

So that reminders still fire after a restart before the phone is first unlocked, the app keeps a small
file with the titles, due times and list names of the next upcoming reminders in device-protected
storage. That storage is readable before the first unlock, unlike the main database. This is accepted
because encryption is not a goal of this app; the file holds only upcoming reminders and is rewritten
whenever the schedule changes.
