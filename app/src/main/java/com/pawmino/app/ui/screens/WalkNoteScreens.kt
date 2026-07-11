package com.pawmino.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pawmino.app.model.DistanceUnit
import com.pawmino.app.ui.components.ConfirmDialog
import com.pawmino.app.ui.components.DatePickerField
import com.pawmino.app.ui.components.DisclaimerCard
import com.pawmino.app.ui.components.EmptyState
import com.pawmino.app.ui.components.EnumDropdown
import com.pawmino.app.ui.components.PawTextField
import com.pawmino.app.ui.components.PawTopBar
import com.pawmino.app.ui.components.SectionTitle
import com.pawmino.app.ui.components.TimePickerField
import com.pawmino.app.ui.navigation.AppNavigator
import com.pawmino.app.ui.rememberPrefer24
import com.pawmino.app.ui.viewmodel.PawminoUiState
import com.pawmino.app.ui.viewmodel.PawminoViewModel
import com.pawmino.app.util.DateTimeUtil
import com.pawmino.app.util.Validation

@Composable
fun WalkLogScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    walkId: String?,
    date: String?,
    instanceId: String?,
    nav: AppNavigator
) {
    val pet = state.activePet
    val prefer24 = rememberPrefer24(state.data.settings.timeFormat)
    val existing = state.data.walkLogs.firstOrNull { it.id == walkId }

    if (pet == null) {
        Scaffold(topBar = { PawTopBar(title = "Walk", onBack = { nav.back() }) }) { p ->
            EmptyState("No pet selected", "Add a pet to log walks.", actionLabel = "Add Pet", onAction = { nav.toAddPet() }, modifier = Modifier.padding(p))
        }
        return
    }

    var walkDate by remember { mutableStateOf(existing?.date ?: date ?: state.selectedDateIso) }
    var startTime by remember { mutableStateOf(existing?.startTime ?: "") }
    var duration by remember { mutableStateOf(existing?.durationMinutes?.toString() ?: "") }
    var distance by remember { mutableStateOf(existing?.distance?.toString() ?: "") }
    var unit by remember { mutableStateOf(existing?.distanceUnit ?: DistanceUnit.NotTracked) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var pendingDelete by remember { mutableStateOf(false) }

    Scaffold(topBar = { PawTopBar(title = if (existing == null) "Add walk" else "Edit walk", onBack = { nav.back() }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DisclaimerCard("Walk details are entered manually. Pawmino does not use GPS or track location.")
            DatePickerField(label = "Date", value = walkDate, onChange = { walkDate = it }, optional = false)
            TimePickerField(label = "Start time (optional)", value = startTime, onChange = { startTime = it }, prefer24 = prefer24)
            PawTextField(value = duration, onValueChange = { duration = it.filter { c -> c.isDigit() } }, label = "Duration in minutes (optional)", keyboardType = KeyboardType.Number)
            PawTextField(value = distance, onValueChange = { distance = it }, label = "Distance (optional)", keyboardType = KeyboardType.Decimal)
            EnumDropdown(label = "Distance unit", options = DistanceUnit.entries, selected = unit, optionLabel = { it.label }, onSelect = { unit = it })
            PawTextField(value = note, onValueChange = { note = it }, label = "Note (optional)", singleLine = false, minLines = 2)

            Button(
                onClick = {
                    vm.saveWalk(
                        existingId = existing?.id,
                        petId = pet.id,
                        date = if (DateTimeUtil.isValidDate(walkDate)) walkDate else state.selectedDateIso,
                        startTime = startTime,
                        durationMinutes = Validation.parseOptionalInt(duration),
                        distance = Validation.parseOptionalDouble(distance),
                        distanceUnit = unit,
                        note = note,
                        linkedTaskInstanceId = existing?.linkedTaskInstanceId ?: instanceId
                    )
                    nav.back()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (existing == null) "Save walk" else "Save changes") }

            if (existing != null) {
                OutlinedButton(onClick = { pendingDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete walk", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (pendingDelete && existing != null) {
        ConfirmDialog(
            title = "Delete walk?",
            text = "This walk log will be removed.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { vm.deleteWalk(existing.id); pendingDelete = false; nav.back() },
            onDismiss = { pendingDelete = false }
        )
    }
}

@Composable
fun NotesScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    nav: AppNavigator
) {
    val pet = state.activePet
    var filterDate by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            PawTopBar(title = "Notes${pet?.let { " · ${it.name}" } ?: ""}", onBack = { nav.back() }, actions = {
                TextButton(onClick = { nav.toAddNote() }) { Text("Add") }
            })
        }
    ) { padding ->
        if (pet == null) {
            EmptyState("No pet selected", "Add a pet to keep notes.", actionLabel = "Add Pet", onAction = { nav.toAddPet() }, modifier = Modifier.padding(padding))
            return@Scaffold
        }
        val notes = state.data.petNotes
            .filter { it.petId == pet.id && (filterDate.isBlank() || it.date == filterDate) }
            .sortedWith(compareByDescending<com.pawmino.app.model.PetNote> { it.date }.thenByDescending { it.time })

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DatePickerField(
                label = "Filter by date (optional)",
                value = filterDate,
                onChange = { filterDate = it },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            if (notes.isEmpty()) {
                EmptyState("No notes", "Add a neutral care note for ${pet.name}.", actionLabel = "Add Note", onAction = { nav.toAddNote() })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notes, key = { it.id }) { n ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(n.title.ifBlank { "(Untitled note)" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    DateTimeUtil.displayDate(n.date) + (if (n.time.isNotBlank()) " · ${n.time}" else ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (n.text.isNotBlank()) {
                                    Text(n.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { nav.toAddNote(noteId = n.id) }) { Text("Edit") }
                                    TextButton(onClick = { pendingDelete = n.id }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { id ->
        ConfirmDialog(
            title = "Delete note?",
            text = "This note will be removed.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { vm.deleteNote(id); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
fun AddEditNoteScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    noteId: String?,
    date: String?,
    instanceId: String?,
    nav: AppNavigator
) {
    val pet = state.activePet
    val existing = state.data.petNotes.firstOrNull { it.id == noteId }

    if (pet == null) {
        Scaffold(topBar = { PawTopBar(title = "Note", onBack = { nav.back() }) }) { p ->
            EmptyState("No pet selected", "Add a pet first.", actionLabel = "Add Pet", onAction = { nav.toAddPet() }, modifier = Modifier.padding(p))
        }
        return
    }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var text by remember { mutableStateOf(existing?.text ?: "") }
    var noteDate by remember { mutableStateOf(existing?.date ?: date ?: state.selectedDateIso) }
    var time by remember { mutableStateOf(existing?.time ?: "") }
    val prefer24 = rememberPrefer24(state.data.settings.timeFormat)

    val titleError = Validation.noteTitleError(title)
    val bodyError = Validation.noteBodyError(text)

    Scaffold(topBar = { PawTopBar(title = if (existing == null) "Add note" else "Edit note", onBack = { nav.back() }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DatePickerField(label = "Date", value = noteDate, onChange = { noteDate = it }, optional = false)
            TimePickerField(label = "Time (optional)", value = time, onChange = { time = it }, prefer24 = prefer24)
            PawTextField(
                value = title,
                onValueChange = { title = it },
                label = "Title (optional)",
                error = titleError,
                supporting = "${title.length}/${Validation.NOTE_TITLE_MAX}"
            )
            PawTextField(
                value = text,
                onValueChange = { text = it },
                label = "Note",
                singleLine = false,
                minLines = 4,
                error = bodyError,
                supporting = "${text.length}/${Validation.NOTE_BODY_MAX}"
            )
            Button(
                onClick = {
                    if (titleError == null && bodyError == null) {
                        vm.saveNote(
                            existingId = existing?.id,
                            petId = pet.id,
                            date = if (DateTimeUtil.isValidDate(noteDate)) noteDate else state.selectedDateIso,
                            time = time,
                            title = title,
                            text = text,
                            linkedTaskInstanceId = existing?.linkedTaskInstanceId ?: instanceId
                        )
                        nav.back()
                    }
                },
                enabled = titleError == null && bodyError == null,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (existing == null) "Save note" else "Save changes") }

            SectionTitle("About notes")
            Text(
                "Notes are free text for routine, behavior, or preferences. Pawmino does not analyze notes or provide any medical interpretation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
