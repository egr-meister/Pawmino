package com.pawmino.app.util

import com.pawmino.app.model.CareTask
import com.pawmino.app.model.DailyTaskInstance
import com.pawmino.app.model.ReminderSettings
import com.pawmino.app.model.ShoppingItem
import com.pawmino.app.model.ShoppingPriority
import com.pawmino.app.model.TaskStatus
import java.time.LocalTime

enum class ReminderKind { Upcoming, Overdue, NothingDone, Shopping }

data class ActiveReminder(
    val kind: ReminderKind,
    val message: String,
    val detail: String,
    val taskInstanceId: String? = null
)

/** A pending timed task paired with its backing task, used for the "next task" ribbon. */
data class NextTask(
    val instance: DailyTaskInstance,
    val task: CareTask,
    val minutesUntil: Long?
)

/**
 * In-app reminder calculations. Everything runs on demand while the app is open — there are
 * no notifications, no background work, no alarms. Callers pass the current local time so the
 * logic is fully testable.
 */
object ReminderUtil {

    fun minutesUntil(scheduledTime: String, now: LocalTime): Long? {
        val t = DateTimeUtil.parseTimeOrNull(scheduledTime) ?: return null
        return java.time.temporal.ChronoUnit.MINUTES.between(now, t)
    }

    /** Earliest still-pending, timed instance at or after [now]. */
    fun nextTask(
        instances: List<DailyTaskInstance>,
        tasks: List<CareTask>,
        now: LocalTime
    ): NextTask? {
        val taskById = tasks.associateBy { it.id }
        return instances.asSequence()
            .filter { it.status == TaskStatus.Pending && it.scheduledTime.isNotBlank() }
            .mapNotNull { inst ->
                val task = taskById[inst.taskId] ?: return@mapNotNull null
                val mins = minutesUntil(inst.scheduledTime, now) ?: return@mapNotNull null
                if (mins < 0) null else NextTask(inst, task, mins)
            }
            .minByOrNull { it.minutesUntil ?: Long.MAX_VALUE }
    }

    /**
     * Compute reminders for TODAY. Reminders are only meaningful for the current day, so the
     * caller passes [isToday]; when false, no reminders are produced.
     */
    fun computeReminders(
        isToday: Boolean,
        activeInstances: List<DailyTaskInstance>,
        tasks: List<CareTask>,
        shoppingItems: List<ShoppingItem>,
        activePetId: String?,
        settings: ReminderSettings,
        now: LocalTime,
        dismissedKinds: Set<ReminderKind> = emptySet()
    ): List<ActiveReminder> {
        if (!settings.enabled || !isToday) return emptyList()

        val taskById = tasks.associateBy { it.id }
        val result = ArrayList<ActiveReminder>()

        // Upcoming: earliest pending timed task within the lead window.
        nextTask(activeInstances, tasks, now)?.let { next ->
            val mins = next.minutesUntil
            if (mins != null && mins in 0..settings.leadTimeMinutes.toLong()) {
                result.add(
                    ActiveReminder(
                        kind = ReminderKind.Upcoming,
                        message = "${next.task.title} is coming up.",
                        detail = "Scheduled for ${next.instance.scheduledTime}.",
                        taskInstanceId = next.instance.id
                    )
                )
            }
        }

        // Overdue: a scheduled time has passed and the task is still pending.
        if (settings.showOverdueTasks) {
            val overdue = activeInstances.asSequence()
                .filter { it.status == TaskStatus.Pending && it.scheduledTime.isNotBlank() }
                .mapNotNull { inst ->
                    val mins = minutesUntil(inst.scheduledTime, now) ?: return@mapNotNull null
                    if (mins < 0) inst to mins else null
                }
                .sortedBy { it.second } // most overdue first (most negative)
                .firstOrNull()
            if (overdue != null) {
                val inst = overdue.first
                val title = taskById[inst.taskId]?.title ?: "A care task"
                result.add(
                    ActiveReminder(
                        kind = ReminderKind.Overdue,
                        message = "$title is still pending.",
                        detail = "Scheduled for ${inst.scheduledTime}.",
                        taskInstanceId = inst.id
                    )
                )
            }
        }

        // Nothing completed yet today, but there are active tasks.
        if (activeInstances.isNotEmpty() && activeInstances.none { it.status == TaskStatus.Completed }) {
            result.add(
                ActiveReminder(
                    kind = ReminderKind.NothingDone,
                    message = "No care tasks completed yet today.",
                    detail = "Mark tasks as you go to keep the loop up to date."
                )
            )
        }

        // High-priority shopping items still unchecked (for this pet or shared).
        if (settings.showShoppingReminder) {
            val relevant = shoppingItems.count { item ->
                !item.checked && item.priority == ShoppingPriority.High &&
                    (item.petId == null || item.petId == activePetId)
            }
            if (relevant > 0) {
                val label = if (relevant == 1) "1 high-priority item" else "$relevant high-priority items"
                result.add(
                    ActiveReminder(
                        kind = ReminderKind.Shopping,
                        message = "Shopping list has $label.",
                        detail = "Open the shopping list to review."
                    )
                )
            }
        }

        return result.filter { it.kind !in dismissedKinds }
    }
}
