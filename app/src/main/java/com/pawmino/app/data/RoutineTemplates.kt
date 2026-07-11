package com.pawmino.app.data

import com.pawmino.app.model.CareCategory
import com.pawmino.app.model.CareTask
import com.pawmino.app.model.ScheduleType
import com.pawmino.app.util.DateTimeUtil
import com.pawmino.app.util.IdGen

/**
 * Optional local starter routines. These are editable examples only — they are NOT
 * veterinary recommendations and are never adapted to any health condition.
 */
data class RoutineTemplate(
    val key: String,
    val name: String,
    val description: String,
    private val entries: List<TemplateEntry>
) {
    fun build(petId: String): List<CareTask> {
        val now = DateTimeUtil.nowTimestamp()
        return entries.mapIndexed { index, entry ->
            CareTask(
                id = IdGen.newId(),
                petId = petId,
                title = entry.title,
                category = entry.category,
                scheduleType = ScheduleType.Daily,
                times = entry.time?.let { listOf(it) } ?: emptyList(),
                enabled = true,
                sortOrder = index,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}

data class TemplateEntry(
    val title: String,
    val category: CareCategory,
    val time: String? = null
)

object RoutineTemplates {

    const val DISCLAIMER =
        "Routine templates are editable examples and are not veterinary recommendations."

    val dogBasic = RoutineTemplate(
        key = "dog_basic",
        name = "Dog Basic Routine",
        description = "Feeding, walks and play across the day.",
        entries = listOf(
            TemplateEntry("Morning feeding", CareCategory.Feeding, "07:30"),
            TemplateEntry("Morning walk", CareCategory.Walk, "08:00"),
            TemplateEntry("Play session", CareCategory.Play, "16:00"),
            TemplateEntry("Evening feeding", CareCategory.Feeding, "18:30"),
            TemplateEntry("Evening walk", CareCategory.Walk, "20:00")
        )
    )

    val catBasic = RoutineTemplate(
        key = "cat_basic",
        name = "Cat Basic Routine",
        description = "Feeding, water, play and litter care.",
        entries = listOf(
            TemplateEntry("Morning feeding", CareCategory.Feeding, "07:30"),
            TemplateEntry("Refresh water", CareCategory.Water, "08:00"),
            TemplateEntry("Play session", CareCategory.Play, "17:00"),
            TemplateEntry("Evening feeding", CareCategory.Feeding, "18:30"),
            TemplateEntry("Clean litter box", CareCategory.Litter, "19:00")
        )
    )

    val smallPetBasic = RoutineTemplate(
        key = "small_pet_basic",
        name = "Small Pet Basic Routine",
        description = "Feeding, water, habitat cleaning and interaction.",
        entries = listOf(
            TemplateEntry("Feeding", CareCategory.Feeding, "08:00"),
            TemplateEntry("Refresh water", CareCategory.Water, "08:15"),
            TemplateEntry("Clean habitat", CareCategory.Cleaning, "10:00"),
            TemplateEntry("Play or interaction", CareCategory.Play, "18:00")
        )
    )

    val all: List<RoutineTemplate> = listOf(dogBasic, catBasic, smallPetBasic)

    fun byKey(key: String?): RoutineTemplate? = all.firstOrNull { it.key == key }
}
