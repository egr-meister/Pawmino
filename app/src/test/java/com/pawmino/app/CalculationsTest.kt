package com.pawmino.app

import com.pawmino.app.model.CareCategory
import com.pawmino.app.model.ShoppingCategory
import com.pawmino.app.model.ShoppingItem
import com.pawmino.app.model.ShoppingPriority
import com.pawmino.app.model.TaskStatus
import com.pawmino.app.util.CareLoopCalculator
import com.pawmino.app.util.ReminderUtil
import com.pawmino.app.util.ShoppingUtil
import com.pawmino.app.util.TimelineUtil
import com.pawmino.app.util.DaySection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class CalculationsTest {

    @Test
    fun percent_handlesZeroTotal() {
        assertEquals(0, CareLoopCalculator.percent(0, 0))
        assertEquals(75, CareLoopCalculator.percent(3, 4))
        assertEquals(100, CareLoopCalculator.percent(4, 4))
    }

    @Test
    fun loop_dailyAndCategoryPercentages() {
        val feed = TestData.task("f", category = CareCategory.Feeding)
        val walk = TestData.task("w", category = CareCategory.Walk)
        val tasks = listOf(feed, walk)
        val instances = listOf(
            TestData.instance("i1", "f", date = "2026-07-10", status = TaskStatus.Completed),
            TestData.instance("i2", "f", date = "2026-07-10", status = TaskStatus.Pending),
            TestData.instance("i3", "w", date = "2026-07-10", status = TaskStatus.Completed)
        )
        val loop = CareLoopCalculator.build(instances, tasks)
        assertEquals(3, loop.totalTasks)
        assertEquals(2, loop.completedTasks)
        assertEquals(67, loop.percent)

        val feeding = loop.segments.first { it.category == CareCategory.Feeding }
        assertEquals(2, feeding.total)
        assertEquals(1, feeding.completed)
        assertEquals(0.5f, feeding.fraction, 0.001f)
    }

    @Test
    fun loop_skippedCountsInTotalNotCompleted() {
        val feed = TestData.task("f", category = CareCategory.Feeding)
        val instances = listOf(
            TestData.instance("i1", "f", date = "2026-07-10", status = TaskStatus.Skipped),
            TestData.instance("i2", "f", date = "2026-07-10", status = TaskStatus.Completed)
        )
        val loop = CareLoopCalculator.build(instances, listOf(feed))
        assertEquals(2, loop.totalTasks)
        assertEquals(1, loop.completedTasks)
        assertEquals(1, loop.skippedTasks)
        assertEquals(50, loop.percent)
    }

    @Test
    fun reminder_leadTimeCalculation() {
        val now = LocalTime.of(18, 45)
        assertEquals(15L, ReminderUtil.minutesUntil("19:00", now))
        assertEquals(-15L, ReminderUtil.minutesUntil("18:30", now))
        assertNull(ReminderUtil.minutesUntil("bad", now))
    }

    @Test
    fun reminder_overdueTaskDetected() {
        val task = TestData.task("t", times = listOf("07:00"))
        val instance = TestData.instance("i1", "t", date = "2026-07-10", time = "07:00", status = TaskStatus.Pending)
        val reminders = ReminderUtil.computeReminders(
            isToday = true,
            activeInstances = listOf(instance),
            tasks = listOf(task),
            shoppingItems = emptyList(),
            activePetId = "pet1",
            settings = com.pawmino.app.model.ReminderSettings(enabled = true, leadTimeMinutes = 30),
            now = LocalTime.of(9, 0)
        )
        assertTrue(reminders.any { it.kind == com.pawmino.app.util.ReminderKind.Overdue })
    }

    @Test
    fun reminder_disabledProducesNothing() {
        val task = TestData.task("t", times = listOf("07:00"))
        val instance = TestData.instance("i1", "t", date = "2026-07-10", time = "07:00")
        val reminders = ReminderUtil.computeReminders(
            isToday = true,
            activeInstances = listOf(instance),
            tasks = listOf(task),
            shoppingItems = emptyList(),
            activePetId = "pet1",
            settings = com.pawmino.app.model.ReminderSettings(enabled = false),
            now = LocalTime.of(9, 0)
        )
        assertTrue(reminders.isEmpty())
    }

    @Test
    fun shopping_sortUncheckedAndHighPriorityFirst() {
        val a = ShoppingItem(id = "a", title = "Checked normal", checked = true, createdAt = "1")
        val b = ShoppingItem(id = "b", title = "Unchecked normal", priority = ShoppingPriority.Normal, createdAt = "2")
        val c = ShoppingItem(id = "c", title = "Unchecked high", priority = ShoppingPriority.High, createdAt = "3")
        val sorted = ShoppingUtil.sort(listOf(a, b, c))
        assertEquals(listOf("c", "b", "a"), sorted.map { it.id })
    }

    @Test
    fun shopping_filterByCategoryAndPet() {
        val items = listOf(
            ShoppingItem(id = "a", petId = "pet1", title = "Food", category = ShoppingCategory.Food),
            ShoppingItem(id = "b", petId = "pet2", title = "Toy", category = ShoppingCategory.Toys),
            ShoppingItem(id = "c", petId = null, title = "Shared", category = ShoppingCategory.Food)
        )
        val filtered = ShoppingUtil.filter(items, ShoppingCategory.Food, "pet1", includeAllPets = true)
        assertEquals(setOf("a", "c"), filtered.map { it.id }.toSet())
    }

    @Test
    fun timeline_sectionAssignment() {
        assertEquals(DaySection.Morning, TimelineUtil.sectionFor("08:00"))
        assertEquals(DaySection.Afternoon, TimelineUtil.sectionFor("13:00"))
        assertEquals(DaySection.Evening, TimelineUtil.sectionFor("19:30"))
        assertEquals(DaySection.Anytime, TimelineUtil.sectionFor(""))
    }
}
