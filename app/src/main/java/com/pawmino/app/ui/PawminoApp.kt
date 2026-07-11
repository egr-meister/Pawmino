package com.pawmino.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pawmino.app.ui.navigation.AppNavigator
import com.pawmino.app.ui.navigation.Routes
import com.pawmino.app.ui.screens.AddEditCareTaskScreen
import com.pawmino.app.ui.screens.AddEditNoteScreen
import com.pawmino.app.ui.screens.AddEditPetScreen
import com.pawmino.app.ui.screens.CalendarScreen
import com.pawmino.app.ui.screens.DailyTaskDetailScreen
import com.pawmino.app.ui.screens.DayDetailScreen
import com.pawmino.app.ui.screens.HistoryScreen
import com.pawmino.app.ui.screens.NotesScreen
import com.pawmino.app.ui.screens.OnboardingScreen
import com.pawmino.app.ui.screens.PetProfilesScreen
import com.pawmino.app.ui.screens.PetSetupScreen
import com.pawmino.app.ui.screens.SettingsScreen
import com.pawmino.app.ui.screens.ShoppingListScreen
import com.pawmino.app.ui.screens.TaskListScreen
import com.pawmino.app.ui.screens.TodayScreen
import com.pawmino.app.ui.screens.WalkLogScreen
import com.pawmino.app.ui.viewmodel.PawminoViewModel

private data class BottomDestination(val route: String, val label: String, val icon: ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(Routes.TODAY, "Today", Icons.Outlined.Home),
    BottomDestination(Routes.CALENDAR, "Calendar", Icons.Outlined.CalendarMonth),
    BottomDestination(Routes.HISTORY, "History", Icons.Outlined.History),
    BottomDestination(Routes.SHOPPING, "Shopping", Icons.Outlined.ShoppingCart),
    BottomDestination(Routes.SETTINGS, "Settings", Icons.Outlined.Settings)
)

@Composable
fun PawminoApp() {
    val vm: PawminoViewModel = viewModel(factory = PawminoViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()

    if (!state.loaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val nav = rememberNavigator(navController)

    val startDestination = if (state.data.settings.onboardingCompleted) Routes.TODAY else Routes.ONBOARDING

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = { nav.toTab(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onCreatePet = { nav.toPetSetup() },
                    onExplore = {
                        vm.completeOnboarding()
                        nav.toMainRoot()
                    }
                )
            }
            composable(Routes.PET_SETUP) {
                PetSetupScreen(vm = vm, state = state, onSaved = {
                    vm.completeOnboarding()
                    nav.toMainRoot()
                }, onBack = { nav.back() })
            }

            composable(Routes.TODAY) { TodayScreen(vm, state, nav) }
            composable(Routes.CALENDAR) { CalendarScreen(vm, state, nav) }
            composable(Routes.HISTORY) { HistoryScreen(vm, state, nav) }
            composable(Routes.SHOPPING) { ShoppingListScreen(vm, state, nav) }
            composable(Routes.SETTINGS) { SettingsScreen(vm, state, nav) }

            composable(Routes.TASK_LIST) { TaskListScreen(vm, state, nav) }
            composable(Routes.PET_PROFILES) { PetProfilesScreen(vm, state, nav) }
            composable(Routes.NOTES) { NotesScreen(vm, state, nav) }

            composable(
                route = Routes.ADD_EDIT_TASK,
                arguments = listOf(navArgument("taskId") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                val taskId = entry.arguments?.getString("taskId").orEmpty().ifBlank { null }
                AddEditCareTaskScreen(vm, state, taskId, nav)
            }

            composable(
                route = Routes.TASK_DETAIL,
                arguments = listOf(navArgument("instanceId") { type = NavType.StringType })
            ) { entry ->
                val instanceId = entry.arguments?.getString("instanceId").orEmpty()
                DailyTaskDetailScreen(vm, state, instanceId, nav)
            }

            composable(
                route = Routes.ADD_EDIT_PET,
                arguments = listOf(navArgument("petId") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                val petId = entry.arguments?.getString("petId").orEmpty().ifBlank { null }
                AddEditPetScreen(vm, state, petId, nav)
            }

            composable(
                route = Routes.ADD_EDIT_NOTE,
                arguments = listOf(
                    navArgument("noteId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("date") { type = NavType.StringType; defaultValue = "" },
                    navArgument("instanceId") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val noteId = entry.arguments?.getString("noteId").orEmpty().ifBlank { null }
                val date = entry.arguments?.getString("date").orEmpty().ifBlank { null }
                val instanceId = entry.arguments?.getString("instanceId").orEmpty().ifBlank { null }
                AddEditNoteScreen(vm, state, noteId, date, instanceId, nav)
            }

            composable(
                route = Routes.WALK_LOG,
                arguments = listOf(
                    navArgument("walkId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("date") { type = NavType.StringType; defaultValue = "" },
                    navArgument("instanceId") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val walkId = entry.arguments?.getString("walkId").orEmpty().ifBlank { null }
                val date = entry.arguments?.getString("date").orEmpty().ifBlank { null }
                val instanceId = entry.arguments?.getString("instanceId").orEmpty().ifBlank { null }
                WalkLogScreen(vm, state, walkId, date, instanceId, nav)
            }

            composable(
                route = Routes.DAY_DETAIL,
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { entry ->
                val date = entry.arguments?.getString("date").orEmpty()
                DayDetailScreen(vm, state, date, nav)
            }
        }
    }
}

@Composable
private fun rememberNavigator(navController: androidx.navigation.NavHostController): AppNavigator {
    return androidx.compose.runtime.remember(navController) { AppNavigator(navController) }
}
