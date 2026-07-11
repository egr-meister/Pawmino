package com.pawmino.app.model

import kotlinx.serialization.Serializable

/**
 * Pawmino data models.
 *
 * Every field has a default value so that older stored JSON that predates a newly
 * added field still deserializes cleanly (backward-compatible deserialization).
 * Dates are stored as "yyyy-MM-dd", times as "HH:mm", timestamps as ISO-8601 strings.
 */

@Serializable
data class PetProfile(
    val id: String = "",
    val name: String = "",
    val petType: PetType = PetType.Other,
    val breed: String = "",
    val birthDate: String = "",
    val adoptionDate: String = "",
    val colorDescription: String = "",
    val notes: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class CareTask(
    val id: String = "",
    val petId: String = "",
    val title: String = "",
    val category: CareCategory = CareCategory.Other,
    val customCategoryName: String = "",
    val scheduleType: ScheduleType = ScheduleType.Daily,
    val daysOfWeek: List<WeekDay> = emptyList(),
    val specificDate: String = "",
    val times: List<String> = emptyList(),
    val repeatIntervalDays: Int? = null,
    val anchorDate: String = "",
    val enabled: Boolean = true,
    val portionLabel: String = "",
    val foodLabel: String = "",
    val notes: String = "",
    val sortOrder: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    /** Effective display name for the category (respects custom naming for Other). */
    fun categoryDisplayName(): String =
        if (category == CareCategory.Other && customCategoryName.isNotBlank()) {
            customCategoryName.trim()
        } else {
            category.label
        }
}

@Serializable
data class DailyTaskInstance(
    val id: String = "",
    val taskId: String = "",
    val petId: String = "",
    val date: String = "",
    val scheduledTime: String = "",
    val status: TaskStatus = TaskStatus.Pending,
    val completedAt: String = "",
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class WalkLog(
    val id: String = "",
    val petId: String = "",
    val date: String = "",
    val startTime: String = "",
    val durationMinutes: Int? = null,
    val distance: Double? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.NotTracked,
    val note: String = "",
    val linkedTaskInstanceId: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class PetNote(
    val id: String = "",
    val petId: String = "",
    val date: String = "",
    val time: String = "",
    val title: String = "",
    val text: String = "",
    val linkedTaskInstanceId: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class ShoppingItem(
    val id: String = "",
    val petId: String? = null,
    val title: String = "",
    val category: ShoppingCategory = ShoppingCategory.Other,
    val quantityLabel: String = "",
    val priority: ShoppingPriority = ShoppingPriority.Normal,
    val checked: Boolean = false,
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class ReminderSettings(
    val enabled: Boolean = true,
    val leadTimeMinutes: Int = 30,
    val showOverdueTasks: Boolean = true,
    val showShoppingReminder: Boolean = true
)

@Serializable
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val activePetId: String? = null,
    val firstDayOfWeek: WeekDay = WeekDay.Monday,
    val timeFormat: TimeFormatPreference = TimeFormatPreference.SystemDefault,
    val reminderSettings: ReminderSettings = ReminderSettings()
)

@Serializable
data class AppData(
    val pets: List<PetProfile> = emptyList(),
    val careTasks: List<CareTask> = emptyList(),
    val taskInstances: List<DailyTaskInstance> = emptyList(),
    val walkLogs: List<WalkLog> = emptyList(),
    val petNotes: List<PetNote> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val settings: AppSettings = AppSettings()
)
