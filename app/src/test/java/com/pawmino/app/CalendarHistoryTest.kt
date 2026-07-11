package com.pawmino.app

import com.pawmino.app.model.CareCategory
import com.pawmino.app.model.PetNote
import com.pawmino.app.model.TaskStatus
import com.pawmino.app.model.WeekDay
import com.pawmino.app.util.CalendarUtil
import com.pawmino.app.util.DayStatus
import com.pawmino.app.util.HistoryUtil
import com.pawmino.app.util.IdGen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarHistoryTest {

    @Test
    fun dayIndicator_allCompleteWhenEveryInstanceDone() {
        val task = TestData.task("t", times = listOf("08:00"))
        val date = LocalDate.of(2026, 7, 10)
        val id = IdGen.instanceId("t", "2026-07-10", "08:00")
        val stored = listOf(TestData.instance(id, "t", date = "2026-07-10", time = "08:00", status = TaskStatus.Completed))
        val indicator = CalendarUtil.dayIndicator(date, "pet1", listOf(task), stored, emptyList())
        assertEquals(1, indicator.total)
        assertEquals(1, indicator.completed)
        assertEquals(DayStatus.AllComplete, indicator.status)
    }

    @Test
    fun dayIndicator_noneCompleteWhenTasksExistButUnfinished() {
        val task = TestData.task("t", times = listOf("08:00"))
        val date = LocalDate.of(2026, 7, 10)
        val indicator = CalendarUtil.dayIndicator(date, "pet1", listOf(task), emptyList(), emptyList())
        assertEquals(DayStatus.NoneComplete, indicator.status)
    }

    @Test
    fun dayIndicator_noDataWhenNoTasks() {
        val date = LocalDate.of(2026, 7, 10)
        val indicator = CalendarUtil.dayIndicator(date, "pet1", emptyList(), emptyList(), emptyList())
        assertEquals(DayStatus.NoData, indicator.status)
    }

    @Test
    fun buildMonth_produces42CellsAndCorrectHeaders() {
        val month = CalendarUtil.buildMonth(
            yearMonth = YearMonth.of(2026, 7),
            firstDayOfWeek = WeekDay.Monday,
            petId = "pet1",
            tasks = emptyList(),
            storedInstances = emptyList(),
            notes = emptyList()
        )
        assertEquals(42, month.cells.size)
        assertEquals("Mon", month.weekHeaders.first())
        assertEquals(7, month.weekHeaders.size)
    }

    @Test
    fun history_reverseChronologicalWithCounts() {
        val task = TestData.task("t", category = CareCategory.Feeding, times = listOf("08:00"))
        val instances = listOf(
            TestData.instance("i1", "t", date = "2026-07-08", status = TaskStatus.Completed),
            TestData.instance("i2", "t", date = "2026-07-10", status = TaskStatus.Skipped)
        )
        val notes = listOf(PetNote(id = "n1", petId = "pet1", date = "2026-07-09", title = "Note"))
        val summaries = HistoryUtil.buildDailySummaries("pet1", listOf(task), instances, notes, emptyList())
        assertEquals(listOf("2026-07-10", "2026-07-09", "2026-07-08"), summaries.map { it.dateIso })
        val first = summaries.first { it.dateIso == "2026-07-08" }
        assertEquals(1, first.completed)
        assertTrue(summaries.first { it.dateIso == "2026-07-09" }.noteCount == 1)
    }
}
