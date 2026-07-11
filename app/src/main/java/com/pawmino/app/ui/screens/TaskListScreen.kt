package com.pawmino.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pawmino.app.model.CareCategory
import com.pawmino.app.model.CareTask
import com.pawmino.app.model.ScheduleType
import com.pawmino.app.ui.components.CategoryDot
import com.pawmino.app.ui.components.ConfirmDialog
import com.pawmino.app.ui.components.EmptyState
import com.pawmino.app.ui.components.PawTopBar
import com.pawmino.app.ui.components.SectionTitle
import com.pawmino.app.ui.navigation.AppNavigator
import com.pawmino.app.ui.viewmodel.PawminoUiState
import com.pawmino.app.ui.viewmodel.PawminoViewModel
import com.pawmino.app.util.DateTimeUtil

@Composable
fun TaskListScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    nav: AppNavigator
) {
    val pet = state.activePet
    var pendingDelete by remember { mutableStateOf<CareTask?>(null) }

    Scaffold(
        topBar = {
            PawTopBar(title = "Tasks${pet?.let { " · ${it.name}" } ?: ""}", onBack = { nav.back() }, actions = {
                TextButton(onClick = { nav.toAddTask() }) { Text("Add") }
            })
        }
    ) { padding ->
        if (pet == null) {
            EmptyState(
                title = "No pet selected",
                message = "Add a pet profile to manage care tasks.",
                actionLabel = "Add Pet",
                onAction = { nav.toAddPet() },
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        val tasks = state.data.careTasks.filter { it.petId == pet.id }
        if (tasks.isEmpty()) {
            EmptyState(
                title = "No care tasks",
                message = "Create a routine for ${pet.name}.",
                actionLabel = "Add Task",
                onAction = { nav.toAddTask() },
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        val grouped = CareCategory.entries.mapNotNull { cat ->
            val items = tasks.filter { it.category == cat }
                .sortedWith(compareBy({ it.sortOrder }, { it.title.lowercase() }))
            if (items.isEmpty()) null else cat to items
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            grouped.forEach { (cat, catTasks) ->
                item(key = "header_${cat.name}") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        CategoryDot(cat)
                        SectionTitle(cat.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                items(catTasks, key = { it.id }) { task ->
                    TaskConfigRow(
                        task = task,
                        onEdit = { nav.toAddTask(task.id) },
                        onToggleEnabled = { vm.setTaskEnabled(task.id, it) },
                        onDuplicate = { vm.duplicateTask(task) },
                        onDelete = { pendingDelete = task },
                        onMoveUp = { vm.moveTask(task.id, up = true) },
                        onMoveDown = { vm.moveTask(task.id, up = false) },
                        onAddToDay = if (task.scheduleType == ScheduleType.Unscheduled) {
                            { vm.addManualInstance(task, state.selectedDateIso, "") }
                        } else null
                    )
                }
            }
        }
    }

    pendingDelete?.let { task ->
        ConfirmDialog(
            title = "Delete task?",
            text = "\"${task.title}\" will be removed. Past completion history is kept.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { vm.deleteTask(task.id); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun TaskConfigRow(
    task: CareTask,
    onEdit: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddToDay: (() -> Unit)?
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (task.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    scheduleSummary(task),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = task.enabled, onCheckedChange = onToggleEnabled)
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit() })
                DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menuOpen = false; onDuplicate() })
                if (onAddToDay != null) {
                    DropdownMenuItem(text = { Text("Add to selected day") }, onClick = { menuOpen = false; onAddToDay() })
                }
                DropdownMenuItem(
                    text = { Text("Move up") },
                    leadingIcon = { Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = null) },
                    onClick = { menuOpen = false; onMoveUp() }
                )
                DropdownMenuItem(
                    text = { Text("Move down") },
                    leadingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null) },
                    onClick = { menuOpen = false; onMoveDown() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

private fun scheduleSummary(task: CareTask): String {
    val schedule = when (task.scheduleType) {
        ScheduleType.Daily -> "Every day"
        ScheduleType.SelectedDays -> task.daysOfWeek.joinToString(", ") { it.shortLabel }.ifBlank { "Selected days" }
        ScheduleType.OneTime -> "Once on ${DateTimeUtil.displayDateShort(task.specificDate)}"
        ScheduleType.EveryNumberOfDays -> "Every ${task.repeatIntervalDays ?: 1} days"
        ScheduleType.Unscheduled -> "Unscheduled"
    }
    val times = if (task.times.isEmpty()) "Anytime" else task.times.joinToString(", ")
    val disabled = if (task.enabled) "" else " · Disabled"
    return "$schedule · $times$disabled"
}
