package com.pawmino.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pawmino.app.model.CareCategory
import com.pawmino.app.model.ScheduleType
import com.pawmino.app.model.WeekDay
import com.pawmino.app.ui.components.DatePickerField
import com.pawmino.app.ui.components.DisclaimerCard
import com.pawmino.app.ui.components.EnumDropdown
import com.pawmino.app.ui.components.PawTextField
import com.pawmino.app.ui.components.PawTopBar
import com.pawmino.app.ui.components.SectionTitle
import com.pawmino.app.ui.components.TimePickerField
import com.pawmino.app.ui.navigation.AppNavigator
import com.pawmino.app.ui.rememberPrefer24
import com.pawmino.app.ui.viewmodel.PawminoUiState
import com.pawmino.app.ui.viewmodel.PawminoViewModel
import com.pawmino.app.util.Validation

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditCareTaskScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    taskId: String?,
    nav: AppNavigator
) {
    val existing = state.data.careTasks.firstOrNull { it.id == taskId }
    val petId = existing?.petId ?: state.activePet?.id
    val prefer24 = rememberPrefer24(state.data.settings.timeFormat)

    if (petId == null) {
        Scaffold(topBar = { PawTopBar(title = "Add task", onBack = { nav.back() }) }) { padding ->
            com.pawmino.app.ui.components.EmptyState(
                title = "No pet selected",
                message = "Add a pet profile before creating care tasks.",
                actionLabel = "Add Pet",
                onAction = { nav.toAddPet() },
                modifier = Modifier.padding(padding)
            )
        }
        return
    }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: CareCategory.Feeding) }
    var customCategory by remember { mutableStateOf(existing?.customCategoryName ?: "") }
    var scheduleType by remember { mutableStateOf(existing?.scheduleType ?: ScheduleType.Daily) }
    val selectedDays = remember { mutableStateListOf<WeekDay>().apply { existing?.daysOfWeek?.let { addAll(it) } } }
    var specificDate by remember { mutableStateOf(existing?.specificDate ?: "") }
    val times = remember { mutableStateListOf<String>().apply { existing?.times?.let { addAll(it) } } }
    var repeatInterval by remember { mutableStateOf(existing?.repeatIntervalDays?.toString() ?: "2") }
    var portionLabel by remember { mutableStateOf(existing?.portionLabel ?: "") }
    var foodLabel by remember { mutableStateOf(existing?.foodLabel ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var enabled by remember { mutableStateOf(existing?.enabled ?: true) }
    var submitted by remember { mutableStateOf(false) }

    val titleError = if (submitted) Validation.taskTitleError(title) else null
    val scheduleError = if (submitted) Validation.scheduleError(
        scheduleType, selectedDays.toList(), specificDate,
        repeatInterval.toIntOrNull()
    ) else null

    Scaffold(topBar = { PawTopBar(title = if (existing == null) "Add care task" else "Edit care task", onBack = { nav.back() }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PawTextField(value = title, onValueChange = { title = it }, label = "Task title *", error = titleError)

            EnumDropdown(
                label = "Category *",
                options = CareCategory.entries,
                selected = category,
                optionLabel = { it.label },
                onSelect = { category = it }
            )
            if (category == CareCategory.Other) {
                PawTextField(value = customCategory, onValueChange = { customCategory = it }, label = "Custom category name (optional)")
            }

            SectionTitle("Schedule")
            EnumDropdown(
                label = "Schedule type",
                options = ScheduleType.entries,
                selected = scheduleType,
                optionLabel = { it.label },
                onSelect = { scheduleType = it }
            )

            when (scheduleType) {
                ScheduleType.SelectedDays -> {
                    Text("Repeat on", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WeekDay.entries.forEach { day ->
                            FilterChip(
                                selected = selectedDays.contains(day),
                                onClick = {
                                    if (selectedDays.contains(day)) selectedDays.remove(day) else selectedDays.add(day)
                                },
                                label = { Text(day.shortLabel) }
                            )
                        }
                    }
                }
                ScheduleType.OneTime -> {
                    DatePickerField(label = "Date *", value = specificDate, onChange = { specificDate = it }, optional = false)
                }
                ScheduleType.EveryNumberOfDays -> {
                    PawTextField(
                        value = repeatInterval,
                        onValueChange = { repeatInterval = it.filter { c -> c.isDigit() } },
                        label = "Repeat every N days",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        supporting = "Counts from today. Minimum 1 day."
                    )
                }
                ScheduleType.Unscheduled -> {
                    Text(
                        "Unscheduled tasks are not generated automatically. Add them to a day from the task list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ScheduleType.Daily -> {}
            }

            if (scheduleError != null) {
                Text(scheduleError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            SectionTitle("Times (optional)")
            times.forEachIndexed { index, t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimePickerField(
                        label = "Time ${index + 1}",
                        value = t,
                        onChange = { times[index] = it },
                        prefer24 = prefer24,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { times.removeAt(index) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Remove time")
                    }
                }
            }
            OutlinedButton(onClick = { times.add("") }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add time")
            }
            Text(
                "No time means the task shows under \"Anytime\". Add several times for multiple daily instances.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (category == CareCategory.Feeding) {
                SectionTitle("Feeding labels (optional)")
                PawTextField(value = portionLabel, onValueChange = { portionLabel = it }, label = "Portion label (e.g. 1 scoop)")
                PawTextField(value = foodLabel, onValueChange = { foodLabel = it }, label = "Food label (e.g. Dry food)")
                DisclaimerCard("Feeding labels are entered manually and are not nutritional recommendations.")
            }

            PawTextField(value = notes, onValueChange = { notes = it }, label = "Notes (optional)", singleLine = false, minLines = 2)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Enabled", modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            Button(
                onClick = {
                    submitted = true
                    val tError = Validation.taskTitleError(title)
                    val sError = Validation.scheduleError(scheduleType, selectedDays.toList(), specificDate, repeatInterval.toIntOrNull())
                    if (tError == null && sError == null) {
                        vm.saveTask(
                            existingId = existing?.id,
                            petId = petId,
                            title = title,
                            category = category,
                            customCategoryName = customCategory,
                            scheduleType = scheduleType,
                            daysOfWeek = selectedDays.toList(),
                            specificDate = specificDate,
                            times = times.toList(),
                            repeatIntervalDays = repeatInterval.toIntOrNull(),
                            enabled = enabled,
                            portionLabel = portionLabel,
                            foodLabel = foodLabel,
                            notes = notes
                        )
                        nav.back()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (existing == null) "Save task" else "Save changes") }

            Spacer(Modifier.height(24.dp))
        }
    }
}
