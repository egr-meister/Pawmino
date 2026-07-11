package com.pawmino.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.pawmino.app.PawminoApplication
import com.pawmino.app.data.PawminoRepository
import com.pawmino.app.data.RoutineTemplate
import com.pawmino.app.model.AppData
import com.pawmino.app.model.CareCategory
import com.pawmino.app.model.CareTask
import com.pawmino.app.model.DailyTaskInstance
import com.pawmino.app.model.DistanceUnit
import com.pawmino.app.model.PetNote
import com.pawmino.app.model.PetProfile
import com.pawmino.app.model.PetType
import com.pawmino.app.model.ScheduleType
import com.pawmino.app.model.ShoppingCategory
import com.pawmino.app.model.ShoppingItem
import com.pawmino.app.model.ShoppingPriority
import com.pawmino.app.model.TaskStatus
import com.pawmino.app.model.TimeFormatPreference
import com.pawmino.app.model.WalkLog
import com.pawmino.app.model.WeekDay
import com.pawmino.app.util.DateTimeUtil
import com.pawmino.app.util.IdGen
import com.pawmino.app.util.ReminderKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Combined UI state shared across all screens. */
data class PawminoUiState(
    val loaded: Boolean = false,
    val data: AppData = AppData(),
    val selectedDateIso: String = DateTimeUtil.todayIso(),
    val dismissedReminders: Set<ReminderKind> = emptySet()
) {
    val pets: List<PetProfile> get() = data.pets
    val hasPets: Boolean get() = data.pets.isNotEmpty()

    val activePet: PetProfile?
        get() {
            val id = data.settings.activePetId
            return data.pets.firstOrNull { it.id == id } ?: data.pets.firstOrNull()
        }

    fun tasksForActive(): List<CareTask> {
        val petId = activePet?.id ?: return emptyList()
        return data.careTasks.filter { it.petId == petId }
    }
}

class PawminoViewModel(private val repo: PawminoRepository) : ViewModel() {

    private val selectedDate = MutableStateFlow(DateTimeUtil.todayIso())
    private val dismissed = MutableStateFlow<Set<ReminderKind>>(emptySet())

    val uiState: StateFlow<PawminoUiState> =
        combine(repo.appData, selectedDate, dismissed) { data, date, dismiss ->
            PawminoUiState(
                loaded = true,
                data = data,
                selectedDateIso = date,
                dismissedReminders = dismiss
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PawminoUiState()
        )

    private fun snapshot(): AppData = uiState.value.data

    // -------------------------------------------------------------------------------------
    // Selection & reminders
    // -------------------------------------------------------------------------------------

    fun selectPet(petId: String) {
        viewModelScope.launch {
            repo.setActivePet(petId)
            dismissed.value = emptySet()
            ensureInstancesForActive(petId, selectedDate.value)
        }
    }

    fun selectDate(dateIso: String) {
        if (!DateTimeUtil.isValidDate(dateIso)) return
        selectedDate.value = dateIso
        if (DateTimeUtil.isToday(dateIso)) dismissed.value = emptySet()
        uiState.value.activePet?.id?.let { ensureInstancesForActive(it, dateIso) }
    }

    fun goToToday() = selectDate(DateTimeUtil.todayIso())

    fun dismissReminder(kind: ReminderKind) {
        dismissed.value = dismissed.value + kind
    }

    /** Ensure instances exist for the active pet + selected date (call from Today on launch). */
    fun ensureInstancesForActive(petId: String? = uiState.value.activePet?.id, dateIso: String = selectedDate.value) {
        val id = petId ?: return
        viewModelScope.launch { repo.ensureInstances(id, dateIso) }
    }

    // -------------------------------------------------------------------------------------
    // Onboarding & settings
    // -------------------------------------------------------------------------------------

    fun completeOnboarding() = viewModelScope.launch { repo.setOnboardingCompleted(true) }
    fun showOnboardingAgain() = viewModelScope.launch { repo.setOnboardingCompleted(false) }

    fun setTimeFormat(pref: TimeFormatPreference) =
        viewModelScope.launch { repo.updateSettings { it.copy(timeFormat = pref) } }

    fun setFirstDayOfWeek(day: WeekDay) =
        viewModelScope.launch { repo.updateSettings { it.copy(firstDayOfWeek = day) } }

    fun setReminderEnabled(enabled: Boolean) =
        viewModelScope.launch { repo.updateReminderSettings { it.copy(enabled = enabled) } }

    fun setReminderLeadTime(minutes: Int) =
        viewModelScope.launch { repo.updateReminderSettings { it.copy(leadTimeMinutes = minutes) } }

    fun setOverdueReminderEnabled(enabled: Boolean) =
        viewModelScope.launch { repo.updateReminderSettings { it.copy(showOverdueTasks = enabled) } }

    fun setShoppingReminderEnabled(enabled: Boolean) =
        viewModelScope.launch { repo.updateReminderSettings { it.copy(showShoppingReminder = enabled) } }

    // -------------------------------------------------------------------------------------
    // Pets
    // -------------------------------------------------------------------------------------

    fun savePet(
        existingId: String?,
        name: String,
        petType: PetType,
        breed: String,
        birthDate: String,
        adoptionDate: String,
        colorDescription: String,
        notes: String
    ): String {
        val now = DateTimeUtil.nowTimestamp()
        val id = existingId ?: IdGen.newId()
        val existing = snapshot().pets.firstOrNull { it.id == id }
        val pet = PetProfile(
            id = id,
            name = name.trim(),
            petType = petType,
            breed = breed.trim(),
            birthDate = birthDate.trim(),
            adoptionDate = adoptionDate.trim(),
            colorDescription = colorDescription.trim(),
            notes = notes.trim(),
            createdAt = existing?.createdAt?.ifBlank { now } ?: now,
            updatedAt = now
        )
        viewModelScope.launch { repo.upsertPet(pet) }
        return id
    }

    fun deletePet(petId: String) = viewModelScope.launch { repo.deletePet(petId) }
    fun deletePetHistory(petId: String) = viewModelScope.launch { repo.deletePetHistory(petId) }

    fun applyTemplate(template: RoutineTemplate, petId: String) =
        viewModelScope.launch { repo.applyTemplate(template, petId) }

    // -------------------------------------------------------------------------------------
    // Care tasks
    // -------------------------------------------------------------------------------------

    fun saveTask(
        existingId: String?,
        petId: String,
        title: String,
        category: CareCategory,
        customCategoryName: String,
        scheduleType: ScheduleType,
        daysOfWeek: List<WeekDay>,
        specificDate: String,
        times: List<String>,
        repeatIntervalDays: Int?,
        enabled: Boolean,
        portionLabel: String,
        foodLabel: String,
        notes: String
    ): String {
        val now = DateTimeUtil.nowTimestamp()
        val id = existingId ?: IdGen.newId()
        val existing = snapshot().careTasks.firstOrNull { it.id == id }
        val sortOrder = existing?.sortOrder
            ?: ((snapshot().careTasks.filter { it.petId == petId }.maxOfOrNull { it.sortOrder } ?: -1) + 1)
        val anchor = when (scheduleType) {
            ScheduleType.EveryNumberOfDays -> existing?.anchorDate?.ifBlank { DateTimeUtil.todayIso() } ?: DateTimeUtil.todayIso()
            else -> existing?.anchorDate ?: ""
        }
        val task = CareTask(
            id = id,
            petId = petId,
            title = title.trim(),
            category = category,
            customCategoryName = customCategoryName.trim(),
            scheduleType = scheduleType,
            daysOfWeek = daysOfWeek.distinct(),
            specificDate = specificDate.trim(),
            times = times.filter { it.isNotBlank() }.distinct().sorted(),
            repeatIntervalDays = repeatIntervalDays,
            anchorDate = anchor,
            enabled = enabled,
            portionLabel = portionLabel.trim(),
            foodLabel = foodLabel.trim(),
            notes = notes.trim(),
            sortOrder = sortOrder,
            createdAt = existing?.createdAt?.ifBlank { now } ?: now,
            updatedAt = now
        )
        viewModelScope.launch {
            repo.upsertTask(task)
            // Refresh today's instances so a new/edited task appears immediately if active.
            uiState.value.activePet?.id?.let { repo.ensureInstances(it, selectedDate.value) }
        }
        return id
    }

    fun setTaskEnabled(taskId: String, enabled: Boolean) =
        viewModelScope.launch {
            repo.setTaskEnabled(taskId, enabled)
            uiState.value.activePet?.id?.let { repo.ensureInstances(it, selectedDate.value) }
        }

    fun deleteTask(taskId: String) = viewModelScope.launch { repo.deleteTask(taskId) }

    fun duplicateTask(task: CareTask) {
        val now = DateTimeUtil.nowTimestamp()
        val sortOrder = (snapshot().careTasks.filter { it.petId == task.petId }.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val copy = task.copy(
            id = IdGen.newId(),
            title = task.title + " (copy)",
            sortOrder = sortOrder,
            createdAt = now,
            updatedAt = now
        )
        viewModelScope.launch { repo.upsertTask(copy) }
    }

    /** Move a task earlier/later within its pet's ordering by swapping sortOrder values. */
    fun moveTask(taskId: String, up: Boolean) {
        val data = snapshot()
        val task = data.careTasks.firstOrNull { it.id == taskId } ?: return
        val siblings = data.careTasks.filter { it.petId == task.petId }
            .sortedWith(compareBy({ it.sortOrder }, { it.title.lowercase() }))
        val index = siblings.indexOfFirst { it.id == taskId }
        if (index < 0) return
        val swapWith = if (up) index - 1 else index + 1
        if (swapWith !in siblings.indices) return
        val other = siblings[swapWith]
        val now = DateTimeUtil.nowTimestamp()
        viewModelScope.launch {
            repo.upsertTask(task.copy(sortOrder = other.sortOrder, updatedAt = now))
            repo.upsertTask(other.copy(sortOrder = task.sortOrder, updatedAt = now))
        }
    }

    // -------------------------------------------------------------------------------------
    // Task instance status
    // -------------------------------------------------------------------------------------

    fun setInstanceStatus(instance: DailyTaskInstance, status: TaskStatus, note: String? = null) =
        viewModelScope.launch { repo.setInstanceStatus(instance, status, note) }

    fun updateInstanceNote(instanceId: String, note: String) =
        viewModelScope.launch { repo.updateInstanceNote(instanceId, note) }

    /** Add a manual instance for an Unscheduled task on the selected date. */
    fun addManualInstance(task: CareTask, dateIso: String, time: String) {
        val now = DateTimeUtil.nowTimestamp()
        val instance = DailyTaskInstance(
            id = IdGen.instanceId(task.id, dateIso, time),
            taskId = task.id,
            petId = task.petId,
            date = dateIso,
            scheduledTime = time,
            status = TaskStatus.Pending,
            createdAt = now,
            updatedAt = now
        )
        viewModelScope.launch { repo.setInstanceStatus(instance, TaskStatus.Pending) }
    }

    // -------------------------------------------------------------------------------------
    // Walk logs
    // -------------------------------------------------------------------------------------

    fun saveWalk(
        existingId: String?,
        petId: String,
        date: String,
        startTime: String,
        durationMinutes: Int?,
        distance: Double?,
        distanceUnit: DistanceUnit,
        note: String,
        linkedTaskInstanceId: String?
    ): String {
        val now = DateTimeUtil.nowTimestamp()
        val id = existingId ?: IdGen.newId()
        val existing = snapshot().walkLogs.firstOrNull { it.id == id }
        val walk = WalkLog(
            id = id,
            petId = petId,
            date = date,
            startTime = startTime.trim(),
            durationMinutes = durationMinutes,
            distance = distance,
            distanceUnit = distanceUnit,
            note = note.trim(),
            linkedTaskInstanceId = linkedTaskInstanceId,
            createdAt = existing?.createdAt?.ifBlank { now } ?: now,
            updatedAt = now
        )
        viewModelScope.launch { repo.upsertWalk(walk) }
        return id
    }

    fun deleteWalk(walkId: String) = viewModelScope.launch { repo.deleteWalk(walkId) }

    // -------------------------------------------------------------------------------------
    // Notes
    // -------------------------------------------------------------------------------------

    fun saveNote(
        existingId: String?,
        petId: String,
        date: String,
        time: String,
        title: String,
        text: String,
        linkedTaskInstanceId: String?
    ): String {
        val now = DateTimeUtil.nowTimestamp()
        val id = existingId ?: IdGen.newId()
        val existing = snapshot().petNotes.firstOrNull { it.id == id }
        val note = PetNote(
            id = id,
            petId = petId,
            date = date,
            time = time.trim(),
            title = title.trim(),
            text = text.trim(),
            linkedTaskInstanceId = linkedTaskInstanceId,
            createdAt = existing?.createdAt?.ifBlank { now } ?: now,
            updatedAt = now
        )
        viewModelScope.launch { repo.upsertNote(note) }
        return id
    }

    fun deleteNote(noteId: String) = viewModelScope.launch { repo.deleteNote(noteId) }

    // -------------------------------------------------------------------------------------
    // Shopping
    // -------------------------------------------------------------------------------------

    fun saveShoppingItem(
        existingId: String?,
        petId: String?,
        title: String,
        category: ShoppingCategory,
        quantityLabel: String,
        priority: ShoppingPriority,
        note: String
    ): String {
        val now = DateTimeUtil.nowTimestamp()
        val id = existingId ?: IdGen.newId()
        val existing = snapshot().shoppingItems.firstOrNull { it.id == id }
        val item = ShoppingItem(
            id = id,
            petId = petId,
            title = title.trim(),
            category = category,
            quantityLabel = quantityLabel.trim(),
            priority = priority,
            checked = existing?.checked ?: false,
            note = note.trim(),
            createdAt = existing?.createdAt?.ifBlank { now } ?: now,
            updatedAt = now
        )
        viewModelScope.launch { repo.upsertShoppingItem(item) }
        return id
    }

    fun setShoppingChecked(itemId: String, checked: Boolean) =
        viewModelScope.launch { repo.setShoppingChecked(itemId, checked) }

    fun deleteShoppingItem(itemId: String) =
        viewModelScope.launch { repo.deleteShoppingItem(itemId) }

    fun clearCheckedShoppingItems(petId: String?) =
        viewModelScope.launch { repo.clearCheckedShoppingItems(petId) }

    // -------------------------------------------------------------------------------------
    // Reset
    // -------------------------------------------------------------------------------------

    fun resetAllData() = viewModelScope.launch { repo.resetAllData() }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as PawminoApplication)
                PawminoViewModel(app.repository)
            }
        }
    }
}
