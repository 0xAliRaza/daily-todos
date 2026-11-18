# Backup file format

Settings → Export writes one JSON file; Settings → Import reads it back and replaces everything after a
confirmation. Only files written by this app are accepted.

## Version 1

```json
{
  "format": "daily-tasks-backup",
  "version": 1,
  "exportedAt": "2026-11-18T12:00:00Z",
  "settings": {
    "defaultReminderTime": "10:00",
    "undatedGetsTomorrow": true,
    "dateOnlyGetsReminder": true,
    "headsUpMinutes": 0,
    "overdueReminders": true,
    "overdueReminderTime": "09:00",
    "dndOverride": false,
    "focusMinutes": 25,
    "breakMinutes": 5,
    "breaksEnabled": true,
    "timerSignal": "BOTH",
    "theme": "SYSTEM",
    "lastBackupAt": "2026-11-18T12:00:00Z"
  },
  "lists": [{ "id": "default", "name": "Daily", "position": 0, "isDefault": true }],
  "tasks": [{
    "id": "…", "listId": "default", "title": "Call bank", "notes": "", "priority": 1,
    "dueDate": "2026-11-19", "dueTime": "16:00", "position": 0,
    "snoozedUntil": null, "notifiedFor": null, "completedAt": null,
    "createdAt": "2026-11-18T09:00:00Z", "updatedAt": "2026-11-18T09:00:00Z"
  }],
  "subtasks": [{ "id": "…", "taskId": "…", "title": "Find statement", "done": false, "position": 0 }],
  "reminderHistory": [{
    "taskId": "…", "dueAt": "2026-11-18T15:00:00Z", "firedAt": "2026-11-18T15:00:01Z", "firedBy": "ALARM"
  }]
}
```

- Dates are ISO dates, times are 24-hour `HH:mm` wall-clock values (no time zone), and instants are
  ISO-8601 UTC timestamps.
- `priority`: 0 none, 1 to 3 (1 is the most urgent). `headsUpMinutes`: 0, 10, 30 or 60.
  `timerSignal`: `SOUND`, `VIBRATE` or `BOTH`. `theme`: `SYSTEM`, `LIGHT` or `DARK`.
  `firedBy`: `ALARM`, `CATCH_UP` or `BEFORE_UNLOCK`.
- Exactly one list has `isDefault: true`.
- Deleted tasks and lists are not exported. The running focus timer is not exported.

## What import refuses

A file that is not this format, a newer version, unknown or missing fields, more than 1,000 lists, 50,000
tasks, 200,000 sub-tasks or 1,000 history entries, a file over 20 MB, blank or overlong text, duplicate
ids, a task in a list that does not exist, a sub-task of a task that does not exist, anything but exactly
one default list, invalid UTF-8, and any value this app would never write.

## Changing the format

Any change to the file is a new version. The app keeps reading every older version it has written.
