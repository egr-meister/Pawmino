package com.pawmino.app.util

import com.pawmino.app.model.CareTask
import com.pawmino.app.model.DailyTaskInstance
import com.pawmino.app.model.ScheduleType
import com.pawmino.app.model.TaskStatus
import java.time.LocalDate

/**
 * Deterministic, offline daily task-instance generation.
 *
 * The generator is pure: given a task and a calendar date it decides whether the task is
 * active and which scheduled times apply. It never mutates state, never uses background
 * work, and relies only on local dates (no timezone-sensitive timestamp arithmetic).
 */
object TaskGenerator {

    /** Whether a task should produce instances on [date]. Disabled tasks are never active. */
    fun isActiveOn(task: CareTask, date: LocalDate): Boolean {
        if (!task.enabled) return false
        return when (task.scheduleType) {
            ScheduleType.Daily -> true

            ScheduleType.SelectedDays -> {
                val weekDay = DateTimeUtil.weekDayOf(date)
                task.daysOfWeek.contains(weekDay)
            }

            ScheduleType.OneTime -> {
                val target = DateTimeUtil.parseDateOrNull(task.specificDate)
                target != null && target == date
            }

            ScheduleType.EveryNumberOfDays -> {
                val interval = (task.repeatIntervalDays ?: 0)
                if (interval < 1) return false
                val anchor = resolveAnchor(task) ?: return false
                if (date.isBefore(anchor)) return false
                val diff = DateTimeUtil.daysBetween(anchor, date)
                diff >= 0 && diff % interval == 0L
            }

            ScheduleType.Unscheduled -> false
        }
    }

    /** Anchor date for EveryNumberOfDays: explicit anchor, else createdAt date, else null. */
    private fun resolveAnchor(task: CareTask): LocalDate? {
        DateTimeUtil.parseDateOrNull(task.anchorDate)?.let { return it }
        // createdAt is an ISO-8601 timestamp; take its local date portion if present.
        val createdDatePart = task.createdAt.take(10)
        DateTimeUtil.parseDateOrNull(createdDatePart)?.let { return it }
        return null
    }

    /**
     * The scheduled times to generate for a task. Invalid times are dropped and duplicates
     * removed. If no valid time remains, a single blank "Anytime" slot is used.
     */
    fun scheduledTimesFor(task: CareTask): List<String> {
        val valid = task.times
            .filter { DateTimeUtil.isValidTime(it) }
            .distinct()
            .sorted()
        return if (valid.isEmpty()) listOf("") else valid
    }

    /** Desired (fresh, pending) instances for a task on a date. Ids are deterministic. */
    fun desiredInstances(task: CareTask, dateIso: String, date: LocalDate): List<DailyTaskInstance> {
        if (!isActiveOn(task, date)) return emptyList()
        val now = DateTimeUtil.nowTimestamp()
        return scheduledTimesFor(task).map { time ->
            DailyTaskInstance(
                id = IdGen.instanceId(task.id, dateIso, time),
                taskId = task.id,
                petId = task.petId,
                date = dateIso,
                scheduledTime = time,
                status = TaskStatus.Pending,
                completedAt = "",
                note = "",
                createdAt = now,
                updatedAt = now
            )
        }
    }

    /**
     * Merge desired instances for the given pet + date into the existing instance store.
     * Existing instances (matched by deterministic id) are preserved exactly so completion
     * history is never overwritten. Only genuinely missing instances are added.
     *
     * Returns the FULL instance list (all pets/dates) with additions applied, plus the list
     * of newly created instances (empty if nothing changed).
     */
    fun ensureInstances(
        allInstances: List<DailyTaskInstance>,
        tasks: List<CareTask>,
        petId: String,
        dateIso: String
    ): Pair<List<DailyTaskInstance>, List<DailyTaskInstance>> {
        val date = DateTimeUtil.parseDateOrNull(dateIso)
            ?: return allInstances to emptyList()

        val existingIds = allInstances.mapTo(HashSet()) { it.id }
        val newOnes = ArrayList<DailyTaskInstance>()

        tasks.asSequence()
            .filter { it.petId == petId }
            .forEach { task ->
                desiredInstances(task, dateIso, date).forEach { candidate ->
                    if (candidate.id !in existingIds) {
                        newOnes.add(candidate)
                        existingIds.add(candidate.id)
                    }
                }
            }

        return if (newOnes.isEmpty()) {
            allInstances to emptyList()
        } else {
            (allInstances + newOnes) to newOnes
        }
    }

    /**
     * The active instances shown in the loop/timeline for a pet + date: stored instances
     * whose backing task still exists, is enabled, and is active on that date.
     */
    fun activeInstancesFor(
        allInstances: List<DailyTaskInstance>,
        tasks: List<CareTask>,
        petId: String,
        dateIso: String
    ): List<DailyTaskInstance> {
        val date = DateTimeUtil.parseDateOrNull(dateIso) ?: return emptyList()
        val taskById = tasks.associateBy { it.id }
        return allInstances.filter { inst ->
            inst.petId == petId && inst.date == dateIso &&
                (taskById[inst.taskId]?.let { isActiveOn(it, date) } == true)
        }
    }
}
