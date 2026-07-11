package com.pawmino.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pawmino.app.model.TaskStatus
import com.pawmino.app.ui.components.EmptyState
import com.pawmino.app.ui.components.PawTopBar
import com.pawmino.app.ui.components.SectionTitle
import com.pawmino.app.ui.components.TaskRow
import com.pawmino.app.ui.navigation.AppNavigator
import com.pawmino.app.ui.rememberPrefer24
import com.pawmino.app.ui.viewmodel.PawminoUiState
import com.pawmino.app.ui.viewmodel.PawminoViewModel
import com.pawmino.app.util.CareLoopCalculator
import com.pawmino.app.util.DateTimeUtil
import com.pawmino.app.util.TaskGenerator
import com.pawmino.app.util.TimelineUtil

@Composable
fun DayDetailScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    date: String,
    nav: AppNavigator
) {
    val pet = state.activePet
    val prefer24 = rememberPrefer24(state.data.settings.timeFormat)

    // Ensure instances exist for this day so status changes persist correctly.
    LaunchedEffect(pet?.id, date) {
        if (pet != null && DateTimeUtil.isValidDate(date)) vm.ensureInstancesForActive(pet.id, date)
    }

    Scaffold(topBar = { PawTopBar(title = "Day detail", onBack = { nav.back() }) }) { padding ->
        if (pet == null || !DateTimeUtil.isValidDate(date)) {
            EmptyState(
                title = "Nothing to show",
                message = "This day is not available for the selected pet.",
                actionLabel = "Back",
                onAction = { nav.back() },
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        val tasks = state.data.careTasks
        val taskById = tasks.associateBy { it.id }
        val instances = TaskGenerator.activeInstancesFor(state.data.taskInstances, tasks, pet.id, date)
        val loop = CareLoopCalculator.build(instances, tasks)
        val sorted = TimelineUtil.sort(instances, tasks)
        val walks = state.data.walkLogs.filter { it.petId == pet.id && it.date == date }
        val notes = state.data.petNotes.filter { it.petId == pet.id && it.date == date }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(pet.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(DateTimeUtil.displayDate(date), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (loop.isEmpty) {
                            Text("No scheduled tasks.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                        } else {
                            Text(
                                "${loop.completedTasks} of ${loop.totalTasks} complete · ${loop.percent}%" +
                                    if (loop.skippedTasks > 0) " · ${loop.skippedTasks} skipped" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            if (sorted.isNotEmpty()) {
                item { SectionTitle("Tasks", modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
                items(sorted.size) { i ->
                    val inst = sorted[i]
                    TaskRow(
                        instance = inst,
                        task = taskById[inst.taskId],
                        prefer24 = prefer24,
                        onClick = { nav.toTaskDetail(inst.id) },
                        onToggleComplete = {
                            val s = if (inst.status == TaskStatus.Completed) TaskStatus.Pending else TaskStatus.Completed
                            vm.setInstanceStatus(inst, s)
                        }
                    )
                }
            }

            if (walks.isNotEmpty()) {
                item { SectionTitle("Walks", modifier = Modifier.padding(start = 4.dp, top = 8.dp)) }
                items(walks.size) { i ->
                    val w = walks[i]
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                buildString {
                                    append("Walk")
                                    if (w.startTime.isNotBlank()) append(" at ${w.startTime}")
                                },
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
                            )
                            val details = buildList {
                                w.durationMinutes?.let { add("$it min") }
                                if (w.distance != null && w.distanceUnit != com.pawmino.app.model.DistanceUnit.NotTracked) {
                                    add("${w.distance} ${w.distanceUnit.label}")
                                }
                            }
                            if (details.isNotEmpty()) Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                            if (w.note.isNotBlank()) Text(w.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { nav.toWalk(walkId = w.id, date = date) }) { Text("Edit") }
                        }
                    }
                }
            }

            if (notes.isNotEmpty()) {
                item { SectionTitle("Notes", modifier = Modifier.padding(start = 4.dp, top = 8.dp)) }
                items(notes.size) { i ->
                    val n = notes[i]
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(n.title.ifBlank { "(Untitled note)" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (n.text.isNotBlank()) Text(n.text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                            TextButton(onClick = { nav.toAddNote(noteId = n.id) }) { Text("Edit") }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { nav.toAddNote(date = date) }) { Text("Add note") }
                    TextButton(onClick = { nav.toWalk(date = date) }) { Text("Add walk") }
                    TextButton(onClick = { nav.toTaskList() }) { Text("Task settings") }
                }
            }
        }
    }
}
