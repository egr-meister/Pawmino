package com.pawmino.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.pawmino.app.model.TimeFormatPreference
import com.pawmino.app.model.WeekDay
import com.pawmino.app.ui.components.ConfirmDialog
import com.pawmino.app.ui.components.DisclaimerCard
import com.pawmino.app.ui.components.EnumDropdown
import com.pawmino.app.ui.components.PawTopBar
import com.pawmino.app.ui.components.SectionTitle
import com.pawmino.app.ui.navigation.AppNavigator
import com.pawmino.app.ui.viewmodel.PawminoUiState
import com.pawmino.app.ui.viewmodel.PawminoViewModel

private const val PRIVACY_NOTE =
    "Pawmino stores pet profiles, care tasks, schedules, completion records, walk logs, " +
        "grooming logs, notes, shopping items, and settings locally on this device. The app has " +
        "no account, no cloud sync, no internet access, no ads, no analytics, no payments, no " +
        "location tracking, no camera access, no push notifications, and no veterinary service."

private const val CARE_DISCLAIMER =
    "Pawmino is a manual pet care routine organizer. Tasks, schedules, notes, and completion " +
        "records are entered by the user. The app does not provide veterinary advice, diagnosis, " +
        "treatment, emergency guidance, or medical recommendations."

private const val EMERGENCY_DISCLAIMER =
    "If your pet appears unwell or needs urgent help, contact a qualified veterinarian or local " +
        "emergency veterinary service. Pawmino is not an emergency or medical application."

private const val REMINDER_NOTE =
    "Pawmino reminders appear inside the app while you use it. The app does not send push " +
        "notifications or run in the background."

@Composable
fun SettingsScreen(
    vm: PawminoViewModel,
    state: PawminoUiState,
    nav: AppNavigator
) {
    val settings = state.data.settings
    val reminders = settings.reminderSettings
    val activePet = state.activePet

    var confirmClearChecked by remember { mutableStateOf(false) }
    var confirmDeleteHistory by remember { mutableStateOf(false) }
    var confirmDeletePet by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)
    ) {
        PawTopBar(title = "Settings")

        SettingsCard("Pets & routines") {
            if (state.pets.isNotEmpty()) {
                EnumDropdown(
                    label = "Active pet",
                    options = state.pets,
                    selected = activePet ?: state.pets.first(),
                    optionLabel = { it.name.ifBlank { "Unnamed" } },
                    onSelect = { vm.selectPet(it.id) }
                )
            }
            SettingsLink("Manage pet profiles") { nav.toPetProfiles() }
            SettingsLink("Manage care tasks") { nav.toTaskList() }
            SettingsLink("View notes") { nav.toNotes() }
        }

        SettingsCard("Display") {
            EnumDropdown(
                label = "Time format",
                options = TimeFormatPreference.entries,
                selected = settings.timeFormat,
                optionLabel = { it.label },
                onSelect = { vm.setTimeFormat(it) }
            )
            EnumDropdown(
                label = "First day of week",
                options = WeekDay.entries,
                selected = settings.firstDayOfWeek,
                optionLabel = { it.label },
                onSelect = { vm.setFirstDayOfWeek(it) }
            )
        }

        SettingsCard("In-app reminders") {
            ToggleRow("Enable reminders", reminders.enabled) { vm.setReminderEnabled(it) }
            EnumDropdown(
                label = "Reminder lead time",
                options = listOf(15, 30, 60, 120),
                selected = listOf(15, 30, 60, 120).firstOrNull { it == reminders.leadTimeMinutes } ?: 30,
                optionLabel = { "$it minutes" },
                onSelect = { vm.setReminderLeadTime(it) }
            )
            ToggleRow("Show overdue task reminders", reminders.showOverdueTasks) { vm.setOverdueReminderEnabled(it) }
            ToggleRow("Show shopping reminders", reminders.showShoppingReminder) { vm.setShoppingReminderEnabled(it) }
            Text(REMINDER_NOTE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SettingsCard("Onboarding") {
            SettingsLink("Show onboarding again") {
                vm.showOnboardingAgain()
                nav.toOnboarding()
            }
        }

        SettingsCard("Data") {
            OutlinedButton(onClick = { confirmClearChecked = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Clear checked shopping items")
            }
            if (activePet != null) {
                OutlinedButton(onClick = { confirmDeleteHistory = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete history for ${activePet.name}")
                }
                OutlinedButton(onClick = { confirmDeletePet = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete pet: ${activePet.name}", color = MaterialTheme.colorScheme.error)
                }
            }
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Reset all local data", color = MaterialTheme.colorScheme.error)
            }
        }

        SettingsCard("About & safety") {
            Text("Pawmino", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Version 1.0.0 · Fully offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle("Care tracking")
            DisclaimerCard(CARE_DISCLAIMER)
            SectionTitle("Emergencies")
            DisclaimerCard(EMERGENCY_DISCLAIMER)
            SectionTitle("Privacy")
            Text(PRIVACY_NOTE, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (confirmClearChecked) {
        ConfirmDialog(
            title = "Clear checked items?",
            text = "Checked shopping items will be removed.",
            confirmLabel = "Clear",
            destructive = true,
            onConfirm = { vm.clearCheckedShoppingItems(null); confirmClearChecked = false },
            onDismiss = { confirmClearChecked = false }
        )
    }
    if (confirmDeleteHistory && activePet != null) {
        ConfirmDialog(
            title = "Delete history?",
            text = "Completion records, walk logs, and notes for ${activePet.name} will be removed. The pet and its task setup are kept.",
            confirmLabel = "Delete history",
            destructive = true,
            onConfirm = { vm.deletePetHistory(activePet.id); confirmDeleteHistory = false },
            onDismiss = { confirmDeleteHistory = false }
        )
    }
    if (confirmDeletePet && activePet != null) {
        ConfirmDialog(
            title = "Delete this pet profile?",
            text = "This will also remove care tasks, completion history, notes, and shopping links stored for this pet.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { vm.deletePet(activePet.id); confirmDeletePet = false },
            onDismiss = { confirmDeletePet = false }
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            title = "Reset all local data?",
            text = "This will permanently remove every pet profile, task, completion record, note, walk log, and shopping item stored by Pawmino on this device.",
            confirmLabel = "Reset everything",
            destructive = true,
            onConfirm = {
                vm.resetAllData()
                confirmReset = false
                nav.toOnboarding()
            },
            onDismiss = { confirmReset = false }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingsLink(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text("›", style = MaterialTheme.typography.titleMedium)
    }
}
