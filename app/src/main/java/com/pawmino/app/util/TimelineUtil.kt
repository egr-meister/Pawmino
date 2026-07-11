package com.pawmino.app.util

import com.pawmino.app.model.CareTask
import com.pawmino.app.model.DailyTaskInstance

enum class DaySection(val label: String) {
    Morning("Morning"),
    Afternoon("Afternoon"),
    Evening("Evening"),
    Anytime("Anytime")
}

object TimelineUtil {

    fun sectionFor(time: String): DaySection {
        val t = DateTimeUtil.parseTimeOrNull(time) ?: return DaySection.Anytime
        return when {
            t.hour < 12 -> DaySection.Morning
            t.hour < 17 -> DaySection.Afternoon
            else -> DaySection.Evening
        }
    }

    /**
     * Order instances by scheduled time, then the backing task's configured order, then title.
     * Untimed ("Anytime") instances sort after timed ones. Safe when times collide.
     */
    fun sort(instances: List<DailyTaskInstance>, tasks: List<CareTask>): List<DailyTaskInstance> {
        val taskById = tasks.associateBy { it.id }
        return instances.sortedWith(
            compareBy<DailyTaskInstance> { it.scheduledTime.isBlank() }
                .thenBy { DateTimeUtil.parseTimeOrNull(it.scheduledTime) ?: java.time.LocalTime.MAX }
                .thenBy { taskById[it.taskId]?.sortOrder ?: Int.MAX_VALUE }
                .thenBy { taskById[it.taskId]?.title?.lowercase() ?: "" }
        )
    }

    /** Group sorted instances into the four day sections, preserving section order. */
    fun grouped(
        instances: List<DailyTaskInstance>,
        tasks: List<CareTask>
    ): List<Pair<DaySection, List<DailyTaskInstance>>> {
        val sorted = sort(instances, tasks)
        return DaySection.entries.mapNotNull { section ->
            val items = sorted.filter { sectionFor(it.scheduledTime) == section }
            if (items.isEmpty()) null else section to items
        }
    }
}
