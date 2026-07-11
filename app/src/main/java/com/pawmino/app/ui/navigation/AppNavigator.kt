package com.pawmino.app.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

/**
 * Thin wrapper over the NavController so screens depend on a small set of named actions
 * rather than raw route strings. All navigation is safe: back is a no-op at the root.
 */
class AppNavigator(val navController: NavHostController) {

    fun back() {
        if (!navController.popBackStack()) {
            // Already at the root; nothing to do.
        }
    }

    private fun go(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
        navController.navigate(route, builder)
    }

    fun toTab(route: String) = go(route) {
        popUpTo(Routes.TODAY) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    fun toToday() = toTab(Routes.TODAY)
    fun toShopping() = toTab(Routes.SHOPPING)

    fun toOnboarding() = go(Routes.ONBOARDING) {
        popUpTo(0) { inclusive = true }
    }

    fun toMainRoot() = go(Routes.TODAY) {
        popUpTo(0) { inclusive = true }
    }

    fun toPetSetup() = go(Routes.PET_SETUP)
    fun toTaskList() = go(Routes.TASK_LIST)
    fun toPetProfiles() = go(Routes.PET_PROFILES)
    fun toNotes() = go(Routes.NOTES)

    fun toAddTask(taskId: String? = null) = go(Routes.addEditTask(taskId))
    fun toTaskDetail(instanceId: String) = go(Routes.taskDetail(instanceId))
    fun toAddPet(petId: String? = null) = go(Routes.addEditPet(petId))
    fun toAddNote(noteId: String? = null, date: String? = null, instanceId: String? = null) =
        go(Routes.addEditNote(noteId, date, instanceId))
    fun toWalk(walkId: String? = null, date: String? = null, instanceId: String? = null) =
        go(Routes.walkLog(walkId, date, instanceId))
    fun toDayDetail(date: String) = go(Routes.dayDetail(date))
}
