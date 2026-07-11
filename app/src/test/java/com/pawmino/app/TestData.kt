package com.pawmino.app

import com.pawmino.app.model.CareCategory
import com.pawmino.app.model.CareTask
import com.pawmino.app.model.DailyTaskInstance
import com.pawmino.app.model.ScheduleType
import com.pawmino.app.model.TaskStatus
import com.pawmino.app.model.WeekDay

/** Small builders to keep tests readable. */
object TestData {

    fun task(
        id: String,
        petId: String = "pet1",
        title: String = "Task $id",
        category: CareCategory = CareCategory.Feeding,
        scheduleType: ScheduleType = ScheduleType.Daily,
        daysOfWeek: List<WeekDay> = emptyList(),
        specificDate: String = "",
        times: List<String> = emptyList(),
        repeatIntervalDays: Int? = null,
        anchorDate: String = "",
        enabled: Boolean = true,
        sortOrder: Int = 0,
        createdAt: String = "2026-01-01T00:00:00Z"
    ): CareTask = CareTask(
        id = id,
        petId = petId,
        title = title,
        category = category,
        scheduleType = scheduleType,
        daysOfWeek = daysOfWeek,
        specificDate = specificDate,
        times = times,
        repeatIntervalDays = repeatIntervalDays,
        anchorDate = anchorDate,
        enabled = enabled,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = createdAt
    )

    fun instance(
        id: String,
        taskId: String,
        petId: String = "pet1",
        date: String,
        time: String = "",
        status: TaskStatus = TaskStatus.Pending
    ): DailyTaskInstance = DailyTaskInstance(
        id = id,
        taskId = taskId,
        petId = petId,
        date = date,
        scheduledTime = time,
        status = status
    )
}
