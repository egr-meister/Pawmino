package com.pawmino.app.ui.navigation

import android.net.Uri

/**
 * Central route definitions. Arguments are URL-encoded so ids that contain reserved
 * characters (like the deterministic instance id) are passed safely.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val PET_SETUP = "pet_setup"

    // Top-level (bottom navigation) destinations.
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val HISTORY = "history"
    const val SHOPPING = "shopping"
    const val SETTINGS = "settings"

    const val TASK_LIST = "task_list"
    const val PET_PROFILES = "pet_profiles"
    const val NOTES = "notes"

    // Parameterized routes.
    const val ADD_EDIT_TASK = "add_edit_task?taskId={taskId}"
    const val TASK_DETAIL = "task_detail/{instanceId}"
    const val ADD_EDIT_PET = "add_edit_pet?petId={petId}"
    const val ADD_EDIT_NOTE = "add_edit_note?noteId={noteId}&date={date}&instanceId={instanceId}"
    const val WALK_LOG = "walk_log?walkId={walkId}&date={date}&instanceId={instanceId}"
    const val DAY_DETAIL = "day_detail/{date}"

    fun addEditTask(taskId: String? = null): String =
        "add_edit_task?taskId=${taskId ?: ""}"

    fun taskDetail(instanceId: String): String =
        "task_detail/${Uri.encode(instanceId)}"

    fun addEditPet(petId: String? = null): String =
        "add_edit_pet?petId=${petId ?: ""}"

    fun addEditNote(noteId: String? = null, date: String? = null, instanceId: String? = null): String =
        "add_edit_note?noteId=${noteId ?: ""}&date=${date ?: ""}&instanceId=${instanceId ?: ""}"

    fun walkLog(walkId: String? = null, date: String? = null, instanceId: String? = null): String =
        "walk_log?walkId=${walkId ?: ""}&date=${date ?: ""}&instanceId=${instanceId ?: ""}"

    fun dayDetail(date: String): String = "day_detail/${Uri.encode(date)}"

    val topLevelRoutes = setOf(TODAY, CALENDAR, HISTORY, SHOPPING, SETTINGS)
}
