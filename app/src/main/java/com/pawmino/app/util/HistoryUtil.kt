package com.pawmino.app.util

import com.pawmino.app.model.CareCategory
import com.pawmino.app.model.CareTask
import com.pawmino.app.model.DailyTaskInstance
import com.pawmino.app.model.PetNote
import com.pawmino.app.model.TaskStatus
import com.pawmino.app.model.WalkLog

/** History filters available on the History screen. */
enum class HistoryFilter(val label: String) {
    All("All"),
    Feeding("Feeding"),
    Walks("Walks"),
    Grooming("Grooming"),
    Play("Play"),
    Notes("Notes"),
    Completed("Completed"),
    Skipped("Skipped")
}

/** One day's rolled-up history for a pet. */
data class DailySummary(
    val dateIso: String,
    val total: Int,
    val completed: Int,
    val skipped: Int,
    val pending: Int,
    val categoriesCompleted: List<CareCategory>,
    val noteCount: Int,
    val walkCount: Int
) {
    val percent: Int get() = CareLoopCalculator.percent(completed, total)
}

object HistoryUtil {

    /**
     * Build reverse-chronological daily summaries for a pet from stored instances, notes and
     * walk logs. A date appears if it has any stored instance, note or walk log. Instances
     * referencing deleted tasks are still counted (they use a fallback category).
     */
    fun buildDailySummaries(
        petId: String,
        tasks: List<CareTask>,
        instances: List<DailyTaskInstance>,
        notes: List<PetNote>,
        walkLogs: List<WalkLog>
    ): List<DailySummary> {
        val taskById = tasks.associateBy { it.id }

        val petInstances = instances.filter { it.petId == petId && it.date.isNotBlank() }
        val petNotes = notes.filter { it.petId == petId && it.date.isNotBlank() }
        val petWalks = walkLogs.filter { it.petId == petId && it.date.isNotBlank() }

        val dates = (petInstances.map { it.date } +
            petNotes.map { it.date } +
            petWalks.map { it.date }).toSortedSet(compareByDescending { it })

        return dates.map { date ->
            val dayInstances = petInstances.filter { it.date == date }
            val completed = dayInstances.count { it.status == TaskStatus.Completed }
            val skipped = dayInstances.count { it.status == TaskStatus.Skipped }
            val pending = dayInstances.count { it.status == TaskStatus.Pending }
            val categoriesCompleted = dayInstances
                .filter { it.status == TaskStatus.Completed }
                .mapNotNull { taskById[it.taskId]?.category }
                .distinct()
            DailySummary(
                dateIso = date,
                total = dayInstances.size,
                completed = completed,
                skipped = skipped,
                pending = pending,
                categoriesCompleted = categoriesCompleted,
                noteCount = petNotes.count { it.date == date },
                walkCount = petWalks.count { it.date == date }
            )
        }
    }

    /** Whether a day matches a history filter (used to filter the summary list). */
    fun matches(
        summary: DailySummary,
        filter: HistoryFilter,
        tasks: List<CareTask>,
        instances: List<DailyTaskInstance>,
        petId: String
    ): Boolean {
        if (filter == HistoryFilter.All) return true
        val taskById = tasks.associateBy { it.id }
        val dayInstances = instances.filter { it.petId == petId && it.date == summary.dateIso }
        return when (filter) {
            HistoryFilter.All -> true
            HistoryFilter.Notes -> summary.noteCount > 0
            HistoryFilter.Walks -> summary.walkCount > 0 ||
                dayInstances.any { taskById[it.taskId]?.category == CareCategory.Walk }
            HistoryFilter.Completed -> summary.completed > 0
            HistoryFilter.Skipped -> summary.skipped > 0
            HistoryFilter.Feeding -> dayInstances.any { taskById[it.taskId]?.category == CareCategory.Feeding }
            HistoryFilter.Grooming -> dayInstances.any { taskById[it.taskId]?.category == CareCategory.Grooming }
            HistoryFilter.Play -> dayInstances.any { taskById[it.taskId]?.category == CareCategory.Play }
        }
    }
}
