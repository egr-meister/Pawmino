package com.pawmino.app.util

import java.util.UUID

/**
 * Local, offline id generation. No network, no account, no device identifiers.
 */
object IdGen {
    fun newId(): String = UUID.randomUUID().toString()

    /**
     * Deterministic, collision-safe instance id from a task id, date and scheduled time.
     * The scheduled time may be blank (an "Anytime" instance). Components are separated by
     * a delimiter that cannot appear in the encoded parts, so identical (task, date, time)
     * triples always map to the same id and never collide across different triples.
     */
    fun instanceId(taskId: String, date: String, scheduledTime: String): String {
        val safeTime = if (scheduledTime.isBlank()) "anytime" else scheduledTime
        return "inst::" + encode(taskId) + "::" + encode(date) + "::" + encode(safeTime)
    }

    private fun encode(part: String): String =
        part.replace("\\", "\\\\").replace(":", "\\c")
}
