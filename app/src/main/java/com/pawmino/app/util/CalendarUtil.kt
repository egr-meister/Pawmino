package com.pawmino.app.util

import com.pawmino.app.model.CareTask
import com.pawmino.app.model.DailyTaskInstance
import com.pawmino.app.model.PetNote
import com.pawmino.app.model.TaskStatus
import com.pawmino.app.model.WeekDay
import java.time.LocalDate
import java.time.YearMonth

enum class DayStatus { NoData, AllComplete, Partial, NoneComplete }

data class DayIndicator(
    val date: LocalDate,
    val dateIso: String,
    val total: Int,
    val completed: Int,
    val skipped: Int,
    val hasNote: Boolean,
    val status: DayStatus,
    val inMonth: Boolean
)

/** A calendar month laid out as fixed weeks, honoring the user's first day of week. */
data class CalendarMonth(
    val yearMonth: YearMonth,
    val title: String,
    val weekHeaders: List<String>,
    val cells: List<DayIndicator>
)

object CalendarUtil {

    /**
     * Compute the indicator for a single date deterministically. Task totals come from the
     * schedule generator (so future/past dates work without persisted instances); completion
     * comes from any stored instances matched by deterministic id.
     */
    fun dayIndicator(
        date: LocalDate,
        petId: String,
        tasks: List<CareTask>,
        storedInstances: List<DailyTaskInstance>,
        notes: List<PetNote>,
        inMonth: Boolean = true
    ): DayIndicator {
        val dateIso = date.format(DateTimeUtil.STORAGE_DATE)
        val storedById = storedInstances.associateBy { it.id }

        var total = 0
        var completed = 0
        var skipped = 0

        tasks.asSequence()
            .filter { it.petId == petId && TaskGenerator.isActiveOn(it, date) }
            .forEach { task ->
                TaskGenerator.scheduledTimesFor(task).forEach { time ->
                    total += 1
                    val id = IdGen.instanceId(task.id, dateIso, time)
                    when (storedById[id]?.status) {
                        TaskStatus.Completed -> completed += 1
                        TaskStatus.Skipped -> skipped += 1
                        else -> { /* pending or not stored */ }
                    }
                }
            }

        val hasNote = notes.any { it.petId == petId && it.date == dateIso }

        val status = when {
            total == 0 -> DayStatus.NoData
            completed == total -> DayStatus.AllComplete
            completed > 0 -> DayStatus.Partial
            else -> DayStatus.NoneComplete
        }

        return DayIndicator(date, dateIso, total, completed, skipped, hasNote, status, inMonth)
    }

    /** Build a full month grid (6 rows) with leading/trailing days from adjacent months. */
    fun buildMonth(
        yearMonth: YearMonth,
        firstDayOfWeek: WeekDay,
        petId: String,
        tasks: List<CareTask>,
        storedInstances: List<DailyTaskInstance>,
        notes: List<PetNote>
    ): CalendarMonth {
        val firstOfMonth = yearMonth.atDay(1)
        val firstDowIso = firstDayOfWeek.isoNumber
        // How many days to step back so the grid starts on firstDayOfWeek.
        val offset = ((firstOfMonth.dayOfWeek.value - firstDowIso) + 7) % 7
        val gridStart = firstOfMonth.minusDays(offset.toLong())

        val cells = (0 until 42).map { i ->
            val date = gridStart.plusDays(i.toLong())
            dayIndicator(
                date = date,
                petId = petId,
                tasks = tasks,
                storedInstances = storedInstances,
                notes = notes,
                inMonth = YearMonth.from(date) == yearMonth
            )
        }

        val headers = (0 until 7).map { i ->
            WeekDay.fromIso(((firstDowIso - 1 + i) % 7) + 1).shortLabel
        }

        return CalendarMonth(
            yearMonth = yearMonth,
            title = DateTimeUtil.monthTitle(yearMonth),
            weekHeaders = headers,
            cells = cells
        )
    }
}
