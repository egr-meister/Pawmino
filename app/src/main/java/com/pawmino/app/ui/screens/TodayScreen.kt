package com.pawmino.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pawmino.app.model.CareCategory
import com.pawmino.app.model.TaskStatus
import com.pawmino.app.ui.components.CareLoop
import com.pawmino.app.ui.components.CategoryChipsRow
import com.pawmino.app.ui.components.DateHeaderStrip
import com.pawmino.app.ui.components.EmptyState
import com.pawmino.app.ui.components.NextTaskRibbon
import com.pawmino.app.ui.components.PetSwitcher
import com.pawmino.app.ui.components.ReminderPanel
import com.pawmino.app.ui.components.SectionTitle
import com.pawmino.app.ui.components.ShoppingPreview
import com.pawmino.app.ui.components.TaskRow
import com.pawmino.app.ui.navigation.AppNavigator
import com.pawmino.app.ui.rememberPrefer24
import com.pawmino.app.ui.viewmodel.PawminoUiState
import com.pawmino.app.ui.viewmodel.PawminoViewModel
import com.pawmino.app.util.CareLoopCalculator
import com.pawmino.app.util.DateTimeUtil
import com.pawmino.app.util.ReminderUtil
import com.pawmino.app.util.TaskGenerator
import com.pawmino.app.util.TimelineUtil
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TodayScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    nav: AppNavigator
) {
    val activePet = state.activePet
    val prefer24 = rememberPrefer24(state.data.settings.timeFormat)
    var categoryFilter by remember { mutableStateOf<CareCategory?>(null) }

    // Generate any missing instances for the active pet + selected date, without background work.
    LaunchedEffect(activePet?.id, state.selectedDateIso, state.data.careTasks.size) {
        activePet?.id?.let { vm.ensureInstancesForActive(it, state.selectedDateIso) }
    }

    if (activePet == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "Welcome to Pawmino",
                message = "Add a pet profile to begin organizing daily care.",
                icon = Icons.Outlined.Pets,
                actionLabel = "Add Pet",
                onAction = { nav.toAddPet() }
            )
        }
        return
    }

    val tasks = state.data.careTasks
    val activeInstances = TaskGenerator.activeInstancesFor(
        state.data.taskInstances, tasks, activePet.id, state.selectedDateIso
    )
    val loop = CareLoopCalculator.build(activeInstances, tasks)
    val taskById = tasks.associateBy { it.id }

    val isToday = DateTimeUtil.isToday(state.selectedDateIso)
    val now = LocalTime.now()
    val next = ReminderUtil.nextTask(activeInstances, tasks, now)
    val reminders = ReminderUtil.computeReminders(
        isToday = isToday,
        activeInstances = activeInstances,
        tasks = tasks,
        shoppingItems = state.data.shoppingItems,
        activePetId = activePet.id,
        settings = state.data.settings.reminderSettings,
        now = now,
        dismissedKinds = state.dismissedReminders
    )

    val filteredInstances = if (categoryFilter == null) activeInstances
        else activeInstances.filter { taskById[it.taskId]?.category == categoryFilter }
    val grouped = TimelineUtil.grouped(filteredInstances, tasks)

    val previewShopping = state.data.shoppingItems
        .filter { !it.checked && (it.petId == null || it.petId == activePet.id) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp, top = 8.dp)
        ) {
            item {
                PetSwitcher(
                    pets = state.pets,
                    activePetId = activePet.id,
                    onSelect = { vm.selectPet(it) },
                    onAddPet = { nav.toAddPet() }
                )
            }
            item {
                DateHeaderStrip(
                    dateIso = state.selectedDateIso,
                    isToday = isToday,
                    onPrev = { shiftDate(vm, state.selectedDateIso, -1) },
                    onNext = { shiftDate(vm, state.selectedDateIso, 1) },
                    onToday = { vm.goToToday() }
                )
            }
            if (reminders.isNotEmpty()) {
                item {
                    ReminderPanel(
                        reminders = reminders,
                        onOpenTask = { nav.toTaskDetail(it) },
                        onOpenShopping = { nav.toShopping() },
                        onDismiss = { vm.dismissReminder(it) }
                    )
                }
            }
            item {
                CareLoop(summary = loop, petName = activePet.name, modifier = Modifier.padding(top = 4.dp))
            }
            item {
                NextTaskRibbon(
                    next = next,
                    allComplete = loop.allComplete,
                    prefer24 = prefer24,
                    onOpen = { nav.toTaskDetail(it) }
                )
            }
            if (loop.segments.isNotEmpty()) {
                item {
                    CategoryChipsRow(
                        segments = loop.segments,
                        selected = categoryFilter,
                        onSelect = { categoryFilter = it }
                    )
                }
            }

            if (loop.isEmpty) {
                item {
                    EmptyState(
                        title = "No care tasks for today",
                        message = "Create a routine for ${activePet.name}.",
                        actionLabel = "Add Task",
                        onAction = { nav.toAddTask() }
                    )
                }
            } else {
                grouped.forEach { (section, items) ->
                    item {
                        SectionTitle(section.label, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                    }
                    taskRows(items, taskById, prefer24, nav, vm)
                }
            }

            item {
                SectionTitle("Nearby", modifier = Modifier.padding(start = 16.dp, top = 8.dp))
            }
            item {
                ShoppingPreview(
                    items = previewShopping,
                    onOpen = { nav.toShopping() },
                    onToggle = { vm.setShoppingChecked(it.id, !it.checked) }
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        ExtendedFloatingActionButton(
            onClick = { nav.toAddTask() },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add Task") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.taskRows(
    items: List<com.pawmino.app.model.DailyTaskInstance>,
    taskById: Map<String, com.pawmino.app.model.CareTask>,
    prefer24: Boolean,
    nav: AppNavigator,
    vm: PawminoViewModel
) {
    items.forEach { instance ->
        item(key = instance.id) {
            TaskRow(
                instance = instance,
                task = taskById[instance.taskId],
                prefer24 = prefer24,
                onClick = { nav.toTaskDetail(instance.id) },
                onToggleComplete = {
                    val newStatus = if (instance.status == TaskStatus.Completed) TaskStatus.Pending else TaskStatus.Completed
                    vm.setInstanceStatus(instance, newStatus)
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

private fun shiftDate(vm: PawminoViewModel, currentIso: String, days: Long) {
    val date = DateTimeUtil.parseDateOrNull(currentIso) ?: LocalDate.now()
    vm.selectDate(date.plusDays(days).format(DateTimeUtil.STORAGE_DATE))
}
