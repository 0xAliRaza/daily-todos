package com.aliraza.dailytodos.reminder

import java.time.Instant

class FakePoster(var allowed: Boolean = true) : ReminderPoster {
    val posted = mutableListOf<ReminderContent>()
    val shown = mutableSetOf<String>()
    var lastDndOverride: Boolean? = null

    override fun post(content: ReminderContent, dndOverride: Boolean): Boolean {
        if (!allowed) return false
        posted += content
        shown += content.taskId
        lastDndOverride = dndOverride
        return true
    }

    override fun cancel(taskId: String) {
        shown -= taskId
    }

    override fun shownTaskIds(): Set<String> = shown.toSet()
}

class FakeTestReminderStore : TestReminderStore {
    var at: Instant? = null

    override fun pendingAt(): Instant? = at

    override fun set(at: Instant?) {
        this.at = at
    }
}

class FakeAlarm : AlarmArmer {
    var armedAt: Instant? = null
    var cancelled = false

    override fun arm(at: Instant) {
        armedAt = at
        cancelled = false
    }

    override fun cancel() {
        armedAt = null
        cancelled = true
    }
}
