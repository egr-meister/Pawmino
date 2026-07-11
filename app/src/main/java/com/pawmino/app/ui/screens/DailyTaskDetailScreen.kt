package com.pawmino.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pawmino.app.model.CareCategory
import com.pawmino.app.model.TaskStatus
import com.pawmino.app.ui.components.CategoryDot
import com.pawmino.app.ui.components.DisclaimerCard
import com.pawmino.app.ui.components.EmptyState
import com.pawmino.app.ui.components.PawTextField
import com.pawmino.app.ui.components.PawTopBar
import com.pawmino.app.ui.components.SectionTitle
import com.pawmino.app.ui.components.StatusPill
import com.pawmino.app.ui.navigation.AppNavigator
import com.pawmino.app.ui.rememberPrefer24
import com.pawmino.app.ui.viewmodel.PawminoUiState
import com.pawmino.app.ui.viewmodel.PawminoViewModel
import com.pawmino.app.util.DateTimeUtil

@Composable
fun DailyTaskDetailScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    instanceId: String,
    nav: AppNavigator
) {
    val instance = state.data.taskInstances.firstOrNull { it.id == instanceId }
    val prefer24 = rememberPrefer24(state.data.settings.timeFormat)

    Scaffold(topBar = { PawTopBar(title = "Care task", onBack = { nav.back() }) }) { padding ->
        if (instance == null) {
            EmptyState(
                title = "Task not found",
                message = "This care task record is no longer available.",
                actionLabel = "Back",
                onAction = { nav.back() },
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        val task = state.data.careTasks.firstOrNull { it.id == instance.taskId }
        val title = task?.title ?: "Deleted care task"
        var completionNote by remember(instance.id) { mutableStateOf(instance.note) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        task?.let { CategoryDot(it.category) }
                        Text(
                            "  " + (task?.categoryDisplayName() ?: "Unknown category"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    DetailLine("Date", DateTimeUtil.displayDate(instance.date))
                    DetailLine(
                        "Scheduled time",
                        if (instance.scheduledTime.isBlank()) "Anytime" else DateTimeUtil.displayTime(instance.scheduledTime, prefer24)
                    )
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                        StatusPill(status = instance.status)
                    }
                    if (instance.status == TaskStatus.Completed && instance.completedAt.isNotBlank()) {
                        DetailLine("Completed", DateTimeUtil.displayDate(instance.completedAt.take(10)))
                    }
                }
            }

            if (task != null && task.category == CareCategory.Feeding &&
                (task.portionLabel.isNotBlank() || task.foodLabel.isNotBlank())
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SectionTitle("Feeding labels")
                        if (task.portionLabel.isNotBlank()) DetailLine("Portion", task.portionLabel)
                        if (task.foodLabel.isNotBlank()) DetailLine("Food", task.foodLabel)
                        Text(
                            "Feeding labels are entered manually and are not nutritional recommendations.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            if (!task?.notes.isNullOrBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SectionTitle("Task notes")
                        Text(task!!.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            SectionTitle("Update status")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { vm.setInstanceStatus(instance, TaskStatus.Completed, completionNote) },
                    modifier = Modifier.weight(1f)
                ) { Text("Complete") }
                FilledTonalButton(
                    onClick = { vm.setInstanceStatus(instance, TaskStatus.Skipped, completionNote) },
                    modifier = Modifier.weight(1f)
                ) { Text("Skip") }
                OutlinedButton(
                    onClick = { vm.setInstanceStatus(instance, TaskStatus.Pending, completionNote) },
                    modifier = Modifier.weight(1f)
                ) { Text("Pending") }
            }

            SectionTitle("Completion note (optional)")
            PawTextField(
                value = completionNote,
                onValueChange = { completionNote = it },
                label = "Short note",
                singleLine = false,
                minLines = 2
            )
            OutlinedButton(onClick = { vm.updateInstanceNote(instance.id, completionNote) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save note")
            }

            if (task != null) {
                SectionTitle("More")
                if (task.category == CareCategory.Walk) {
                    Button(
                        onClick = { nav.toWalk(date = instance.date, instanceId = instance.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add walk details") }
                }
                OutlinedButton(
                    onClick = { nav.toAddNote(date = instance.date, instanceId = instance.id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add note") }
                OutlinedButton(onClick = { nav.toAddTask(task.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Edit care task")
                }
            } else {
                DisclaimerCard("The original care task was deleted. This record is kept for history.")
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
