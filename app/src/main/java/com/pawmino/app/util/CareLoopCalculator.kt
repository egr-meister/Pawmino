package com.pawmino.app.util

import com.pawmino.app.model.CareCategory
import com.pawmino.app.model.CareTask
import com.pawmino.app.model.DailyTaskInstance
import com.pawmino.app.model.TaskStatus

/** One slice of the daily care loop, aggregated by care category. */
data class CategorySegment(
    val category: CareCategory,
    val label: String,
    val total: Int,
    val completed: Int,
    val skipped: Int,
    val pending: Int
) {
    /** Completed / total. Returns 0f when the category has no active tasks. */
    val fraction: Float get() = if (total <= 0) 0f else completed.toFloat() / total.toFloat()
    val allComplete: Boolean get() = total > 0 && completed == total
}

/** Aggregate loop data for a pet + date. */
data class LoopSummary(
    val segments: List<CategorySegment>,
    val totalTasks: Int,
    val completedTasks: Int,
    val skippedTasks: Int,
    val pendingTasks: Int
) {
    val percent: Int get() = CareLoopCalculator.percent(completedTasks, totalTasks)
    val isEmpty: Boolean get() = totalTasks == 0
    val allComplete: Boolean get() = totalTasks > 0 && completedTasks == totalTasks

    /** Textual summary duplicated for accessibility / TalkBack. */
    fun accessibilityText(petName: String): String = when {
        isEmpty -> "$petName has no care tasks for this day."
        allComplete -> "$petName: all $totalTasks care tasks complete for today."
        else -> "$petName: $completedTasks of $totalTasks care tasks complete, $percent percent."
    }
}

object CareLoopCalculator {

    fun percent(completed: Int, total: Int): Int {
        if (total <= 0) return 0
        val raw = completed.toFloat() / total.toFloat() * 100f
        return raw.toInt().coerceIn(0, 100)
    }

    /**
     * Build the loop summary from the active instances and the tasks they reference.
     * Skipped tasks remain part of the total but do not count as completed.
     */
    fun build(instances: List<DailyTaskInstance>, tasks: List<CareTask>): LoopSummary {
        val taskById = tasks.associateBy { it.id }

        // Group instances by the category of their backing task.
        data class Acc(var total: Int = 0, var completed: Int = 0, var skipped: Int = 0, var pending: Int = 0, var label: String = "")
        val byCategory = LinkedHashMap<CareCategory, Acc>()

        instances.forEach { inst ->
            val task = taskById[inst.taskId]
            val category = task?.category ?: CareCategory.Other
            val label = task?.categoryDisplayName() ?: CareCategory.Other.label
            val acc = byCategory.getOrPut(category) { Acc(label = label) }
            acc.total += 1
            when (inst.status) {
                TaskStatus.Completed -> acc.completed += 1
                TaskStatus.Skipped -> acc.skipped += 1
                TaskStatus.Pending -> acc.pending += 1
            }
        }

        // Order segments by the canonical category order for a stable, readable loop.
        val segments = CareCategory.entries
            .filter { byCategory.containsKey(it) }
            .map { cat ->
                val acc = byCategory.getValue(cat)
                CategorySegment(
                    category = cat,
                    label = acc.label.ifBlank { cat.label },
                    total = acc.total,
                    completed = acc.completed,
                    skipped = acc.skipped,
                    pending = acc.pending
                )
            }

        val total = instances.size
        val completed = instances.count { it.status == TaskStatus.Completed }
        val skipped = instances.count { it.status == TaskStatus.Skipped }
        val pending = instances.count { it.status == TaskStatus.Pending }

        return LoopSummary(
            segments = segments,
            totalTasks = total,
            completedTasks = completed,
            skippedTasks = skipped,
            pendingTasks = pending
        )
    }
}
