package com.pawmino.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.pawmino.app.data.RoutineTemplate
import com.pawmino.app.data.RoutineTemplates
import com.pawmino.app.model.PetProfile
import com.pawmino.app.model.PetType
import com.pawmino.app.ui.components.DatePickerField
import com.pawmino.app.ui.components.DisclaimerCard
import com.pawmino.app.ui.components.EnumDropdown
import com.pawmino.app.ui.components.PawTextField
import com.pawmino.app.ui.components.PawTopBar
import com.pawmino.app.ui.components.SectionTitle
import com.pawmino.app.ui.navigation.AppNavigator
import com.pawmino.app.ui.viewmodel.PawminoUiState
import com.pawmino.app.ui.viewmodel.PawminoViewModel
import com.pawmino.app.util.CareLoopCalculator
import com.pawmino.app.util.TaskGenerator
import com.pawmino.app.util.Validation

@Composable
private fun PetFormContent(
    initial: PetProfile?,
    showTemplates: Boolean,
    saveLabel: String,
    onSave: (name: String, type: PetType, breed: String, birth: String, adoption: String, color: String, notes: String, template: RoutineTemplate?) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.petType ?: PetType.Dog) }
    var breed by remember { mutableStateOf(initial?.breed ?: "") }
    var birth by remember { mutableStateOf(initial?.birthDate ?: "") }
    var adoption by remember { mutableStateOf(initial?.adoptionDate ?: "") }
    var color by remember { mutableStateOf(initial?.colorDescription ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var template by remember { mutableStateOf<RoutineTemplate?>(null) }
    var submitted by remember { mutableStateOf(false) }

    val nameError = if (submitted) Validation.petNameError(name) else null
    val birthError = if (submitted) Validation.optionalDateError(birth) else null
    val adoptionError = if (submitted) Validation.optionalDateError(adoption) else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PawTextField(value = name, onValueChange = { name = it }, label = "Pet name *", error = nameError)
        EnumDropdown(
            label = "Pet type *",
            options = PetType.entries,
            selected = type,
            optionLabel = { it.label },
            onSelect = { type = it }
        )
        PawTextField(value = breed, onValueChange = { breed = it }, label = "Breed (optional)")
        DatePickerField(label = "Birth date (optional)", value = birth, onChange = { birth = it }, error = birthError)
        DatePickerField(label = "Adoption date (optional)", value = adoption, onChange = { adoption = it }, error = adoptionError)
        PawTextField(value = color, onValueChange = { color = it }, label = "Color description (optional)")
        PawTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Notes (optional)",
            singleLine = false,
            minLines = 3
        )

        if (showTemplates) {
            SectionTitle("Optional starter routine")
            EnumDropdown(
                label = "Routine template",
                options = listOf<RoutineTemplate?>(null) + RoutineTemplates.all,
                selected = template,
                optionLabel = { it?.name ?: "None" },
                onSelect = { template = it }
            )
            template?.let {
                Text(it.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DisclaimerCard(RoutineTemplates.DISCLAIMER)
        }

        Button(
            onClick = {
                submitted = true
                if (Validation.petNameError(name) == null &&
                    Validation.optionalDateError(birth) == null &&
                    Validation.optionalDateError(adoption) == null
                ) {
                    onSave(name, type, breed, birth, adoption, color, notes, template)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(saveLabel) }

        Text(
            "Pawmino does not use a camera or photo library. No image is required.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun PetSetupScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = { PawTopBar(title = "Create pet profile", onBack = onBack) }) { padding ->
        PetFormContent(
            initial = null,
            showTemplates = true,
            saveLabel = "Save pet",
            onSave = { name, type, breed, birth, adoption, color, notes, template ->
                val id = vm.savePet(null, name, type, breed, birth, adoption, color, notes)
                template?.let { vm.applyTemplate(it, id) }
                onSaved()
            },
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun AddEditPetScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    petId: String?,
    nav: AppNavigator
) {
    val existing = state.data.pets.firstOrNull { it.id == petId }
    val isNew = existing == null
    Scaffold(topBar = { PawTopBar(title = if (isNew) "Add pet" else "Edit pet", onBack = { nav.back() }) }) { padding ->
        PetFormContent(
            initial = existing,
            showTemplates = isNew,
            saveLabel = if (isNew) "Save pet" else "Save changes",
            onSave = { name, type, breed, birth, adoption, color, notes, template ->
                val id = vm.savePet(existing?.id, name, type, breed, birth, adoption, color, notes)
                if (isNew) template?.let { vm.applyTemplate(it, id) }
                nav.back()
            },
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun PetProfilesScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    nav: AppNavigator
) {
    Scaffold(
        topBar = {
            PawTopBar(title = "Pet profiles", onBack = { nav.back() }, actions = {
                TextButton(onClick = { nav.toAddPet() }) { Text("Add") }
            })
        }
    ) { padding ->
        if (state.pets.isEmpty()) {
            com.pawmino.app.ui.components.EmptyState(
                title = "No pets yet",
                message = "Add a pet profile to start tracking care.",
                icon = Icons.Outlined.Pets,
                actionLabel = "Add Pet",
                onAction = { nav.toAddPet() },
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.pets, key = { it.id }) { pet ->
                val tasks = state.data.careTasks.filter { it.petId == pet.id && it.enabled }
                val active = TaskGenerator.activeInstancesFor(
                    state.data.taskInstances, state.data.careTasks, pet.id, state.selectedDateIso
                )
                val loop = CareLoopCalculator.build(active, state.data.careTasks)
                PetProfileRow(
                    pet = pet,
                    activeTaskCount = tasks.size,
                    percent = loop.percent,
                    isActive = pet.id == state.activePet?.id,
                    onSelect = { vm.selectPet(pet.id) },
                    onEdit = { nav.toAddPet(pet.id) }
                )
            }
        }
    }
}

@Composable
private fun PetProfileRow(
    pet: PetProfile,
    activeTaskCount: Int,
    percent: Int,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    pet.name.trim().take(1).uppercase().ifBlank { "?" },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pet.name.ifBlank { "Unnamed" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (isActive) {
                        Text(
                            "  • Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    "${pet.petType.label} · $activeTaskCount active tasks · $percent% today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (!isActive) TextButton(onClick = onSelect) { Text("Select") }
                TextButton(onClick = onEdit) { Text("Edit") }
            }
        }
    }
}
